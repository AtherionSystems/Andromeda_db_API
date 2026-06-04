# Andromeda API — Referencia de Endpoints

URL base (local): `http://localhost:8080`

Todos los cuerpos de request y response usan `application/json`.  
Las fechas usan formato ISO-8601 (`2025-06-15T00:00:00Z` para `Instant`, `2025-06-15T10:30:00` para `LocalDateTime`).

---

## Tabla de Contenidos

- [Raíz y Health](#raíz-y-health)
- [Auth](#auth)
- [Usuarios](#usuarios)
- [Proyectos](#proyectos)
- [Tareas](#tareas)
- [Asignaciones de Tarea](#asignaciones-de-tarea)
- [Sprints](#sprints)
- [Tareas de Sprint](#tareas-de-sprint)
- [Asignaciones de Historia a Sprint](#asignaciones-de-historia-a-sprint)
- [Miembros de Proyecto](#miembros-de-proyecto)
- [Logs](#logs)
- [Dashboard](#dashboard)
- [Notificaciones de IA](#notificaciones-de-ia)
- [Respuestas de Error](#respuestas-de-error)
- [Valores de Enums](#valores-de-enums)
- [Endpoints del Product Backlog (V5)](#endpoints-del-product-backlog-v5)

---

## Raíz y Health

### `GET /`
Devuelve metadatos de la API y una lista de grupos de endpoints disponibles.

**Respuesta `200`**
```json
{
  "service": "Andromeda Backend API",
  "version": "0.0.1",
  "endpoints": [...]
}
```

---

### `GET /health`
Verificación de disponibilidad (liveness check).

**Respuesta `200`**
```json
{
  "status": "UP"
}
```

---

## Auth

### `POST /api/auth/register`
Registra una nueva cuenta de usuario.

**Cuerpo de la request**
```json
{
  "name": "Santiago Quiroz",
  "username": "santiago",
  "password": "secret123",
  "email": "santiago@example.com",
  "phone": "+521234567890",
  "userTypeId": 1
}
```

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| name | string | sí | |
| username | string | sí | Debe ser único |
| password | string | sí | Se almacena como hash BCrypt |
| email | string | sí | Debe ser único |
| phone | string | no | |
| userTypeId | number | sí | FK a la tabla user_type |

**Respuesta `201`**
```json
{
  "id": 3,
  "name": "Santiago Quiroz",
  "username": "santiago",
  "email": "santiago@example.com",
  "phone": "+521234567890",
  "userTypeId": 1,
  "userType": "developer",
  "createdAt": "2025-04-14T10:00:00"
}
```

**Errores**
| Status | Razón |
|---|---|
| `409` | El nombre de usuario o email ya está en uso |
| `400` | Falta un campo requerido |

---

### `POST /api/auth/login`
Autentica a un usuario existente.

**Cuerpo de la request**
```json
{
  "username": "santiago",
  "password": "secret123"
}
```

**Respuesta `200`** — misma forma que la respuesta de registro.

**Errores**
| Status | Razón |
|---|---|
| `401` | Credenciales inválidas |
| `404` | Usuario no encontrado |

---

## Usuarios

### `GET /api/users`
Lista todos los usuarios.

**Respuesta `200`**
```json
[
  {
    "id": 1,
    "name": "Javier García",
    "username": "javier",
    "email": "javier@example.com",
    "phone": null,
    "userTypeId": 1,
    "userType": "admin",
    "createdAt": "2025-01-10T09:00:00"
  }
]
```

> `telegramId` es un campo interno gestionado por el comando `/link` del bot y no se expone en las respuestas de la API.

---

### `GET /api/users/{id}`
Obtiene un usuario por ID.

**Respuesta `200`** — objeto de usuario único (misma forma que arriba).

**Errores**
| Status | Razón |
|---|---|
| `404` | Usuario no encontrado |

---

### `PUT /api/users/{id}`
Actualiza un usuario. Todos los campos son opcionales; solo se modifican los campos enviados.

**Cuerpo de la request**
```json
{
  "name": "Javier G.",
  "email": "javier.new@example.com",
  "phone": "+521111111111",
  "password": "newpassword",
  "username": "javier2",
  "userTypeId": 2
}
```

**Respuesta `200`** — objeto de usuario actualizado.

**Errores**
| Status | Razón |
|---|---|
| `404` | Usuario no encontrado |
| `409` | El nombre de usuario o email ya está en uso |

---

### `DELETE /api/users/{id}`
Elimina un usuario.

**Respuesta `204`** — sin cuerpo.

**Errores**
| Status | Razón |
|---|---|
| `404` | Usuario no encontrado |

---

## Proyectos

Alias disponible para todas las rutas de proyectos en esta sección: `/projects`.

### `GET /api/projects`
Lista todos los proyectos.

**Respuesta `200`**
```json
[
  {
    "id": 1,
    "name": "Andromeda Backend",
    "description": "Main API project",
    "status": "active",
    "startDate": "2025-01-15T00:00:00Z",
    "endDate": "2025-12-31T00:00:00Z",
    "createdAt": "2025-01-10T09:00:00Z"
  }
]
```

---

### `GET /api/projects/{id}`
Obtiene un proyecto por ID.

**Respuesta `200`** — objeto de proyecto único (misma forma que arriba).

**Errores**
| Status | Razón |
|---|---|
| `404` | Proyecto no encontrado |

---

### `POST /api/projects`
Crea un nuevo proyecto.

**Cuerpo de la request**
```json
{
  "name": "New Project",
  "description": "Optional description",
  "status": "active",
  "startDate": "2025-05-01T00:00:00Z",
  "endDate": "2025-11-01T00:00:00Z"
}
```

| Campo | Tipo | Requerido | Default |
|---|---|---|---|
| name | string | sí | — |
| description | string | no | null |
| status | string | no | `active` |
| startDate | Instant | no | null |
| endDate | Instant | no | null |

**Respuesta `201`** — objeto de proyecto creado.

---

### `PATCH /api/projects/{id}`
Actualiza parcialmente un proyecto. Solo se modifican los campos enviados.

**Cuerpo de la request** — misma forma que POST, todos los campos opcionales.

**Respuesta `200`** — objeto de proyecto actualizado.

**Errores**
| Status | Razón |
|---|---|
| `404` | Proyecto no encontrado |

---

### `DELETE /api/projects/{id}`
Elimina un proyecto.

**Respuesta `204`** — sin cuerpo.

**Errores**
| Status | Razón |
|---|---|
| `404` | Proyecto no encontrado |

---

## Tareas

Todos los endpoints de tareas están anidados bajo un proyecto: `/api/projects/{projectId}/tasks`.

### `GET /api/projects/{projectId}/tasks`
Lista todas las tareas de un proyecto.

**Respuesta `200`**
```json
[
  {
    "id": 3,
    "title": "Set up CI/CD pipeline",
    "description": null,
    "priority": "high",
    "status": "in_progress",
    "estimatedHours": 3.0,
    "actualHours": null,
    "storyPoints": 5,
    "acceptanceCriteria": "Pipeline must run on every push to main",
    "startDate": "2025-02-01T00:00:00Z",
    "dueDate": "2025-03-15T00:00:00Z",
    "actualEnd": null,
    "createdAt": "2025-01-20T08:00:00Z"
  }
]
```

---

### `GET /api/projects/{projectId}/tasks/{taskId}`
Obtiene una tarea por ID.

**Respuesta `200`** — objeto de tarea único (misma forma que arriba).

**Errores**
| Status | Razón |
|---|---|
| `404` | Tarea no encontrada |

---

### `POST /api/projects/{projectId}/tasks`
Crea una tarea dentro de un proyecto.

**Cuerpo de la request**
```json
{
  "title": "Fix login redirect bug",
  "description": "Happens when session expires",
  "priority": "high",
  "status": "todo",
  "estimatedHours": 2.0,
  "storyPoints": 3,
  "acceptanceCriteria": "Error message is shown within 2 seconds",
  "startDate": "2025-05-01T00:00:00Z",
  "dueDate": "2025-05-10T00:00:00Z"
}
```

| Campo | Tipo | Requerido | Default | Notas |
|---|---|---|---|---|
| title | string | sí | — | |
| description | string | no | null | |
| priority | string | no | `medium` | |
| status | string | no | `todo` | |
| estimatedHours | decimal | no | null | Máximo 4.0 según el bot de Telegram |
| actualHours | decimal | no | null | Se establece con el comando `/completetask` del bot |
| storyPoints | integer | no | null | |
| acceptanceCriteria | string | no | null | Definición de terminado |
| startDate | Instant | no | null | |
| dueDate | Instant | no | null | |

**Respuesta `201`** — objeto de tarea creado.

**Errores**
| Status | Razón |
|---|---|
| `400` | Falta el título |
| `404` | Proyecto no encontrado |

---

### `PATCH /api/projects/{projectId}/tasks/{taskId}`
Actualiza parcialmente una tarea. Solo se modifican los campos enviados.

**Cuerpo de la request** — misma forma que POST, todos los campos opcionales.

**Respuesta `200`** — objeto de tarea actualizado.

**Errores**
| Status | Razón |
|---|---|
| `404` | Tarea no encontrada |

---

### `DELETE /api/projects/{projectId}/tasks/{taskId}`
Elimina una tarea.

**Respuesta `204`** — sin cuerpo.

**Errores**
| Status | Razón |
|---|---|
| `404` | Tarea no encontrada |

---

## Asignaciones de Tarea

### `GET /api/projects/{projectId}/tasks/{taskId}/assignments`
Lista todos los usuarios asignados a una tarea.

**Respuesta `200`**
```json
[
  {
    "id": 1,
    "task": { "id": 3, "title": "Set up CI/CD pipeline" },
    "user": { "id": 2, "username": "alfredo" },
    "assignedAt": "2025-02-02T10:00:00Z"
  }
]
```

---

### `POST /api/projects/{projectId}/tasks/{taskId}/assignments`
Asigna un usuario a una tarea.

**Cuerpo de la request**
```json
{
  "userId": 2
}
```

**Respuesta `201`** — objeto de asignación (misma forma que arriba).

**Errores**
| Status | Razón |
|---|---|
| `404` | Tarea o usuario no encontrado |
| `409` | El usuario ya está asignado |

---

### `DELETE /api/projects/{projectId}/tasks/{taskId}/assignments/{userId}`
Elimina la asignación de un usuario de una tarea.

**Respuesta `204`** — sin cuerpo.

**Errores**
| Status | Razón |
|---|---|
| `404` | Asignación no encontrada |

---

## Sprints

Todos los endpoints de sprints están anidados bajo un proyecto: `/api/projects/{projectId}/sprints`.

### `GET /api/projects/{projectId}/sprints`
Lista todos los sprints de un proyecto.

**Respuesta `200`** — arreglo de objetos de sprint.

**Errores**
| Status | Razón |
|---|---|
| `404` | Proyecto no encontrado |

---

### `GET /api/projects/{projectId}/sprints/{sprintId}`
Obtiene un sprint por ID.

**Respuesta `200`** — objeto de sprint único.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |

---

### `POST /api/projects/{projectId}/sprints`
Crea un sprint dentro de un proyecto.

**Cuerpo de la request**
```json
{
  "name": "Sprint 7",
  "goal": "Close auth and reporting backlog",
  "status": "planned",
  "startDate": "2026-04-01T09:00:00",
  "dueDate": "2026-04-15T18:00:00"
}
```

| Campo | Tipo | Requerido | Default |
|---|---|---|---|
| name | string | sí | — |
| goal | string | no | null |
| status | string | no | `planned` |
| startDate | LocalDateTime | no | null |
| dueDate | LocalDateTime | no | null |
| actualEnd | LocalDateTime | no | null |

**Respuesta `201`** — objeto de sprint creado.

**Errores**
| Status | Razón |
|---|---|
| `400` | Falta el nombre |
| `404` | Proyecto no encontrado |

---

### `PATCH /api/projects/{projectId}/sprints/{sprintId}`
Actualiza parcialmente un sprint. Solo se modifican los campos enviados.

**Respuesta `200`** — objeto de sprint actualizado.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |

---

### `DELETE /api/projects/{projectId}/sprints/{sprintId}`
Elimina un sprint.

**Respuesta `204`** — sin cuerpo.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |

---

## Tareas de Sprint

Todos los endpoints de tareas-sprint están anidados bajo un sprint de proyecto:
`/api/projects/{projectId}/sprints/{sprintId}/tasks`.

Alias disponible: `/api/projects/{projectId}/sprints/{sprintId}/sprint_tasks`.

### `GET /api/projects/{projectId}/sprints/{sprintId}/tasks`
Lista todos los vínculos de tarea en un sprint.

**Respuesta `200`** — arreglo de objetos tarea-sprint.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |

---

### `GET /api/projects/{projectId}/sprints/{sprintId}/tasks/{sprintTaskId}`
Obtiene un vínculo tarea-sprint por ID.

**Respuesta `200`** — objeto tarea-sprint único.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |
| `404` | Tarea de sprint no encontrada |

---

### `POST /api/projects/{projectId}/sprints/{sprintId}/tasks`
Agrega una tarea a un sprint.

**Cuerpo de la request**
```json
{
  "taskId": 15
}
```

**Respuesta `201`** — objeto tarea-sprint creado.

**Errores**
| Status | Razón |
|---|---|
| `400` | Falta taskId |
| `404` | Sprint no encontrado |
| `404` | Tarea no encontrada |
| `409` | La tarea ya está activa en este sprint |

---

### `PATCH /api/projects/{projectId}/sprints/{sprintId}/tasks/{sprintTaskId}`
Actualiza parcialmente un vínculo tarea-sprint. Campos disponibles: `removedAt`, `movedToId`.

**Respuesta `200`** — objeto tarea-sprint actualizado.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |
| `404` | Tarea de sprint no encontrada |

---

### `DELETE /api/projects/{projectId}/sprints/{sprintId}/tasks/{sprintTaskId}`
Elimina un vínculo tarea-sprint.

**Respuesta `204`** — sin cuerpo.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |
| `404` | Tarea de sprint no encontrada |

---

## Asignaciones de Historia a Sprint

Los endpoints de asignación de historias a sprint están anidados bajo un sprint de proyecto:
`/api/projects/{projectId}/sprints/{sprintId}/user_stories`.

### `GET /api/projects/{projectId}/sprints/{sprintId}/user_stories`
Lista todos los vínculos historia-sprint en un sprint.

**Respuesta `200`** — arreglo de objetos asignación historia-sprint.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |

---

### `GET /api/projects/{projectId}/sprints/{sprintId}/user_stories/{sprintStoryAssignmentId}`
Obtiene un vínculo historia-sprint por ID.

**Respuesta `200`** — objeto asignación historia-sprint único.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |
| `404` | Asignación de historia a sprint no encontrada |

---

### `POST /api/projects/{projectId}/sprints/{sprintId}/user_stories`
Agrega una historia de usuario a un sprint.

**Cuerpo de la request**
```json
{
  "userStoryId": 15
}
```

**Respuesta `201`** — objeto asignación historia-sprint creado.

**Errores**
| Status | Razón |
|---|---|
| `400` | Falta userStoryId |
| `404` | Sprint no encontrado |
| `404` | Historia de usuario no encontrada |
| `409` | La historia de usuario ya está activa en este sprint |

---

### `PATCH /api/projects/{projectId}/sprints/{sprintId}/user_stories/{sprintStoryAssignmentId}`
Actualiza parcialmente un vínculo historia-sprint. Campos disponibles: `removedAt`, `movedToId`.

**Respuesta `200`** — objeto asignación historia-sprint actualizado.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |
| `404` | Asignación de historia a sprint no encontrada |

---

### `DELETE /api/projects/{projectId}/sprints/{sprintId}/user_stories/{sprintStoryAssignmentId}`
Elimina un vínculo historia-sprint.

**Respuesta `204`** — sin cuerpo.

**Errores**
| Status | Razón |
|---|---|
| `404` | Sprint no encontrado |
| `404` | Asignación de historia a sprint no encontrada |

---

## Miembros de Proyecto

### `GET /api/project-members`
Lista los miembros de proyectos. Se pueden filtrar con parámetros de query.

| Parámetro de query | Tipo | Descripción |
|---|---|---|
| projectId | number | Filtrar por proyecto |
| userId | number | Filtrar por usuario |

**Respuesta `200`**
```json
[
  {
    "id": 1,
    "projectId": 1,
    "projectName": "Andromeda Backend",
    "userId": 2,
    "username": "alfredo",
    "role": "manager",
    "joinedAt": "2025-01-16T09:00:00Z"
  }
]
```

---

### `GET /api/project-members/{id}`
Obtiene un registro de membresía por ID.

**Respuesta `200`** — objeto de miembro único (misma forma que arriba).

**Errores**
| Status | Razón |
|---|---|
| `404` | Registro de miembro no encontrado |

---

### `POST /api/project-members`
Agrega un usuario a un proyecto.

**Cuerpo de la request**
```json
{
  "projectId": 1,
  "userId": 3,
  "role": "member"
}
```

| Campo | Tipo | Requerido | Default |
|---|---|---|---|
| projectId | number | sí | — |
| userId | number | sí | — |
| role | string | no | `member` |

**Respuesta `201`** — objeto de miembro creado.

**Errores**
| Status | Razón |
|---|---|
| `404` | Proyecto o usuario no encontrado |
| `409` | El usuario ya es miembro de este proyecto |

---

### `PUT /api/project-members/{id}`
Actualiza un registro de membresía (cambiar rol o reasignar a diferente proyecto/usuario).

**Cuerpo de la request** — misma forma que POST, todos los campos opcionales.

**Respuesta `200`** — objeto de miembro actualizado.

**Errores**
| Status | Razón |
|---|---|
| `404` | Registro de miembro, proyecto o usuario no encontrado |
| `409` | La combinación proyecto/usuario ya existe |

---

### `DELETE /api/project-members/{id}`
Elimina un miembro de un proyecto.

**Respuesta `204`** — sin cuerpo.

**Errores**
| Status | Razón |
|---|---|
| `404` | Registro de miembro no encontrado |

---

## Logs

Alias legacy disponibles:
- `/logs` (igual que `/api/logs`)
- `/projects/{projectId}/logs` (igual que `/api/projects/{projectId}/logs`)

### `GET /api/logs`
Busca en el log de auditoría. Todos los parámetros de query son opcionales y combinables.

| Parámetro de query | Tipo | Descripción |
|---|---|---|
| projectId | number | Logs de un proyecto o sus tareas |
| taskId | number | Logs de una tarea específica |
| userId | number | Logs creados por un usuario específico |
| from | ISO datetime | Límite inferior en `logDate` |
| to | ISO datetime | Límite superior en `logDate` |

**Ejemplo**
```
GET /api/logs?projectId=1&from=2025-01-01T00:00:00&to=2025-12-31T23:59:59
```

**Respuesta `200`**
```json
[
  {
    "id": 10,
    "userId": 2,
    "entity": "task",
    "entityId": 3,
    "action": "update",
    "detail": "Status changed to in_progress",
    "logDate": "2025-02-05T14:30:00"
  }
]
```

Los resultados se ordenan por `logDate` descendente.

---

### `GET /api/projects/{projectId}/logs`
Atajo — devuelve todos los logs de un proyecto y sus tareas.  
Equivalente a `GET /api/logs?projectId={projectId}`.

**Respuesta `200`** — arreglo de objetos de log.

---

### `POST /api/logs`
Crea una entrada de log manualmente.

**Cuerpo de la request**
```json
{
  "userId": 2,
  "entity": "task",
  "entityId": 3,
  "action": "update",
  "detail": "Status changed to done",
  "logDate": "2025-04-14T10:00:00"
}
```

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| userId | number | no | FK a la tabla users |
| entity | string | no | ej. `project`, `task` |
| entityId | number | no | ID de la entidad registrada |
| action | string | no | ej. `create`, `update`, `delete` |
| detail | string | no | Descripción en texto libre |
| logDate | LocalDateTime | no | Por defecto la hora actual |

**Respuesta `201`** — objeto de log creado.

---

## Dashboard

### `GET /api/dashboard?projectId={projectId}`
Devuelve KPIs agregados de un proyecto en una sola respuesta.

**Respuesta `200`**
```json
{
  "projectId": 1,
  "generatedAt": "2026-05-11T09:00:00",
  "completionRateBySprint": [],
  "teamVelocity": [],
  "taskDistribution": [],
  "userTasksPerSprint": []
}
```

**Errores**
| Status | Razón |
|---|---|
| `400` | Falta el parámetro `projectId` |
| `500` | Error al agregar KPIs |

---

## Notificaciones de IA

### `POST /api/ai/notify`
Genera y envía una notificación de Telegram asistida por IA.

**Cuerpo de la request**
```json
{
  "chatId": "123456789",
  "context": "Build failed on main branch"
}
```

**Respuesta `200`**
```text
Notification sent.
```

**Errores**
| Status | Razón |
|---|---|
| `400` | Falta `chatId` o `context` |
| `500` | La IA no respondió |

---

### `GET /api/ai/status`
Devuelve el estado del backend de IA.

**Respuesta `200`**
```text
AI online | model: <model-name> | latency: <ms> ms
```

Respuesta alternativa cuando está deshabilitada:
```text
AI disabled (agent.ai.enabled=false).
```

Respuesta posible cuando no está disponible:
```text
AI backend unreachable.
```

---

## Respuestas de Error

Todas las respuestas de error siguen esta forma:

```json
{
  "error": "Mensaje legible"
}
```

| Status | Significado |
|---|---|
| `400` | Request inválida / fallo de validación |
| `401` | No autenticado |
| `404` | Recurso no encontrado |
| `409` | Conflicto (clave única duplicada) |
| `500` | Error interno del servidor |

---

## Valores de Enums

| Campo | Valores permitidos |
|---|---|
| Project `status` | `active` `paused` `completed` `cancelled` |
| Task `status` | `todo` `in_progress` `review` `done` |
| Task `priority` | `low` `medium` `high` `critical` |
| Sprint `status` | `planned` `active` `completed` |
| Member `role` | `owner` `manager` `member` |
| Task `estimatedHours` | Decimal positivo, máx. `4.0` (aplicado por el bot de Telegram, no por la API REST) |
| Task `actualHours` | Decimal positivo, registrado al completar |
| Task `storyPoints` | Entero positivo (valores Fibonacci comunes: 1, 2, 3, 5, 8, 13) |

---

## Endpoints del Product Backlog (V5)

### Capabilities
- `GET /api/projects/{projectId}/capabilities`
- `GET /api/projects/{projectId}/capabilities/{capabilityId}`
- `POST /api/projects/{projectId}/capabilities`
- `PATCH /api/projects/{projectId}/capabilities/{capabilityId}`
- `DELETE /api/projects/{projectId}/capabilities/{capabilityId}`

### Features
- `GET /api/projects/{projectId}/capabilities/{capabilityId}/features`
- `GET /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}`
- `POST /api/projects/{projectId}/capabilities/{capabilityId}/features`
- `PATCH /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}`
- `DELETE /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}`

### Historias de Usuario
- `GET /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories`
- `GET /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories/{storyId}`
- `POST /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories`
- `PATCH /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories/{storyId}`
- `DELETE /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories/{storyId}`

### Dependencias de Historias de Usuario
- `GET /api/projects/{projectId}/stories/{storyId}/dependencies`
- `GET /api/projects/{projectId}/stories/{storyId}/dependencies/{dependencyId}`
- `POST /api/projects/{projectId}/stories/{storyId}/dependencies`
- `PATCH /api/projects/{projectId}/stories/{storyId}/dependencies/{dependencyId}`
- `DELETE /api/projects/{projectId}/stories/{storyId}/dependencies/{dependencyId}`

### Retrospectiva de Sprint
- `GET /api/projects/{projectId}/sprints/{sprintId}/retrospective`
- `POST /api/projects/{projectId}/sprints/{sprintId}/retrospective`
- `PATCH /api/projects/{projectId}/sprints/{sprintId}/retrospective`
- `DELETE /api/projects/{projectId}/sprints/{sprintId}/retrospective`

### Story Spillovers
- `GET /api/projects/{projectId}/story-spillovers`
- `GET /api/projects/{projectId}/story-spillovers/{spilloverId}`
- `POST /api/projects/{projectId}/story-spillovers`
- `PATCH /api/projects/{projectId}/story-spillovers/{spilloverId}`
- `DELETE /api/projects/{projectId}/story-spillovers/{spilloverId}`

### Deuda Técnica
- `GET /api/projects/{projectId}/technical-debt`
- `GET /api/projects/{projectId}/technical-debt/{debtId}`
- `POST /api/projects/{projectId}/technical-debt`
- `PATCH /api/projects/{projectId}/technical-debt/{debtId}`
- `DELETE /api/projects/{projectId}/technical-debt/{debtId}`

### Work Items del Proyecto (vista agregada)
- `GET /api/projects/{projectId}/work-items`

Devuelve un payload JSON con:
- resumen del proyecto
- jerarquía capabilities → features → historias de usuario → tareas
- sprints con los IDs de historias activas
