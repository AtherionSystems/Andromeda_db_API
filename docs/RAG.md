# RAG — Retrieval-Augmented Generation

Semantic search system over project data for the Andromeda Telegram bot.
Allows users to ask open-ended questions in natural language about user stories, tasks, and sprints,
and get answers grounded in real project data.

---

## Architecture

```
INGESTION (one-time, manual)
  Oracle Database (UserStories, Tasks, Sprints)
        ↓ text formatting
  EmbeddingService → Gemini gemini-embedding-2 API
        ↓ 3072-dimensional vector
  VectorStoreService → andromeda_vectors table (Oracle 26ai VECTOR)

QUERY (real-time, per question)
  User question
        ↓
  EmbeddingService → question vector (3072 dims)
        ↓
  VectorStoreService → VECTOR_DISTANCE (COSINE) → top 5 relevant chunks
        ↓
  AiService.chat() → Gemini with the 5 chunks as context
        ↓
  Answer grounded in real project data
```

---

## Components

### EmbeddingService
Converts text into a 3072-dimensional numeric vector using the native Gemini API.

- **Model**: `gemini-embedding-2`
- **Endpoint**: `https://generativelanguage.googleapis.com/v1/models/gemini-embedding-2:embedContent`
- **Input**: free-form text (user story, task, user question)
- **Output**: `float[]` of 3072 elements

### VectorStoreService
Manages vector persistence and search in Oracle 26ai.

- **Table**: `andromeda_vectors`
- **Column type**: `VECTOR(3072, FLOAT32)` — native Oracle 23ai+ type
- **Upsert**: DELETE + INSERT with deterministic UUID (`type:entityId`)
- **Search**: `ORDER BY VECTOR_DISTANCE(embedding, TO_VECTOR(?), COSINE) FETCH FIRST 5 ROWS ONLY`
- **Project filter**: all queries are scoped to the user's active `project_id`

### RagIngestionService
Orchestrates data ingestion from Oracle into the vector store.

Processes three entity types per project:

| Entity | Repository | Indexed text format |
|---|---|---|
| UserStory | `UserStoryRepository` | `[UserStory #id] title \n Status \| Priority \| Points \n Description \n Acceptance Criteria` |
| Tasks | `TasksRepository` | `[Task #id] title \n Status \| Priority \| Estimated hours \n Description` |
| Sprint | `SprintRepository` | `[Sprint #id] name \n Status \| Start \| End \n Goal` |

Ingestion is **idempotent**: re-indexing the same project does not create duplicates.

### RagService
Entry point for RAG queries called from `AiIntentRouter`.

Internal flow:
1. Embeds the user's question
2. Searches the top 5 most similar chunks in Oracle (filtered by `projectId`)
3. Joins the chunks as context
4. Calls `AiService.chat()` with the system prompt + context + question
5. Returns the LLM response

**Language behavior**: the bot responds in the same language the user writes in. If the user asks in Spanish, the answer is in Spanish; if in English, in English.

### RagController
REST endpoints for administration.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/admin/rag/ingest?projectId=X` | Index a specific project |
| `POST` | `/api/admin/rag/ingest` | Index all projects |

---

## Database table

```sql
CREATE TABLE andromeda_vectors (
    id           VARCHAR2(150)  NOT NULL,  -- UUID: md5(type:entityId)
    entity_type  VARCHAR2(50)   NOT NULL,  -- "user_story" | "task" | "sprint"
    entity_id    NUMBER         NOT NULL,  -- ID of the original record
    project_id   NUMBER,                   -- used to filter by project in queries
    text_content VARCHAR2(4000) NOT NULL,  -- human-readable text sent to the LLM
    embedding    VECTOR(3072, FLOAT32),    -- semantic representation
    created_at   TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT pk_andromeda_vectors PRIMARY KEY (id)
);
```

**Note**: the table is created manually (Flyway is disabled in this project).
The reference file is `src/main/resources/db/migration/V7__rag_vector_store.sql`.

---

## Configuration

| Property | Environment variable | Current value |
|---|---|---|
| Embedding model | `AGENT_AI_EMBEDDING_MODEL` | `gemini-embedding-2` |
| API Key | `AGENT_AI_API_KEY` | (Gemini API key) |
| Base URL | `AGENT_AI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` |

**Note on the URL**: `EmbeddingService` ignores the `/openai` suffix from `baseUrl` and replaces
`v1beta` with `v1` to build the native embeddings endpoint. The rest of the app (chat)
continues using Gemini's OpenAI-compatible endpoint.

---

## Telegram bot integration

RAG is triggered automatically when `AiIntentRouter` detects an open-ended question.
The LLM classifies the message as `/rag_query` when it does not match any specific command.

Example questions that trigger RAG:
- *"What tasks are pending?"*
- *"What is the goal of the current sprint?"*
- *"Which user stories have high priority?"*
- *"Summarize the project status"*
- *"¿Qué tareas están bloqueadas?"*

The active project context from the session (`session.getActiveProjectId()`) is used to
filter Oracle results, preventing data from different projects from being mixed.

---

## Recommended flow for a new project

1. Admin creates the project, capabilities, features, user stories, and tasks in Andromeda
2. Admin calls `POST /api/admin/rag/ingest?projectId={id}` to index that project
3. The bot can now answer questions about that project

If entities are added after the initial ingest, calling the endpoint again
re-indexes only the new/modified ones (upsert handles duplicates via UUID).
