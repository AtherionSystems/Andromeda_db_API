# DTO Response Refactoring

Refactorización de los endpoints GET que devolvían entidades JPA directamente. Aplica a Tasks, User Stories, Sprints y Sprint Story Assignments.

---

## El problema original

### N+1 queries por lazy loading

Las entidades JPA tienen relaciones `FetchType.LAZY`. Cuando Spring devuelve una entidad directamente y Jackson la serializa a JSON, Hibernate dispara una query SQL **por cada objeto** para cargar cada relación.

```
// Con 20 tasks en el proyecto:
SELECT * FROM tasks WHERE project_id = ?              -- 1 query
SELECT * FROM projects WHERE id = ?                    -- ×20 queries (una por task)
                                                       -- = 21 queries totales
```

Con entidades que tienen 4 relaciones lazy (como `UserStory`), el problema se multiplica:

```
SELECT * FROM user_stories WHERE feature_id = ?       -- 1 query
SELECT * FROM features WHERE id = ?                    -- ×N
SELECT * FROM users WHERE id = ?  (owner)              -- ×N
SELECT * FROM users WHERE id = ?  (created_by)         -- ×N
SELECT * FROM users WHERE id = ?  (updated_by)         -- ×N
                                                       -- = 4N+1 queries totales
```

### ORA-22848 con DISTINCT y columnas CLOB

Oracle no permite usar columnas de tipo `CLOB` como clave de comparación. El query original para el filtro `?assignedTo` usaba `SELECT DISTINCT` sobre la entidad `Tasks`, que incluye el campo `description` anotado con `@Lob`:

```
ORA-22848: no se puede utilizar el tipo CLOB como clave de comparación
[select distinct t1_0.id, ..., t1_0.description, ... from tasks t1_0, task_assignments ta1_0 ...]
```

### Over-fetching

Las respuestas incluían campos que el cliente no necesita: `passwordHash` (dentro de objetos `User` anidados), `createdBy`/`updatedBy` como IDs crudos sin nombre, el objeto `Project` completo cuando solo se necesitaba su nombre, y campos de auditoría internos.

---

## La solución: DTOs con JPQL constructor expressions

En lugar de cargar entidades y dejar que Hibernate haga lazy loads, las queries traen exactamente los campos necesarios en una sola SQL.

### Patrón 1 — Constructor expression para un solo nivel de relación

```java
// Repository
@Query("""
    SELECT new com.atherion.andromeda.dto.SprintResponse(
        s.id, s.name, s.goal, s.status, s.startDate, s.dueDate, s.actualEnd,
        s.createdAt, s.updatedAt, p.id, p.name
    )
    FROM Sprint s JOIN s.project p
    WHERE p.id = :projectId
    """)
List<SprintResponse> findByProjectIdAsResponse(@Param("projectId") Long projectId);
```

Genera una sola SQL con un JOIN. Sin lazy loads.

### Patrón 2 — JOIN FETCH + mapping estático para mutaciones

Cuando la entidad ya está en memoria (por ejemplo, después de un `save()`), se puede mapear sin query adicional:

```java
// DTO con factory method estático
public static SprintResponse from(Sprint s) {
    return new SprintResponse(
        s.getId(), s.getName(), ..., s.getProject().getId(), s.getProject().getName()
    );
}

// Controller después del save
return ResponseEntity.ok(SprintResponse.from(sprintService.save(sprint)));
```

### Patrón 3 — Dos queries para colecciones anidadas (evita ORA-22848)

Cuando una entidad tiene campos CLOB y además necesita traer una colección de elementos relacionados, no se puede hacer en un solo JOIN (el CLOB + DISTINCT causaría ORA-22848). La solución es separar en dos queries y fusionar en Java:

```
Query 1: trae la lista principal (sin colección)
Query 2: trae los items de la colección con WHERE id IN (ids de query 1)
         → usa DISTINCT solo sobre columnas sin CLOB
Java:    agrupa por id y une los resultados
```

### Patrón 4 — Constructor secundario en records para JPQL

El JPQL constructor expression no puede pasar un `List<>` como argumento. La solución es un constructor secundario que delega al canónico con `List.of()`:

