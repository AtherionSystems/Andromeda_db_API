# AI Memory — Implementación de Memoria para el Bot de Telegram

## Resumen

Se implementó un sistema de memoria en 4 fases para el bot de Telegram con AI, permitiendo que los usuarios interactúen sin necesidad de recordar IDs de entidades. La sesión del usuario se mantiene entre mensajes y sobrevive reinicios del servidor.

---

## Fase 1 — Sesión en memoria

**Objetivo:** El bot recuerda en qué entidad está trabajando el usuario sin que tenga que repetir el ID en cada mensaje.

### Archivos nuevos

| Archivo | Descripción |
|---|---|
| `telegram/ConversationSession.java` | POJO con contexto activo por usuario (proyecto, capability, feature, user story, tarea) |
| `telegram/ConversationSessionManager.java` | Gestiona sesiones por `telegramUserId` con `ConcurrentHashMap` |
| `telegram/ChatMessage.java` | Record `(role, content)` para el historial de conversación |

### Comportamiento de cascada

Al cambiar de entidad padre, se limpian automáticamente los hijos:
- `setActiveProject()` → limpia capability, feature, user story y tarea
- `setActiveCapability()` → limpia feature, user story
- `setActiveFeature()` → limpia user story

### Archivos modificados

- **`BotCommandHandler`** — todos los handlers de lectura reciben `telegramUserId` y actualizan la sesión tras cada respuesta exitosa.

---

## Fase 2 — Resolución de nombres a IDs

**Objetivo:** El usuario puede decir "proyecto Andromeda" y el bot resuelve el ID automáticamente.

### Archivos nuevos

| Archivo | Descripción |
|---|---|
| `telegram/EntityResolver.java` | Resuelve nombres a IDs por búsqueda substring case-insensitive. Soporta Project, Capability, Feature, UserStory, Task |

### Lógica de resolución en `AiIntentRouter`

```
args vacíos       → fallback a ID activo en sesión
args numéricos    → pasar directamente sin cambios
args con nombre   → EntityResolver → si no hay match → fallback a sesión → si no hay sesión → pasar nombre tal cual
```

---

## Fase 3 — Historial multi-turno

**Objetivo:** El AI recibe los turnos anteriores de la conversación para resolver referencias como "ese proyecto" o "la tarea anterior".

### Cambios en `AiService`

Se agregó `chatJsonWithHistory(systemPrompt, priorMessages, userMessage)` que construye el array de mensajes completo: `[system, ...history, user]`.

### Cambios en `AiIntentRouter`

- Usa `chatJsonWithHistory()` en lugar de `chatJson()`
- Guarda cada intercambio (`user` + `assistant`) en el historial de la sesión
- El historial tiene máximo 10 mensajes (5 intercambios) con ventana deslizante

---

## Fase 4 — Persistencia en base de datos

**Objetivo:** La sesión sobrevive reinicios del servidor.

### Archivos nuevos

| Archivo | Descripción |
|---|---|
| `model/ConversationSessionEntity.java` | Entidad JPA mapeada a `CONVERSATION_SESSIONS` |
| `repositories/ConversationSessionRepository.java` | JPA repository |
| `resources/db/migration/V6__conversation_sessions.sql` | DDL Oracle para crear la tabla |

### Tabla `CONVERSATION_SESSIONS`

```sql
CREATE TABLE conversation_sessions (
    telegram_user_id    NUMBER           NOT NULL,  -- PK (Telegram ID)
    user_id             NUMBER,                     -- FK → users(id) ON DELETE CASCADE
    active_project_id   NUMBER,
    active_project_name VARCHAR2(255),
    active_cap_id       NUMBER,
    active_cap_name     VARCHAR2(255),
    active_feature_id   NUMBER,
    active_feature_name VARCHAR2(255),
    active_story_id     NUMBER,
    active_story_title  VARCHAR2(500),
    active_task_id      NUMBER,
    active_task_title   VARCHAR2(500),
    history_json        CLOB,
    last_activity       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Decisión de diseño — Sin FKs a entidades de negocio:**
Las columnas `active_project_id`, `active_cap_id`, etc. no tienen FK a sus respectivas tablas. La sesión es una capa de caché/UI, no una relación de negocio. Si se elimina un proyecto, la sesión no debe fallar — simplemente ignora el contexto stale.

La única FK es `user_id → users(id)` porque la sesión siempre debe pertenecer a un usuario real de la aplicación, y se llena automáticamente la primera vez que el usuario interactúa (lookup por `USERS.TELEGRAM_ID`).

### Patrón write-through en `ConversationSessionManager`

```
getOrCreate(telegramUserId)
  └── cache hit  → devuelve sesión en memoria
  └── cache miss → carga de DB → guarda en cache → devuelve

setActiveXxx(...)
  └── actualiza sesión en cache
  └── persiste en DB inmediatamente (@Transactional)

persistHistory(telegramUserId)
  └── llamado por AiIntentRouter tras cada turno AI
  └── persiste el historial actualizado en DB
```

---

## Tests

| Archivo | Tests |
|---|---|
| `ConversationMemoryTest` | 19 tests: ConversationSession (6), ConversationSessionManager (5), EntityResolver (8) |
| `AiIntentRouterMemoryTest` | 10 tests: Phase 1 session fallback (5), Phase 2 name resolution (5) |

Todos los tests pasan sin necesidad de Spring context — usan `@ExtendWith(MockitoExtension.class)` con mocks de `ConversationSessionRepository` y `UserRepository`.
