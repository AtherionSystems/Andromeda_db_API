# RAG — Retrieval-Augmented Generation

Sistema de búsqueda semántica sobre datos del proyecto para el bot de Telegram de Andromeda.
Permite al usuario hacer preguntas en lenguaje natural sobre user stories, tareas y sprints,
y obtener respuestas basadas en los datos reales del proyecto.

---

## Arquitectura

```
INGESTA (una vez, manual)
  Base de datos Oracle (UserStories, Tasks, Sprints)
        ↓ formateo de texto
  EmbeddingService → Gemini gemini-embedding-2 API
        ↓ vector de 3072 números
  VectorStoreService → tabla andromeda_vectors (Oracle 26ai VECTOR)

CONSULTA (en tiempo real, por cada pregunta)
  Pregunta del usuario
        ↓
  EmbeddingService → vector de la pregunta (3072 dims)
        ↓
  VectorStoreService → VECTOR_DISTANCE (COSINE) → top 5 chunks relevantes
        ↓
  AiService.chat() → Gemini con los 5 chunks como contexto
        ↓
  Respuesta basada en datos reales del proyecto
```

---

## Componentes

### EmbeddingService
Convierte texto en un vector numérico de 3072 dimensiones usando la API nativa de Gemini.

- **Modelo**: `gemini-embedding-2`
- **Endpoint**: `https://generativelanguage.googleapis.com/v1/models/gemini-embedding-2:embedContent`
- **Autenticación**: header `x-goog-api-key`
- **Entrada**: texto libre (user story, task, pregunta del usuario)
- **Salida**: `float[]` de 3072 elementos

### VectorStoreService
Gestiona la persistencia y búsqueda de vectores en Oracle 26ai.

- **Tabla**: `andromeda_vectors`
- **Tipo de columna**: `VECTOR(3072, FLOAT32)` — tipo nativo de Oracle 23ai+
- **Upsert**: DELETE + INSERT con UUID determinístico (`type:entityId`)
- **Búsqueda**: `ORDER BY VECTOR_DISTANCE(embedding, TO_VECTOR(?), COSINE) FETCH FIRST 5 ROWS ONLY`
- **Filtro por proyecto**: todas las consultas se acotan al `project_id` del usuario activo

### RagIngestionService
Orquesta la ingesta de datos desde Oracle hacia el vector store.

Procesa tres tipos de entidades por proyecto:

| Entidad | Repositorio | Formato del texto indexado |
|---|---|---|
| UserStory | `UserStoryRepository` | `[UserStory #id] título \n Estado \| Prioridad \| Puntos \n Descripción \n Criterios` |
| Tasks | `TasksRepository` | `[Task #id] título \n Estado \| Prioridad \| Horas estimadas \n Descripción` |
| Sprint | `SprintRepository` | `[Sprint #id] nombre \n Estado \| Inicio \| Fin \n Objetivo` |

La ingesta es **idempotente**: re-indexar el mismo proyecto no genera duplicados.

### RagService
Punto de entrada para consultas RAG desde `AiIntentRouter`.

Flujo interno:
1. Embeds la pregunta del usuario
2. Busca los 5 chunks más similares en Oracle (filtrado por `projectId`)
3. Une los chunks como contexto
4. Llama a `AiService.chat()` con el system prompt + contexto + pregunta
5. Retorna la respuesta del LLM

### RagController
Endpoints REST de administración.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/admin/rag/ingest?projectId=X` | Indexa un proyecto específico |
| `POST` | `/api/admin/rag/ingest` | Indexa todos los proyectos |

---

## Tabla en base de datos

```sql
CREATE TABLE andromeda_vectors (
    id           VARCHAR2(150)  NOT NULL,  -- UUID: md5(type:entityId)
    entity_type  VARCHAR2(50)   NOT NULL,  -- "user_story" | "task" | "sprint"
    entity_id    NUMBER         NOT NULL,  -- ID del registro original
    project_id   NUMBER,                   -- para filtrar por proyecto en búsquedas
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
| API Key | `AGENT_AI_API_KEY` | (Gemini API key) |
| Base URL | `AGENT_AI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` |

**Nota sobre la URL**: el `EmbeddingService` ignora el sufijo `/openai` de `baseUrl` y reemplaza
`v1beta` por `v1` para construir el endpoint nativo de embeddings. El resto de la app (chat)
sigue usando el endpoint OpenAI-compatible de Gemini.

---

## Integración con el bot de Telegram

El RAG se activa automáticamente cuando `AiIntentRouter` detecta una pregunta abierta.
El LLM clasifica el mensaje como `/rag_query` cuando no corresponde a ningún comando específico.

Ejemplos de preguntas que activan el RAG:
- *"¿Qué tareas están pendientes?"*
- *"¿Cuál es el objetivo del sprint actual?"*
- *"¿Qué user stories tienen prioridad alta?"*
- *"Resume el estado del proyecto"*

El contexto del proyecto activo en sesión (`session.getActiveProjectId()`) se usa para
filtrar los resultados de Oracle, evitando mezclar datos de distintos proyectos.

---

## Flujo recomendado para un proyecto nuevo

1. Admin crea el proyecto, capabilities, features, user stories y tasks en Andromeda
2. Admin llama `POST /api/admin/rag/ingest?projectId={id}` para indexar ese proyecto
3. El bot ya puede responder preguntas sobre ese proyecto

Si se agregan entidades después del ingest inicial, volver a llamar el endpoint
re-indexa solo las nuevas/modificadas (el upsert maneja duplicados por UUID).