```java
public record UserStoryResponse(..., List<AssignedUserSummary> assignedUsers) {

    // Usado por JPQL (sin lista)
    public UserStoryResponse(Long id, String title, ..., LocalDateTime updatedAt) {
        this(id, title, ..., updatedAt, List.of());
    }

    // Usado por el service para enriquecer
    public UserStoryResponse withAssignedUsers(List<AssignedUserSummary> assignedUsers) {
        return new UserStoryResponse(id, title, ..., updatedAt, assignedUsers);
    }
}
```

---

## Cambios por endpoint

---

### Tasks — `GET /api/projects/{id}/tasks`

**Antes**

```json
{
  "id": 42,
  "title": "Diseñar login",
  "description": "...",
  "priority": "high",
  "status": "in_progress",
  "project": {
    "id": 1,
    "name": "Andromeda",
    "description": "...",
    "status": "active",
    "startDate": "...",
    "endDate": null,
    "createdAt": "...",
    "createdBy": 1,
    "updatedBy": null,
    "updatedAt": null
  },
  "startDate": "...",
  "dueDate": "...",
  "actualEnd": null,
  "estimatedHours": 4.0,
  "actualHours": 2.5,
  "createdAt": "...",
  "createdBy": 1,
  "updatedBy": null,
  "updatedAt": null,
  "userStoryId": 7
}
```

Queries generadas: `1 + N` (una por tarea para cargar `project`).

El filtro `?assignedTo={userId}` lanzaba `ORA-22848` por el `SELECT DISTINCT` sobre la columna CLOB `description`.

**Después**

```json
{
  "id": 42,
  "title": "Diseñar login",
  "description": "...",
  "priority": "high",
  "status": "in_progress",
  "startDate": "...",
  "dueDate": "...",
  "actualEnd": null,
  "estimatedHours": 4.0,
  "actualHours": 2.5,
  "userStoryId": 7,
  "projectName": "Andromeda",
  "assignedUserName": "Alfredo Luce"
}
```

Queries generadas: **1 query** para todos los filtros.

El filtro `?assignedTo` usa `EXISTS` (sin DISTINCT) o constructor expression con JOIN directo — ORA-22848 eliminado.

**Archivos modificados**

| Archivo | Cambio |
|---------|--------|
| `dto/TaskResponse.java` | Nuevo DTO |
| `repositories/TasksRepository.java` | +3 queries con `JOIN FETCH`, +1 constructor expression para `assignedTo` |
| `services/TasksService.java` | +4 métodos `*AsResponse()` |
| `controllers/TasksController.java` | GET devuelve `List<TaskResponse>` |

---

### User Stories — `GET /api/projects/{id}/stories` y `GET .../features/{id}/stories/{id}`

**Antes**

```json
{
  "id": 5,
  "title": "Login de usuario",
  "description": "Como usuario quiero...",
  "acceptanceCriteria": "Dado que...",
  "priority": "high",
  "status": "in_progress",
  "storyPoints": 3,
  "feature": {
    "id": 2,
    "name": "Autenticación",
    "description": "...",
    "status": "active",
    "capability": { ... },
    "createdBy": { "id": 1, "name": "...", "passwordHash": "...", ... },
    "createdAt": "..."
  },
  "owner": { "id": 1, "name": "Alfredo Luce", "passwordHash": "...", "email": "...", ... },
  "createdBy": { "id": 1, "passwordHash": "...", ... },
  "updatedBy": null,
  "createdAt": "...",
  "updatedAt": null
}
```

Queries generadas: `4N+1`. Exponía `passwordHash` a través de los objetos `User` anidados.

**Después**

```json
{
  "id": 5,
  "title": "Login de usuario",
  "description": "Como usuario quiero...",
  "acceptanceCriteria": "Dado que...",
  "priority": "high",
  "status": "in_progress",
  "storyPoints": 3,
  "featureId": 2,
  "featureName": "Autenticación",
  "ownerName": "Alfredo Luce",
  "createdByName": "Alfredo Luce",
  "updatedByName": null,
  "createdAt": "...",
  "updatedAt": null,
  "assignedUsers": [
    { "userId": 1, "userName": "Alfredo Luce" },
    { "userId": 3, "userName": "Ana García" }
  ]
}
```

