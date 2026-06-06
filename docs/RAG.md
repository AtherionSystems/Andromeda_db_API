# RAG — Retrieval-Augmented Generation

Sistema de búsqueda semántica sobre datos del proyecto para el bot de Telegram de Andromeda.
Permite a los usuarios hacer preguntas abiertas en lenguaje natural sobre historias de usuario, tareas y sprints,
y obtener respuestas basadas en datos reales del proyecto.

---

## Arquitectura

```
INGESTA (automática al guardar/borrar)
  save(UserStory / Task / Sprint)
        ↓ async (no bloquea el endpoint)
  EmbeddingService → API Gemini gemini-embedding-2
        ↓ vector de 3072 dimensiones
  VectorStoreService → upsert en andromeda_vectors (Oracle 26ai VECTOR)

CONSULTA (en tiempo real, por pregunta)
  Pregunta del usuario
        ↓
  EmbeddingService → vector de la pregunta (3072 dims)
        ↓
  VectorStoreService → VECTOR_DISTANCE (COSINE) < 0.5 → fragmentos relevantes
        ↓
  AiService.chat() → Gemini con los fragmentos como contexto
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
- **Búsqueda**: filtra por `VECTOR_DISTANCE(COSINE) < 0.5` y devuelve hasta 10 resultados ordenados por relevancia
- **Filtro por proyecto**: todas las consultas están acotadas al `project_id` activo del usuario
- **Delete**: elimina el vector correspondiente a una entidad por su UUID determinista

### RagIngestionService
Orquesta la ingesta de datos de Oracle hacia el vector store.

Procesa tres tipos de entidad:

| Entidad | Formato de texto indexado |
|---|---|
| UserStory | `[UserStory #id] title \n Status \| Priority \| Points \n Owner: name \n Asignados: user1, user2 \n Description \n Acceptance Criteria` |
| Tasks | `[Task #id] title \n Status \| Priority \| Estimated hours \n Asignados: user1, user2 \n Description` |
| Sprint | `[Sprint #id] name \n Status \| Start \| End \n Goal` |

La ingesta es **idempotente**: re-indexar la misma entidad no crea duplicados.

Expone métodos `@Async @Transactional` llamados desde los services. Reciben el ID (no la entidad) para recargar las asociaciones lazy en una nueva transacción desde el hilo async.

| Método | Cuándo se llama |
|---|---|
| `ingestUserStoryAsync(storyId)` | `UserStoryService.save()` |
| `ingestTaskAsync(taskId)` | `TasksService.save()` — también `TaskAssignmentService.save()` y `TaskAssignmentService.deleteById()` para mantener los asignados actualizados en el vector |
| `ingestSprintAsync(sprintId)` | `SprintService.save()` |
| `deleteAsync(type, entityId)` | `*.deleteById()` en cada service, incluidos `FeatureService` y `CapabilityService` |

También expone `ingest(projectId)` para re-indexación completa manual vía `RagController`.

### RagService
Punto de entrada para consultas RAG llamadas desde `AiIntentRouter`.

Flujo interno:
1. Genera el embedding de la pregunta del usuario
2. Busca en Oracle los fragmentos con distancia coseno < 0.5 (hasta 10, filtrados por `projectId`)
3. Si no hay fragmentos suficientemente relevantes, retorna `null` (el bot indica que no encontró información)
4. Une los fragmentos como contexto
5. Llama a `AiService.chat()` con el prompt de sistema + contexto + pregunta
6. Devuelve la respuesta del LLM

**Umbral de distancia**: el valor 0.5 significa que solo se usan fragmentos con al menos ~75% de alineación semántica con la pregunta, evitando que el LLM reciba contexto irrelevante y alucine.

**Comportamiento por idioma**: el bot responde en el mismo idioma en que escribe el usuario.

### RagController
Endpoints REST para re-indexación completa manual.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/admin/rag/ingest?projectId=X` | Re-indexa un proyecto específico |
| `POST` | `/api/admin/rag/ingest` | Re-indexa todos los proyectos |

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

**Sin FK a tablas de negocio**: los vectores no tienen restricción referencial con `user_stories`, `tasks` o `sprints`. Los borrados en cascada que pasan por `FeatureService.deleteById()` o `CapabilityService.deleteById()` ahora limpian los vectores automáticamente antes de borrar la entidad padre. Si se borran datos directamente en DB sin pasar por la API, ejecutar `POST /api/admin/rag/ingest?projectId=X`.

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

## Flujo para un proyecto nuevo

1. Crear el proyecto, capabilities, features, historias de usuario, tareas y sprints en Andromeda — **la indexación ocurre automáticamente** al guardar cada entidad
2. El bot ya puede responder preguntas sobre ese proyecto sin ningún paso adicional

### Re-indexación manual

Útil cuando:
- Se borraron entidades en cascada (los vectores huérfanos quedaron en Oracle)
- Se migró o importó datos directamente en la DB sin pasar por la API

```
POST /api/admin/rag/ingest?projectId={id}   # un proyecto
POST /api/admin/rag/ingest                  # todos los proyectos
```
