# Andromeda API — Endpoint Reference

Base URL (local): `http://localhost:8080`

All request and response bodies use `application/json`.  
Dates use ISO-8601 format (`2025-06-15T00:00:00Z` for `Instant`, `2025-06-15T10:30:00` for `LocalDateTime`).

---

## Table of Contents

- [Root & Health](#root--health)
- [Auth](#auth)
- [Users](#users)
- [Projects](#projects)
- [Tasks](#tasks)
- [Task Assignments](#task-assignments)
- [Project Members](#project-members)
- [Logs](#logs)
- [Error Responses](#error-responses)
- [Enum Values](#enum-values)

---

## Root & Health

### `GET /`
Returns API metadata and a list of available endpoint groups.

**Response `200`**
```json
{
  "service": "Andromeda Backend API",
  "version": "0.0.1",
  "endpoints": [...]
}
```

---

### `GET /health`
Liveness check.

**Response `200`**
```json
{
  "status": "UP"
}
```

---

## Auth

### `POST /api/auth/register`
Register a new user account.

**Request body**
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

| Field | Type | Required | Notes |
|---|---|---|---|
| name | string | yes | |
| username | string | yes | Must be unique |
| password | string | yes | Stored as BCrypt hash |
| email | string | yes | Must be unique |
| phone | string | no | |
| userTypeId | number | yes | FK to user_type table |

**Response `201`**
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

**Errors**
| Status | Reason |
|---|---|
| `409` | Username or email already taken |
| `400` | Missing required field |

---

### `POST /api/auth/login`
Authenticate an existing user.

**Request body**
```json
{
  "username": "santiago",
  "password": "secret123"
}
```

**Response `200`** — same shape as register response.

**Errors**
| Status | Reason |
|---|---|
| `401` | Invalid credentials |
| `404` | User not found |

---

## Users

### `GET /api/users`
List all users.

**Response `200`**
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

---

### `GET /api/users/{id}`
Get one user by ID.

**Response `200`** — single user object (same shape as above).

**Errors**
| Status | Reason |
|---|---|
| `404` | User not found |

---

### `PUT /api/users/{id}`
Update a user. All fields are optional; only supplied fields are changed.

**Request body**
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

**Response `200`** — updated user object.

**Errors**
| Status | Reason |
|---|---|
| `404` | User not found |
| `409` | Username or email already taken |

---

### `DELETE /api/users/{id}`
Delete a user.

**Response `204`** — no body.

**Errors**
| Status | Reason |
|---|---|
| `404` | User not found |

---

## Projects

### `GET /api/projects`
List all projects.

**Response `200`**
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
Get one project by ID.

**Response `200`** — single project object (same shape as above).

**Errors**
| Status | Reason |
|---|---|
| `404` | Project not found |

---

### `POST /api/projects`
Create a new project.

**Request body**
```json
{
  "name": "New Project",
  "description": "Optional description",
  "status": "active",
  "startDate": "2025-05-01T00:00:00Z",
  "endDate": "2025-11-01T00:00:00Z"
}
```

| Field | Type | Required | Default |
|---|---|---|---|
| name | string | yes | — |
| description | string | no | null |
| status | string | no | `active` |
| startDate | Instant | no | null |
| endDate | Instant | no | null |

**Response `201`** — created project object.

---

### `PATCH /api/projects/{id}`
Partially update a project. Only supplied fields are changed.

**Request body** — same shape as POST, all fields optional.

**Response `200`** — updated project object.

**Errors**
| Status | Reason |
|---|---|
| `404` | Project not found |

---

### `DELETE /api/projects/{id}`
Delete a project.

**Response `204`** — no body.

**Errors**
| Status | Reason |
|---|---|
| `404` | Project not found |

---

## Tasks

All task endpoints are nested under a project: `/api/projects/{projectId}/tasks`.

### `GET /api/projects/{projectId}/tasks`
List all tasks in a project.

**Response `200`**
```json
[
  {
    "id": 3,
    "title": "Set up CI/CD pipeline",
    "description": null,
    "priority": "high",
    "status": "in_progress",
    "startDate": "2025-02-01T00:00:00Z",
    "dueDate": "2025-03-15T00:00:00Z",
    "actualEnd": null,
    "createdAt": "2025-01-20T08:00:00Z"
  }
]
```

---

### `GET /api/projects/{projectId}/tasks/{taskId}`
Get one task by ID.

**Response `200`** — single task object (same shape as above).

**Errors**
| Status | Reason |
|---|---|
| `404` | Task not found |

---

### `POST /api/projects/{projectId}/tasks`
Create a task inside a project.

**Request body**
```json
{
  "title": "Fix login redirect bug",
  "description": "Happens when session expires",
  "priority": "high",
  "status": "todo",
  "startDate": "2025-05-01T00:00:00Z",
  "dueDate": "2025-05-10T00:00:00Z"
}
```

| Field | Type | Required | Default |
|---|---|---|---|
| title | string | yes | — |
| description | string | no | null |
| priority | string | no | `medium` |
| status | string | no | `todo` |
| startDate | Instant | no | null |
| dueDate | Instant | no | null |

**Response `201`** — created task object.

**Errors**
| Status | Reason |
|---|---|
| `400` | Missing title |
| `404` | Project not found |

---

### `PATCH /api/projects/{projectId}/tasks/{taskId}`
Partially update a task. Only supplied fields are changed.

**Request body** — same shape as POST, all fields optional.

**Response `200`** — updated task object.

**Errors**
| Status | Reason |
|---|---|
| `404` | Task not found |

---

### `DELETE /api/projects/{projectId}/tasks/{taskId}`
Delete a task.

**Response `204`** — no body.

**Errors**
| Status | Reason |
|---|---|
| `404` | Task not found |

---

## Task Assignments

### `GET /api/projects/{projectId}/tasks/{taskId}/assignments`
List all users assigned to a task.

**Response `200`**
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
Assign a user to a task.

**Request body**
```json
{
  "userId": 2
}
```

**Response `201`** — assignment object (same shape as above).

**Errors**
| Status | Reason |
|---|---|
| `404` | Task or user not found |
| `409` | User already assigned |

---

### `DELETE /api/projects/{projectId}/tasks/{taskId}/assignments/{userId}`
Remove a user's assignment from a task.

**Response `204`** — no body.

**Errors**
| Status | Reason |
|---|---|
| `404` | Assignment not found |

---

## Project Members

### `GET /api/project-members`
List project members. Results can be filtered with query parameters.

| Query param | Type | Description |
|---|---|---|
| projectId | number | Filter by project |
| userId | number | Filter by user |

**Response `200`**
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
Get one membership record by ID.

**Response `200`** — single member object (same shape as above).

**Errors**
| Status | Reason |
|---|---|
| `404` | Member record not found |

---

### `POST /api/project-members`
Add a user to a project.

**Request body**
```json
{
  "projectId": 1,
  "userId": 3,
  "role": "member"
}
```

| Field | Type | Required | Default |
|---|---|---|---|
| projectId | number | yes | — |
| userId | number | yes | — |
| role | string | no | `member` |

**Response `201`** — created member object.

**Errors**
| Status | Reason |
|---|---|
| `404` | Project or user not found |
| `409` | User is already a member of this project |

---

### `PUT /api/project-members/{id}`
Update a membership record (change role or re-assign to a different project/user).

**Request body** — same shape as POST, all fields optional.

**Response `200`** — updated member object.

**Errors**
| Status | Reason |
|---|---|
| `404` | Member record, project, or user not found |
| `409` | Target project/user combination already exists |

---

### `DELETE /api/project-members/{id}`
Remove a member from a project.

**Response `204`** — no body.

**Errors**
| Status | Reason |
|---|---|
| `404` | Member record not found |

---

## Logs

### `GET /api/logs`
Search the audit log. All query parameters are optional and combinable.

| Query param | Type | Description |
|---|---|---|
| projectId | number | Logs for a project or its tasks |
| taskId | number | Logs for a specific task |
| userId | number | Logs created by a specific user |
| from | ISO datetime | Lower bound on `logDate` |
| to | ISO datetime | Upper bound on `logDate` |

**Example**
```
GET /api/logs?projectId=1&from=2025-01-01T00:00:00&to=2025-12-31T23:59:59
```

**Response `200`**
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

Results are ordered by `logDate` descending.

---

### `GET /api/projects/{projectId}/logs`
Shorthand — returns all logs for a project and its tasks.  
Equivalent to `GET /api/logs?projectId={projectId}`.

**Response `200`** — array of log objects.

---

### `POST /api/logs`
Create a log entry manually.

**Request body**
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

| Field | Type | Required | Notes |
|---|---|---|---|
| userId | number | no | FK to users table |
| entity | string | no | e.g. `project`, `task` |
| entityId | number | no | ID of the logged entity |
| action | string | no | e.g. `create`, `update`, `delete` |
| detail | string | no | Free-text description |
| logDate | LocalDateTime | no | Defaults to current time |

**Response `201`** — created log object.

---

## Error Responses

All error responses follow this shape:

```json
{
  "error": "Human-readable message"
}
```

| Status | Meaning |
|---|---|
| `400` | Bad request / validation failure |
| `401` | Unauthenticated |
| `404` | Resource not found |
| `409` | Conflict (duplicate unique key) |
| `500` | Internal server error |

---

## Enum Values

| Field | Allowed values |
|---|---|
| Project `status` | `active` `paused` `completed` `cancelled` |
| Task `status` | `todo` `in_progress` `review` `done` |
| Task `priority` | `low` `medium` `high` `critical` |
| Sprint `status` | `planned` `active` `completed` |
| Member `role` | `owner` `manager` `member` |
