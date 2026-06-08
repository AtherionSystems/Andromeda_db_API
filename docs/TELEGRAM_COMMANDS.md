# Andromeda Bot — Referencia de Comandos de Telegram

El bot responde únicamente a mensajes que comienzan con `/`. Los comandos desconocidos se ignoran silenciosamente.  
Los comandos funcionan en chats privados y en grupos (usa el sufijo `@BotUsername` en grupos, ej. `/projects@AndromedaBot`).

---

## Tabla de Contenidos

- [Info y Health](#info-y-health)
- [Vinculación de Cuenta](#vinculación-de-cuenta)
- [Lectura — Proyectos](#lectura--proyectos)
- [Lectura — Tareas](#lectura--tareas)
- [Lectura — Miembros](#lectura--miembros)
- [Lectura — Sprints](#lectura--sprints)
- [Lectura — Tablero de Sprint](#lectura--tablero-de-sprint)
- [Lectura — Usuarios](#lectura--usuarios)
- [Escritura — Crear Proyecto](#escritura--crear-proyecto)
- [Escritura — Crear Sprint](#escritura--crear-sprint)
- [Escritura — Crear Tarea](#escritura--crear-tarea)
- [Escritura — Asignar Tarea a Sprint](#escritura--asignar-tarea-a-sprint)
- [Escritura — Completar Tarea](#escritura--completar-tarea)
- [Escritura — Actualizar Estado de Tarea](#escritura--actualizar-estado-de-tarea)
- [Escritura — Actualizar Prioridad de Tarea](#escritura--actualizar-prioridad-de-tarea)
- [Escritura — Actualizar Estado de Proyecto](#escritura--actualizar-estado-de-proyecto)
- [Escritura — Agregar Miembro](#escritura--agregar-miembro)
- [Valores Permitidos](#valores-permitidos)

---

## Info y Health

### `/ping`
Verifica que el bot esté disponible.

**Ejemplo**
```
/ping
```
**Respuesta**
```
Pong! Andromeda API is up and running.
```

---

### `/health`
Verifica el estado general de la API y el bot.

**Ejemplo**
```
/health
```
**Respuesta**
```
Status: OK
Service: Andromeda Backend API
Bot: Connected
```

---

### `/help`
Lista todos los comandos disponibles con sugerencias de sintaxis.

**Ejemplo**
```
/help
```
**Respuesta**
```
Andromeda Bot — Commands
════════════════════════

SETUP
/link <username> <password>   Link your account

READ
/projects                     List all projects
/project <id>                 Project details
/tasks <projectId>            Tasks in a project
/task <id>                    Task details
/members <projectId>          Project members
/sprints <projectId>          Project sprints
/sprinttasks <projectId>      Sprint board (last 2 sprints)
/users                        List all users
/user <id>                    User details

WRITE  (requires /link)
/newproject <name> [| desc] [| status]
/newsprint <projectId> | <name> [| goal] [| status] [| startDate] [| dueDate]
/newtask <projectId> | <title> | <estimatedHours> | <storyPoints> [| priority] [| acceptanceCriteria]
/assigntask <sprintId> <taskId>
/addsprinttask <sprintId> <taskId>
/completetask <taskId> <actualHours>
/taskstatus <taskId> <status>
/taskpriority <taskId> <priority>
/projectstatus <projectId> <status>
/addmember <projectId> <userId> [role]

VALUES
Project status : active · paused · completed · cancelled
Sprint status  : planned · active · completed
Task status    : todo · in_progress · review · done
Task priority  : low · medium · high · critical
Member role    : owner · manager · member
Max est. hours : 4.0 h per task

OTHER
/health                API health check
/ping                  Ping the bot
```

---

## Vinculación de Cuenta

Todos los comandos de escritura requieren que tu cuenta de Telegram esté vinculada a un usuario del sistema. Los comandos de lectura funcionan sin vinculación.

### `/link <username> <password>`
Autentícate con tus credenciales del sistema. El bot almacena tu ID de usuario de Telegram para que los comandos de escritura posteriores sepan quién eres.

**Ejemplo**
```
/link santiago secret123
```
**Respuesta**
```
Linked! Welcome, Santiago Quiroz (@santiago).
You can now use all write commands.
```

**Credenciales incorrectas**
```
Invalid username or password.
```

**Ya vinculado**
```
Already linked to @santiago. Welcome back, Santiago Quiroz!
```

---

## Lectura — Proyectos

### `/projects`
Lista todos los proyectos con su estado actual.

**Ejemplo**
```
/projects
```
**Respuesta**
```
Projects (3)
───────────────────────
[1] Andromeda Backend — active
[2] Mobile App — paused
[3] Legacy Migration — completed
```

---

### `/project <id>`
Detalle completo de un proyecto, incluyendo conteos de miembros y tareas.

**Ejemplo**
```
/project 1
```
**Respuesta**
```
Project #1
Name:    Andromeda Backend
Status:  active
Start:   2025-01-15
End:     2025-12-31
Members: 4
Tasks:   12
```

**No encontrado**
```
Project #99 not found.
```

---

## Lectura — Tareas

### `/tasks <projectId>`
Lista todas las tareas que pertenecen a un proyecto.

**Ejemplo**
```
/tasks 1
```
**Respuesta**
```
Tasks for project #1 (4)
───────────────────────
[3] Set up CI/CD pipeline — high | in_progress | 5 pts | 3.0 h
[4] Write unit tests — medium | todo | 2 pts | 1.5 h
[5] Deploy to staging — high | todo | 5 pts | 4.0 h
[6] Update API docs — low | done | 1 pts | 1.0 h
```

---

### `/task <id>`
Detalle completo de una tarea.

**Ejemplo**
```
/task 3
```
**Respuesta**
```
Task #3
Title:       Set up CI/CD pipeline
Project:     #1 Andromeda Backend
Priority:    high
Status:      in_progress
Story pts:   5
Est. hours:  3.0
Act. hours:  —
Start:       2025-02-01
Due:         2025-03-15
```

---

## Lectura — Miembros

### `/members <projectId>`
Lista todos los miembros de un proyecto con sus roles.

**Ejemplo**
```
/members 1
```
**Respuesta**
```
Members of project #1 (3)
───────────────────────
@javier — owner
@alfredo — manager
@santiago — member
```

---

## Lectura — Sprints

### `/sprints <projectId>`
Lista todos los sprints de un proyecto con estado y rango de fechas.

**Ejemplo**
```
/sprints 1
```
**Respuesta**
```
Sprints for project #1 (2)
───────────────────────
[1] Sprint 1 — completed | 2025-01-15 → 2025-01-29
[2] Sprint 2 — active | 2025-01-29 → 2025-02-12
```

---

## Lectura — Tablero de Sprint

### `/sprinttasks <projectId>`
Muestra el tablero de tareas de los **últimos 2 sprints** de un proyecto. Las tareas se agrupan por sprint y se ordenan por estado (in_progress → review → todo → done) y luego por prioridad. Los responsables se obtienen mediante JOIN de las asignaciones de tareas.

**Ejemplo**
```
/sprinttasks 1
```
**Respuesta**
```
Sprint Board — Project #1
════════════════════════════════

▸ Sprint 2
────────────────────────────────
[3] Set up CI/CD pipeline
    IN_PROG | high | 5 pts | 3.0h est | @javier, @alfredo

[4] Write unit tests
    TODO    | medium | 2 pts | 1.5h est | —

▸ Sprint 1
────────────────────────────────
[1] Initial DB schema
    DONE    | high | 8 pts | 4.0h est / 3.5h act | @javier
```

**Sin tareas encontradas**
```
No tasks found in recent sprints for project #1.
```

---

## Lectura — Usuarios

### `/users`
Lista todos los usuarios registrados.

**Ejemplo**
```
/users
```
**Respuesta**
```
Users (3)
───────────────────────
[1] @javier — Javier García
[2] @alfredo — Alfredo López
[3] @santiago — Santiago Quiroz
```

---

### `/user <id>`
Detalle de un usuario.

**Ejemplo**
```
/user 2
```
**Respuesta**
```
User #2
Name:     Alfredo López
Username: @alfredo
Email:    alfredo@example.com
Phone:    +521234567890
```

---

## Escritura — Crear Proyecto

### `/newproject <name> [| description] [| status]`

Crea un nuevo proyecto. La descripción y el estado son opcionales; el estado por defecto es `active`.  
Usa ` | ` (pipe con espacios) para separar los campos — esto permite usar espacios en el nombre y la descripción.

**Solo nombre**
```
/newproject Andromeda v2
```
**Respuesta**
```
Project created!
ID:     4
Name:   Andromeda v2
Status: active
```

---

**Con descripción**
```
/newproject Andromeda v2 | Complete backend rewrite
```
**Respuesta**
```
Project created!
ID:     4
Name:   Andromeda v2
Status: active
```

---

**Con descripción y estado**
```
/newproject Andromeda v2 | Complete backend rewrite | paused
```
**Respuesta**
```
Project created!
ID:     4
Name:   Andromeda v2
Status: paused
```

---

**Estado inválido**
```
/newproject Andromeda v2 | desc | unknown
```
**Respuesta**
```
Invalid status 'unknown'. Valid: active, paused, completed, cancelled
```

---

## Escritura — Crear Sprint

### `/newsprint <projectId> | <name> [| goal] [| status] [| startDate] [| dueDate]`

Crea un sprint dentro de un proyecto. El estado por defecto es `planned`.  
Formatos de fecha aceptados: `yyyy-MM-dd` o `yyyy-MM-ddTHH:mm:ss`.

**Ejemplo**
```
/newsprint 1 | Sprint 3 | Finish auth module | active | 2026-04-15 | 2026-04-30
```
**Respuesta**
```
Sprint created!
ID:      5
Name:    Sprint 3
Project: #1 Andromeda Backend
Status:  active
Start:   2026-04-15
Due:     2026-04-30
```

---

## Escritura — Crear Tarea

### `/newtask <projectId> | <title> | <estimatedHours> | <storyPoints> [| priority] [| acceptanceCriteria]`

Crea una tarea dentro de un proyecto. `estimatedHours` debe ser > 0 y **≤ 4.0** — las tareas estimadas en más de 4 h se rechazan con una sugerencia de subdivisión. La prioridad por defecto es `medium`.

**Mínimo**
```
/newtask 1 | Fix login redirect bug | 2 | 3
```
**Respuesta**
```
Task created!
ID:          7
Title:       Fix login redirect bug
Project:     #1 Andromeda Backend
Priority:    medium
Est. hours:  2.0 h
Story pts:   3
```

---

**Con prioridad y criterios de aceptación**
```
/newtask 1 | Fix login redirect bug | 1.5 | 3 | high | Error message must be visible within 2 seconds
```
**Respuesta**
```
Task created!
ID:          7
Title:       Fix login redirect bug
Project:     #1 Andromeda Backend
Priority:    high
Est. hours:  1.5 h
Story pts:   3
```

---

**Supera el límite de 4 horas**
```
/newtask 1 | Big feature | 8 | 13
```
**Respuesta**
```
This task is estimated at 8.0 h, which exceeds the 4 h limit.
Please split it into 2 subtasks of ≤ 4 h each and add them separately.
```

---

**Proyecto no encontrado**
```
/newtask 99 | Some task | 2 | 3
```
**Respuesta**
```
Project #99 not found.
```

---

## Escritura — Asignar Tarea a Sprint

### `/assigntask <sprintId> <taskId>`

Agrega la tarea al sprint, establece su estado a `in_progress`, registra la fecha de inicio y auto-asigna al desarrollador que ejecuta el comando.

Alias: `/addsprinttask <sprintId> <taskId>`

**Ejemplo**
```
/assigntask 2 7
```
**Respuesta**
```
Task assigned to sprint!
Task:   #7 Fix login redirect bug
Sprint: #2 Sprint 2
Status: todo → in_progress
Dev:    @santiago
```

**Ya está en el sprint**
```
Task #7 is already in sprint #2.
```

**Proyectos diferentes**
```
Sprint #2 and task #7 belong to different projects.
```

---

## Escritura — Completar Tarea

### `/completetask <taskId> <actualHours>`

Marca la tarea como `done`, registra las horas reales trabajadas y establece el timestamp de completado.

**Ejemplo**
```
/completetask 7 1.5
```
**Respuesta**
```
Task completed!
ID:     7
Title:  Fix login redirect bug
Status: in_progress → done
Est. hours:  2.0 h
Act. hours:  1.5 h  (-0.5 h)
```

**Por encima del estimado**
```
/completetask 7 3.0
```
**Respuesta**
```
Task completed!
ID:     7
Title:  Fix login redirect bug
Status: in_progress → done
Est. hours:  2.0 h
Act. hours:  3.0 h  (+1.0 h)
```

---

## Escritura — Actualizar Estado de Tarea

### `/taskstatus <taskId> <status>`

**Ejemplo**
```
/taskstatus 7 done
```
**Respuesta**
```
Task #7 updated.
Title:  Fix login redirect bug
Status: in_progress → done
```

---

**Estado inválido**
```
/taskstatus 7 finished
```
**Respuesta**
```
Invalid status 'finished'. Valid: todo, in_progress, review, done
```

---

## Escritura — Actualizar Prioridad de Tarea

### `/taskpriority <taskId> <priority>`

**Ejemplo**
```
/taskpriority 7 critical
```
**Respuesta**
```
Task #7 updated.
Title:    Fix login redirect bug
Priority: medium → critical
```

---

## Escritura — Actualizar Estado de Proyecto

### `/projectstatus <projectId> <status>`

**Ejemplo**
```
/projectstatus 1 completed
```
**Respuesta**
```
Project #1 updated.
Name:   Andromeda Backend
Status: active → completed
```

---

## Escritura — Agregar Miembro

### `/addmember <projectId> <userId> [role]`

Agrega un usuario a un proyecto. El rol por defecto es `member`.

**Rol por defecto**
```
/addmember 1 3
```
**Respuesta**
```
Member added!
Project: #1 Andromeda Backend
User:    @santiago
Role:    member
```

---

**Con rol explícito**
```
/addmember 1 3 manager
```
**Respuesta**
```
Member added!
Project: #1 Andromeda Backend
User:    @santiago
Role:    manager
```

---

**Ya es miembro**
```
/addmember 1 3
```
**Respuesta**
```
@santiago is already a member of project #1.
```

---

## Valores Permitidos

| Campo | Valores permitidos |
|---|---|
| Estado del proyecto | `active` `paused` `completed` `cancelled` |
| Estado del sprint | `planned` `active` `completed` |
| Estado de la tarea | `todo` `in_progress` `review` `done` |
| Prioridad de la tarea | `low` `medium` `high` `critical` |
| Rol de miembro | `owner` `manager` `member` |
| estimatedHours | Cualquier decimal positivo ≤ 4.0 (ej. `1`, `2.5`, `4`) |
| actualHours | Cualquier decimal positivo (ej. `0.5`, `3.0`) |
| storyPoints | Cualquier entero positivo (ej. `1`, `3`, `8`, `13`) |