Queries generadas: **2 queries** fijas.
- Query 1: historia con JOINs a `feature`, `owner`, `createdBy`, `updatedBy`.
- Query 2: `TaskAssignment → task.userStoryId → user` con `WHERE userStoryId IN (...)` para los assignees. Separada de query 1 para evitar ORA-22848 (las historias tienen 2 CLOBs).

**Archivos modificados**

| Archivo | Cambio |
|---------|--------|
| `dto/UserStoryResponse.java` | Nuevo DTO con constructor secundario |
| `dto/AssignedUserSummary.java` | Nuevo record embebido |
| `dto/StoryAssigneeRow.java` | Record interno para query de assignees |
| `repositories/UserStoryRepository.java` | +3 queries JPQL con constructor expression, +1 query `findAssigneesByStoryIds` |
| `services/UserStoryService.java` | +3 métodos `*AsResponse()`, +método privado `enrichWithAssignees()` |
| `controllers/UserStoriesController.java` | GETs devuelven `UserStoryResponse` |
| `controllers/ProjectController.java` | `GET /{id}/stories` devuelve `List<UserStoryResponse>` |

---

### Sprints — `GET /api/projects/{id}/sprints` y `GET .../sprints/{id}`

**Antes**

```json
{
  "id": 3,
  "name": "Sprint 1",
  "goal": "Completar autenticación",
  "status": "active",
  "startDate": "...",
  "dueDate": "...",
  "actualEnd": null,
  "project": {
    "id": 1,
    "name": "Andromeda",
    "description": "...",
    "status": "active",
    "startDate": "...",
    "endDate": null,
    "createdAt": "...",
    "createdBy": 1,
    "updatedBy": null,
    "updatedAt": null
  },
  "createdAt": "...",
  "createdBy": 1,
  "updatedBy": null,
  "updatedAt": null
}
```

Queries generadas: `1 + N`.

**Después**

```json
{
  "id": 3,
  "name": "Sprint 1",
  "goal": "Completar autenticación",
  "status": "active",
  "startDate": "...",
  "dueDate": "...",
  "actualEnd": null,
  "createdAt": "...",
  "updatedAt": null,
  "projectId": 1,
  "projectName": "Andromeda"
}
```

Queries generadas: **1 query**.

POST y PATCH también devuelven `SprintResponse` usando `SprintResponse.from(saved)` — el proyecto ya está en memoria, sin query extra.

**Archivos modificados**

| Archivo | Cambio |
|---------|--------|
| `dto/SprintResponse.java` | Nuevo DTO con factory `from(Sprint)` |
| `repositories/SprintRepository.java` | +2 queries JPQL con constructor expression |
| `services/SprintService.java` | +2 métodos `*AsResponse()` |
| `controllers/SprintsController.java` | Todos los endpoints devuelven `SprintResponse` |

---

### Sprint Story Assignments — `GET .../sprints/{id}/tasks`

**Antes**

```json
{
  "id": 7,
  "sprint": {
    "id": 3,
    "name": "Sprint 1",
    "project": { ... objeto completo ... },
    "createdBy": 1,
    ...
  },
  "userStoryId": 12,
  "addedAt": "...",
  "removedAt": null,
  "isActive": 1,
  "movedTo": null
}
```

Queries generadas: `1 + 2N` (lazy load de `sprint` + lazy load del `project` dentro del sprint, por cada item).

Sin información de la user story ni de las tasks del sprint.

**Después**

```json
{
  "id": 7,
  "sprintId": 3,
  "sprintName": "Sprint 1",
  "userStoryId": 12,
  "addedAt": "...",
  "removedAt": null,
  "isActive": 1,
  "movedToSprintId": null,
  "movedToSprintName": null,
  "userStory": {
    "id": 12,
    "title": "Login de usuario",
    "priority": "high",
    "status": "in_progress",
    "storyPoints": 3,
    "featureName": "Autenticación",
    "ownerName": "Alfredo Luce"
  },
  "tasks": [
    {
      "id": 42,
      "title": "Diseñar pantalla de login",
      "priority": "high",
      "status": "in_progress",
      "dueDate": "...",
      "estimatedHours": 4.0,
      "actualHours": 2.5,
      "assignees": [
        { "userId": 1, "userName": "Alfredo Luce" }
      ]
    }
  ]
}
```

