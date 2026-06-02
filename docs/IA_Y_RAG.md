# IA y RAG

La inteligencia del sistema tiene tres componentes: la **capa de IA generativa** (chat con un LLM), el sistema **RAG** (respuestas fundamentadas en los datos reales del proyecto) y la **memoria conversacional** del bot. Todo se consume principalmente desde el bot de Telegram, pero también hay endpoints REST.

---

## 1. Capa de IA generativa (`services/AiService`)

`AiService` es el cliente del LLM. Habla con el **endpoint compatible con OpenAI de Gemini** (`/chat/completions`) usando `RestClient` de Spring.

- Configuración vía `config/AiProperties` (`@ConfigurationProperties(prefix="agent.ai")`):

  | Propiedad | Variable de entorno | Valor por defecto |
  |---|---|---|
  | `enabled` | `AGENT_AI_ENABLED` | `true` |
  | `baseUrl` | `AGENT_AI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` |
  | `apiKey` | `AGENT_AI_API_KEY` | (API key de Gemini) |
  | `model` | `AGENT_AI_MODEL` | `gemini-flash-latest` (`.env.example` usa `gemini-2.0-flash`) |
  | `embeddingModel` | `AGENT_AI_EMBEDDING_MODEL` | `gemini-embedding-2` |

- Métodos clave:
  - `chat(systemPrompt, userMessage)` — respuesta de texto. Limpia bloques `<think>...</think>` que producen algunos modelos de razonamiento.
  - `chatJson(...)` y `chatJsonWithHistory(systemPrompt, priorMessages, userMessage)` — devuelven JSON; el segundo arma el array completo `[system, ...history, user]` para conversaciones multi-turno.
  - `isEnabled()` / `getModel()`.
- Si `agent.ai.enabled=false`, `chat()` devuelve `null` y las funciones de IA se desactivan limpiamente.

### Comandos de IA del bot

Apoyados en `AiService` (ver `BotCommandHandler`):

| Comando | Función |
|---|---|
| `/suggest <projectId>` | Sugerencias de IA para un proyecto |
| `/analyze <projectId>` | Análisis de salud del proyecto |
| `/fix <taskId>` | Guía de IA para resolver una tarea |
| `/pingai` | Prueba de conectividad con el backend de IA |

---

## 2. RAG — Retrieval-Augmented Generation

Permite preguntas abiertas en lenguaje natural sobre historias, tareas y sprints, con respuestas fundamentadas en los datos reales de Oracle. Documento técnico de referencia: [`RAG.md`](RAG.md).

### Flujo

```
INGESTA (manual, por proyecto)
  Oracle (UserStories, Tasks, Sprints)
    → formateo a texto
    → EmbeddingService (Gemini gemini-embedding-2) → vector 3072 dims
    → VectorStoreService → tabla andromeda_vectors (VECTOR de Oracle 26ai)

CONSULTA (en tiempo real, por pregunta)
  Pregunta del usuario
    → EmbeddingService → vector de la pregunta
    → VectorStoreService → VECTOR_DISTANCE (COSINE) → top 5 chunks (filtrados por project_id)
    → AiService.chat() con los 5 chunks como contexto
    → respuesta fundamentada
```

### Componentes

| Clase | Rol |
|---|---|
| `services/EmbeddingService` | Convierte texto en `float[3072]` con la API nativa de Gemini. Ajusta la URL: ignora el sufijo `/openai` del `baseUrl` y cambia `v1beta`→`v1` para el endpoint `embedContent` |
| `services/VectorStoreService` | Persiste y busca vectores en `andromeda_vectors`. Upsert = DELETE+INSERT con UUID determinista (`md5(type:entityId)`). Búsqueda: `ORDER BY VECTOR_DISTANCE(embedding, TO_VECTOR(?), COSINE) FETCH FIRST 5 ROWS ONLY`, siempre filtrada por `project_id` |
| `services/RagIngestionService` | Orquesta la ingesta de UserStories, Tasks y Sprints por proyecto. **Idempotente**: re-indexar no duplica (gracias al UUID) |
| `services/RagService` | Punto de entrada de consultas RAG (lo llama `AiIntentRouter`) |
| `controllers/RagController` | Administración: `POST /api/admin/rag/ingest?projectId=X` (indexa un proyecto), `POST /api/admin/rag/ingest` (todos) |

### Comportamiento

- **Idioma:** el bot responde en el mismo idioma en que pregunta el usuario.
- **Aislamiento por proyecto:** todas las consultas se filtran por `session.getActiveProjectId()`, evitando mezclar datos de proyectos distintos.
- **Flujo recomendado para un proyecto nuevo:** crear las entidades en Andromeda → `POST /api/admin/rag/ingest?projectId={id}` → el bot ya puede responder. Si se agregan entidades después, volver a llamar el endpoint re-indexa solo lo nuevo/modificado.

---

## 3. Router de intención (`telegram/AiIntentRouter`)

Traduce un mensaje en lenguaje natural al comando del bot más cercano. Le pasa al LLM un *system prompt* con la jerarquía del dominio (Project → Capabilities → Features → User Stories → Tasks) y el catálogo de comandos disponibles, y exige una respuesta **solo JSON**: `{"cmd": "/comando", "args": "..."}`.

- Si el mensaje es una pregunta abierta (p. ej. "¿qué tareas están pendientes?", "resume el sprint"), mapea a `/rag_query` y dispara el flujo RAG.
- Si no puede mapear, devuelve `{"cmd": "none", "args": ""}`.
- Usa `chatJsonWithHistory()` para resolver referencias como "ese proyecto" o "la tarea anterior", y guarda cada intercambio (user + assistant) en el historial de la sesión.
- Resolución de argumentos:
  - args vacíos → usa el ID activo de la sesión;
  - args numéricos → se pasan tal cual;
  - args con nombre → `EntityResolver` (búsqueda substring case-insensitive) → si no hay match, fallback a la sesión → si tampoco, se pasa el nombre tal cual.

---

## 4. Memoria conversacional

Implementada en 4 fases. Documento de referencia: [`AI_MEMORY.md`](AI_MEMORY.md).

- **Fase 1 — Sesión en memoria:** `ConversationSession` (contexto activo por usuario), `ConversationSessionManager` (`ConcurrentHashMap` por `telegramUserId`), `ChatMessage` (record `role`/`content`). Cascada: cambiar de entidad padre limpia los hijos.
- **Fase 2 — Resolución de nombres:** `EntityResolver` resuelve nombres → IDs (Project, Capability, Feature, UserStory, Task).
- **Fase 3 — Historial multi-turno:** ventana deslizante de máx. 10 mensajes (5 intercambios).
- **Fase 4 — Persistencia:** `ConversationSessionEntity` + `ConversationSessionRepository` + tabla `CONVERSATION_SESSIONS` (V6). Patrón write-through: cache en memoria + persistencia inmediata en BD, de modo que la sesión sobrevive reinicios.

---

## 5. Endpoints REST de IA

| Método | Ruta | Controller | Función |
|---|---|---|---|
| `POST` | `/api/ai/notify` | `AiNotifyController` | Notificación/disparo de IA |
| `GET` | `/api/ai/status` | `AiNotifyController` | Estado/disponibilidad de la IA |
| `POST` | `/api/admin/rag/ingest[?projectId=X]` | `RagController` | Ingesta de embeddings |
