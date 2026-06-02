# Modelo de datos

Detalle de todas las tablas, su jerarquía, relaciones, enumeraciones (vía `CHECK`) y el historial de migraciones. La fuente de verdad son los scripts Flyway en `src/main/resources/db/migration/`.

> **Importante:** Flyway está **deshabilitado en runtime** (`spring.flyway.enabled=false`) e Hibernate corre con `ddl-auto=none`. Los scripts `V*.sql` describen el esquema, pero se aplican **manualmente** sobre la Oracle Autonomous Database. Esto incluye `V7` (tabla de vectores), que se crea a mano.

---

## 1. Jerarquía del dominio

```
PROJECTS
  ├── CAPABILITIES
  │     └── FEATURES
  │           └── USER_STORIES
  │                 ├── TASKS                (tasks.user_story_id → user_stories.id)
  │                 └── USER_STORY_DEPENDENCIES (historia ↔ historia)
  ├── SPRINTS
  │     ├── SPRINT_STORIES_ASSIGNMENTS   (sprint ↔ user_story)
  │     ├── SPRINT_RETROSPECTIVES        (1:1 con sprint)
  │     └── STORY_SPILLOVERS             (historia que pasa de un sprint a otro)
  ├── PROJECT_MEMBERS                    (project ↔ user con rol)
  └── TECHNICAL_DEBT                     (asociada a project, opcionalmente a story/task)

USERS ── USER_TYPE
TASKS ── TASK_ASSIGNMENTS ── USERS
LOGS                                     (auditoría transversal)
CONVERSATION_SESSIONS                    (sesión del bot, por usuario de Telegram)
ANDROMEDA_VECTORS                        (embeddings para RAG)
```

Patrón de auditoría: a partir de V5, la mayoría de las tablas de negocio (`projects`, `sprints`, `tasks`, `capabilities`, `features`, `user_stories`, etc.) incluyen `created_by`, `created_at`, `updated_by`, `updated_at` con FKs a `users` y un `CHECK (updated_at IS NULL OR updated_at >= created_at)`.

---

## 2. Tablas base (V1)

### `USER_TYPE`
Tipos/roles de usuario. `id` (PK, identity), `user_type` (único), `description`, `created_at`.

### `USERS`
Usuarios registrados.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | NUMBER (identity) | PK |
| `user_type_id` | NUMBER | FK → `user_type` |
| `name` | VARCHAR2(255) | |
| `username` | VARCHAR2(50) | único |
| `password_hash` | VARCHAR2(255) | BCrypt (en OAuth se guarda `"OAUTH"`) |
| `email` | VARCHAR2(255) | único |
| `phone` | VARCHAR2(20) | |
| `created_at` | TIMESTAMP | |
| `telegram_id` | NUMBER | añadido en V2, índice único — vincula la cuenta de Telegram |
| `iam_sub` | (añadido para OAuth) | identifica al usuario en OCI IAM |

### `PROJECTS`
`id`, `name`, `description` (CLOB), `status` ∈ {`active`, `paused`, `completed`, `cancelled`} (default `active`), `start_date`, `end_date`, `created_at`. V5 añade `created_by`, `updated_by`, `updated_at`.

### `PROJECT_MEMBERS`
Relación proyecto↔usuario con rol. `project_id` (FK), `user_id` (FK), `role` ∈ {`owner`, `manager`, `member`} (default `member`), `joined_at`. Restricción única `(project_id, user_id)`.

### `SPRINTS`
`id`, `project_id` (FK), `name`, `goal`, `status` ∈ {`planned`, `active`, `completed`} (default `planned`), `start_date`, `due_date`, `actual_end`, `created_at`. V5 añade auditoría.

### `TASKS`
| Columna | Tipo / valores | Notas |
|---|---|---|
| `id` | NUMBER identity | PK |
| `project_id` | NUMBER | FK → `projects` |
| `title` | VARCHAR2(255) | |
| `description` | CLOB | |
| `priority` | {`low`, `medium`, `high`, `critical`} | default `medium` |
| `status` | {`todo`, `in_progress`, `review`, `done`} | default `todo` |
| `start_date` / `due_date` / `actual_end` | TIMESTAMP | |
| `estimated_hours` | NUMBER(4,1) | máx 4 h por tarea (regla aplicada en el bot) |
| `actual_hours` | NUMBER(4,1) | registrado al completar (`/completetask`) |
| `user_story_id` | NUMBER | FK → `user_stories` (V5) |
| `created_by` / `updated_by` / `created_at` / `updated_at` | | auditoría (V5) |

> Nota histórica: `story_points` y `acceptance_criteria` se añadieron a `tasks` en V2/V3 y luego se **eliminaron** en V5 (esos conceptos viven ahora en `user_stories`).

### `TASK_ASSIGNMENTS`
Asignación tarea↔usuario. `task_id` (FK), `user_id` (FK), `assigned_at`. Único `(task_id, user_id)`.

### `LOGS`
Auditoría genérica. `user_id` (FK, nullable), `entity`, `entity_id`, `action`, `detail`, `log_date`. Índice por `(entity, entity_id)`.

> `SPRINT_TASKS` existió en V1 pero fue **eliminada (`DROP TABLE`) en V5**; la relación sprint↔trabajo pasó a `SPRINT_STORIES_ASSIGNMENTS`.

---

## 3. Jerarquía ágil (V5)

### `CAPABILITIES`
Pertenecen a un proyecto. `project_id` (FK), `name`, `description`, `status` ∈ {`active`, `completed`, `cancelled`}, + auditoría.

### `FEATURES`
Pertenecen a una capability. `capability_id` (FK), `name`, `description`, `status` ∈ {`active`, `completed`, `cancelled`}, + auditoría.