Queries generadas: **3 queries** fijas.
- Query 1: assignments con `JOIN sprint LEFT JOIN movedTo`.
- Query 2: tasks con `LEFT JOIN TaskAssignment ON ta.task = t LEFT JOIN ta.user u` filtradas por `userStoryId IN (...)`. El `LEFT JOIN` es crítico: un `INNER JOIN` excluiría tasks sin asignar.
- Query 3: user story summaries filtradas por `id IN (...)`.

El agrupamiento tasks→assignees se hace en Java con `SprintTaskSummaryBuilder` (clase interna privada en `SprintStoryAssignmentService`).

**Archivos modificados**

| Archivo | Cambio |
|---------|--------|
| `dto/SprintStoryAssignmentResponse.java` | Nuevo DTO con `userStory` y `tasks`, constructor secundario, `withDetails()` |
| `dto/SprintTaskSummary.java` | Nuevo record embebido |
| `dto/SprintTaskAssigneeRow.java` | Record interno para query plana tasks+assignees |
| `dto/UserStorySummary.java` | Nuevo record embebido |
| `repositories/SprintStoryAssignmentRepository.java` | +2 queries JPQL con constructor expression |
| `repositories/TasksRepository.java` | +query `findTasksWithAssigneesByStoryIds` |
| `repositories/UserStoryRepository.java` | +query `findSummariesByIds` |
| `services/SprintStoryAssignmentService.java` | +2 métodos `*AsResponse()`, +método privado `enrichWithDetails()`, +clase interna `SprintTaskSummaryBuilder` |
| `controllers/SprintTasksController.java` | Todos los endpoints devuelven `SprintStoryAssignmentResponse` |

---

## Resumen de queries generadas

| Endpoint | Antes | Después |
|----------|-------|---------|
| `GET /tasks` (cualquier filtro) | `1 + N` | **1** |
| `GET /tasks?assignedTo=` | Error ORA-22848 | **1** |
| `GET /stories` | `4N + 1` | **2** |
| `GET /stories/{id}` | `4 + lazy` | **2** |
| `GET /sprints` | `1 + N` | **1** |
| `GET /sprints/{id}/tasks` | `2N + 1` | **3** |

---

## DTOs introducidos

| DTO | Descripción |
|-----|-------------|
| `TaskResponse` | Respuesta para todos los GETs de tasks |
| `UserStoryResponse` | Respuesta para GETs de user stories (incluye `assignedUsers`) |
| `UserStorySummary` | Versión ligera de user story, embebida en sprint assignments |
| `AssignedUserSummary` | Par `(userId, userName)`, reutilizado en varios DTOs |
| `StoryAssigneeRow` | Record interno `(storyId, userId, userName)` para la query de assignees de stories |
| `SprintResponse` | Respuesta para GETs de sprints |
| `SprintStoryAssignmentResponse` | Respuesta para GETs de sprint assignments (incluye `userStory` y `tasks`) |
| `SprintTaskSummary` | Task resumida con assignees, embebida en sprint assignments |
| `SprintTaskAssigneeRow` | Record interno plano `(userStoryId, taskId, ..., assigneeId, assigneeName)` para la query de tasks de sprint |

---

## Reglas para nuevos endpoints

1. **Nunca devolver una entidad JPA con relaciones `LAZY` directamente desde un controller GET.**
2. **Un solo nivel de relación** (ej. Sprint→Project): usar constructor expression JPQL o `JOIN FETCH`.
3. **Colección anidada con CLOB en la entidad padre** (ej. UserStory→assignedUsers): segunda query con `WHERE id IN (...)` + merge en Java. No usar JOIN porque los CLOBs impiden DISTINCT en Oracle (ORA-22848).
4. **Múltiples niveles de colecciones** (ej. SprintAssignment→UserStory + Tasks→Assignees): una query por nivel, merge en Java. 3 queries es el máximo razonable.
5. **Campos `passwordHash`, `createdBy`/`updatedBy` como Long crudo**: nunca exponer en respuestas. Sustituir por nombre (`createdByName`) o eliminar si no aporta valor al cliente.
