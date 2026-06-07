# RAG — Retrieval-Augmented Generation

Sistema de búsqueda semántica sobre datos del proyecto para el bot de Telegram de Andromeda.
Permite a los usuarios hacer preguntas abiertas en lenguaje natural sobre historias de usuario, tareas y sprints,
y obtener respuestas basadas en datos reales del proyecto.

---

## Arquitectura

```
INGESTA (una sola vez, manual)
  Oracle Database (UserStories, Tasks, Sprints)
        ↓ formateo de texto
  EmbeddingService → API Gemini gemini-embedding-2
        ↓ vector de 3072 dimensiones
  VectorStoreService → tabla andromeda_vectors (Oracle 26ai VECTOR)

CONSULTA (en tiempo real, por pregunta)
  Pregunta del usuario
        ↓
  EmbeddingService → vector de la pregunta (3072 dims)
        ↓
  VectorStoreService → VECTOR_DISTANCE (COSINE) → top 5 fragmentos relevantes
        ↓
  AiService.chat() → Gemini con los 5 fragmentos como contexto
        ↓
  Respuesta basada en datos reales del proyecto
```

---

## Componentes

### EmbeddingService
Convierte texto en un vector numérico de 3072 dimensiones usando la API nativa de Gemini.

- **Modelo**: `gemini-embedding-2`
- **Endpoint**: `https://generativelanguage.googleapis.com/v1/models/gemini-embedding-2:embedContent`
- **Entrada**: texto libre (historia de usuario, tarea, pregunta del usuario)
- **Salida**: `float[]` de 3072 elementos

### VectorStoreService
Gestiona la persistencia y búsqueda de vectores en Oracle 26ai.

- **Tabla**: `andromeda_vectors`
- **Tipo de columna**: `VECTOR(3072, FLOAT32)` — tipo nativo de Oracle 23ai+
- **Upsert**: DELETE + INSERT con UUID determinista (`type:entityId`)
- **Búsqueda**: `ORDER BY VECTOR_DISTANCE(embedding, TO_VECTOR(?), COSINE) FETCH FIRST 5 ROWS ONLY`
- **Filtro por proyecto**: todas las consultas están acotadas al `project_id` activo del usuario

### RagIngestionService
Orquesta la ingesta de datos de Oracle hacia el vector store.

Procesa tres tipos de entidad por proyecto:

| Entidad | Repositorio | Formato de texto indexado |
|---|---|---|
| UserStory | `UserStoryRepository` | `[UserStory #id] title \n Status \| Priority \| Points \n Description \n Acceptance Criteria` |
| Tasks | `TasksRepository` | `[Task #id] title \n Status \| Priority \| Estimated hours \n Description` |
| Sprint | `SprintRepository` | `[Sprint #id] name \n Status \| Start \| End \n Goal` |

La ingesta es **idempotente**: re-indexar el mismo proyecto no crea duplicados.

### RagService
Punto de entrada para consultas RAG llamadas desde `AiIntentRouter`.

Flujo interno:
1. Genera el embedding de la pregunta del usuario
2. Busca los 5 fragmentos más similares en Oracle (filtrados por `projectId`)
3. Une los fragmentos como contexto
4. Llama a `AiService.chat()` con el prompt de sistema + contexto + pregunta
5. Devuelve la respuesta del LLM

**Comportamiento por idioma**: el bot responde en el mismo idioma en que escribe el usuario. Si el usuario pregunta en español, la respuesta es en español; si pregunta en inglés, en inglés.

### RagController
Endpoints REST para administración.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/admin/rag/ingest?projectId=X` | Indexa un proyecto específico |
| `POST` | `/api/admin/rag/ingest` | Indexa todos los proyectos |

---

## Tabla de base de datos

```sql
CREATE TABLE andromeda_vectors (
    id           VARCHAR2(150)  NOT NULL,  -- UUID: md5(type:entityId)
    entity_type  VARCHAR2(50)   NOT NULL,  -- "user_story" | "task" | "sprint"
    entity_id    NUMBER         NOT NULL,  -- ID del registro original
    project_id   NUMBER,                   -- usado para filtrar por proyecto en las consultas
    text_content VARCHAR2(4000) NOT NULL,  -- texto legible enviado al LLM
    embedding    VECTOR(3072, FLOAT32),    -- representación semántica
    created_at   TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT pk_andromeda_vectors PRIMARY KEY (id)
);
```

**Nota**: la tabla se crea manualmente (Flyway está deshabilitado en este proyecto).
El archivo de referencia es `src/main/resources/db/migration/V7__rag_vector_store.sql`.

---

## Configuración

| Propiedad | Variable de entorno | Valor actual |
|---|---|---|
| Modelo de embedding | `AGENT_AI_EMBEDDING_MODEL` | `gemini-embedding-2` |
| API Key | `AGENT_AI_API_KEY` | (clave de API de Gemini) |
| URL base | `AGENT_AI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` |

**Nota sobre la URL**: `EmbeddingService` ignora el sufijo `/openai` de `baseUrl` y reemplaza
`v1beta` con `v1` para construir el endpoint nativo de embeddings. El resto de la aplicación (chat)
sigue usando el endpoint de Gemini compatible con OpenAI.

---

## Integración con el bot de Telegram

RAG se activa automáticamente cuando `AiIntentRouter` detecta una pregunta abierta.
El LLM clasifica el mensaje como `/rag_query` cuando no coincide con ningún comando específico.

Ejemplos de preguntas que activan RAG:
- *"¿Qué tareas están pendientes?"*
- *"¿Cuál es el objetivo del sprint actual?"*
- *"¿Qué historias de usuario tienen prioridad alta?"*
- *"Resume el estado del proyecto"*
- *"What tasks are blocked?"*

El contexto de proyecto activo de la sesión (`session.getActiveProjectId()`) se usa para
filtrar los resultados de Oracle, evitando que se mezclen datos de diferentes proyectos.

---

## Flujo recomendado para un proyecto nuevo

1. El administrador crea el proyecto, capabilities, features, historias de usuario y tareas en Andromeda
2. El administrador llama a `POST /api/admin/rag/ingest?projectId={id}` para indexar ese proyecto
3. El bot ya puede responder preguntas sobre ese proyecto

Si se agregan entidades después de la ingesta inicial, volver a llamar al endpoint
re-indexa solo las nuevas o modificadas (el upsert maneja duplicados mediante UUID).