### `USER_STORIES`
Pertenecen a una feature.

| Columna | Valores | Notas |
|---|---|---|
| `feature_id` | | FK → `features` |
| `title` / `description` / `acceptance_criteria` | | criterios de aceptación = definición de "hecho" |
| `priority` | {`low`, `medium`, `high`, `critical`} | default `medium` |
| `status` | {`todo`, `in_progress`, `review`, `done`} | default `todo` |
| `story_points` | NUMBER(3) | métrica de velocidad |
| `owner_id` | | FK → `users` |
| auditoría | | `created_by` obligatorio |

### `USER_STORY_DEPENDENCIES`
Dependencias entre historias. `story_id` (FK), `blocked_by_id` (FK), `dependency_type` ∈ {`blocks`, `related`, `duplicates`, `split_from`, `parent_child`} (default `blocks`). Único `(story_id, blocked_by_id)` y `CHECK (story_id != blocked_by_id)` para evitar auto-dependencias.

### `SPRINT_STORIES_ASSIGNMENTS`
Asignación de historias a sprints (reemplaza a `sprint_tasks`). `sprint_id` (FK), `user_story_id` (FK), `added_at`, `removed_at`, `moved_to` (FK → sprints), `is_active` ∈ {0,1} (default 1).

Índice único parcial `uq_ss_one_active`: garantiza **una sola asignación activa** por `(sprint_id, user_story_id)` (usando expresiones `CASE ... is_active`).

### `SPRINT_RETROSPECTIVES`
Una por sprint (`sprint_id` único). `summary`, `what_went_well`, `what_went_wrong` (todos CLOB), + auditoría.

### `STORY_SPILLOVERS`
Historias que se "derraman" de un sprint a otro. `sprint_story_id` (FK → sprint_stories_assignments), `user_story_id` (FK), `origin_sprint_id` (FK), `destination_sprint_id` (FK), `reason` ∈ {`scope_change`, `blocked`, `underestimated`, `resource_unavailable`, `technical_issue`, `other`}, `detail`, + auditoría.

### `TECHNICAL_DEBT`
Deuda técnica del proyecto. `project_id` (FK), `user_story_id` (FK opcional), `task_id` (FK opcional), `title`, `description`, `debt_type` ∈ {`code_quality`, `missing_tests`, `security`, `performance`, `documentation`, `architecture`}, `priority` ∈ {`low`, `medium`, `high`, `critical`}, `status` ∈ {`open`, `in_progress`, `resolved`, `accepted`}, `assigned_to` (FK, obligatorio), `resolved_at`, + auditoría.

---

## 4. Tablas de soporte de IA

### `CONVERSATION_SESSIONS` (V6)
Contexto y memoria del bot, una fila por usuario de Telegram. PK = `telegram_user_id`. Columnas de contexto activo (`active_project_id/name`, `active_cap_id/name`, `active_feature_id/name`, `active_story_id/title`, `active_task_id/title`), `history_json` (CLOB con el historial multi-turno) y `last_activity`.

- Única FK: `user_id → users(id) ON DELETE CASCADE` (nullable: se llena cuando el usuario vincula su cuenta).
- **Sin FKs a entidades de negocio** a propósito: la sesión es una capa de caché/UI; si se borra un proyecto, la sesión simplemente ignora el contexto obsoleto en lugar de fallar.

### `ANDROMEDA_VECTORS` (V7)
Almacén de embeddings para RAG. `id` (VARCHAR2(150), UUID determinista `md5(type:entityId)`), `entity_type` ∈ {`user_story`, `task`, `sprint`}, `entity_id`, `project_id` (para filtrar por proyecto), `text_content` (VARCHAR2(4000), texto legible enviado al LLM), `embedding` (`VECTOR(3072, FLOAT32)`, tipo nativo Oracle 23ai+), `created_at`. Índice por `project_id`.

---

## 5. Historial de migraciones Flyway

| Versión | Archivo | Cambios |
|---|---|---|
| V1 | `V1__initial_setup.sql` | Tablas base: user_type, users, projects, project_members, sprints, tasks, task_assignments, sprint_tasks, logs + índices |
| V2 | `V2__agile_fields.sql` | `users.telegram_id`; campos ágiles en tasks (`estimated_hours`, `actual_hours`, `story_points`, `acceptance_criteria`) |
| V3 | `V3__add_acceptance_criteria.sql` | `acceptance_criteria` como `VARCHAR2(4000)` (en vez de CLOB) por restricciones de Oracle ADB con LOB |
| V4 | `V4__ensure_task_columns.sql` | Bloque PL/SQL idempotente que asegura las columnas ágiles (Oracle ADB ignora silenciosamente un `ALTER TABLE ADD` multi-columna que incluya un LOB; se manejan ORA-01430 "columna ya existe") |
| V5 | `V5_update_bd.sql` | Refactor mayor: auditoría en tablas de negocio; nuevas tablas capabilities, features, user_stories, user_story_dependencies, sprint_stories_assignments, sprint_retrospectives, story_spillovers, technical_debt; `tasks.user_story_id`; **DROP** de sprint_tasks; quita story_points/acceptance_criteria de tasks |
| V6 | `V6__conversation_sessions.sql` | Tabla `conversation_sessions` (memoria del bot) |
| V7 | `V7__rag_vector_store.sql` | Tabla `andromeda_vectors` con tipo `VECTOR` (RAG) |

### Lecciones de Oracle Autonomous Database

- Un `ALTER TABLE ADD` con varias columnas, una de ellas LOB, puede **ignorarse en silencio**. Por eso V4 añade cada columna por separado dentro de PL/SQL y tolera ORA-01430.
- El tipo `VECTOR` requiere Oracle 23ai/26ai. La tabla de V7 se crea manualmente.
