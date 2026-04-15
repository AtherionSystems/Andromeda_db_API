# Andromeda Bot — Telegram Command Reference

The bot responds only to messages that start with `/`. Unknown commands are silently ignored.  
Commands work in private chats and in groups (use the `@BotUsername` suffix in groups, e.g. `/projects@AndromedaBot`).

---

## Table of Contents

- [Info & Health](#info--health)
- [Read — Projects](#read--projects)
- [Read — Tasks](#read--tasks)
- [Read — Members](#read--members)
- [Read — Sprints](#read--sprints)
- [Read — Users](#read--users)
- [Write — Create Project](#write--create-project)
- [Write — Create Task](#write--create-task)
- [Write — Update Task Status](#write--update-task-status)
- [Write — Update Task Priority](#write--update-task-priority)
- [Write — Update Project Status](#write--update-project-status)
- [Write — Add Member](#write--add-member)
- [Allowed Values](#allowed-values)

---

## Info & Health

### `/ping`
Check that the bot is reachable.

**Example**
```
/ping
```
**Response**
```
Pong! Andromeda API is up and running.
```

---

### `/health`
Check the overall API and bot status.

**Example**
```
/health
```
**Response**
```
Status: OK
Service: Andromeda Backend API
Bot: Connected
```

---

### `/help`
List every available command with syntax hints.

**Example**
```
/help
```
**Response**
```
Andromeda Bot — Commands
════════════════════════

READ
/projects              List all projects
/project <id>          Project details
/tasks <projectId>     Tasks in a project
/task <id>             Task details
/members <projectId>   Project members
/sprints <projectId>   Project sprints
/users                 List all users
/user <id>             User details

WRITE
/newproject <name> [| description] [| status]
/newtask <projectId> | <title> [| priority] [| status]
/taskstatus <taskId> <status>
/taskpriority <taskId> <priority>
/projectstatus <projectId> <status>
/addmember <projectId> <userId> [role]

VALUES
Project status : active · paused · completed · cancelled
Task status    : todo · in_progress · review · done
Task priority  : low · medium · high · critical
Member role    : owner · manager · member

OTHER
/health                API health check
/ping                  Ping the bot
```

---

## Read — Projects

### `/projects`
List every project with its current status.

**Example**
```
/projects
```
**Response**
```
Projects (3)
───────────────────────
[1] Andromeda Backend — active
[2] Mobile App — paused
[3] Legacy Migration — completed
```

---

### `/project <id>`
Full details for one project, including member and task counts.

**Example**
```
/project 1
```
**Response**
```
Project #1
Name:    Andromeda Backend
Status:  active
Start:   2025-01-15
End:     2025-12-31
Members: 4
Tasks:   12
```

**Not found**
```
Project #99 not found.
```

---

## Read — Tasks

### `/tasks <projectId>`
List all tasks that belong to a project.

**Example**
```
/tasks 1
```
**Response**
```
Tasks for project #1 (4)
───────────────────────
[3] Set up CI/CD pipeline — high | in_progress
[4] Write unit tests — medium | todo
[5] Deploy to staging — high | todo
[6] Update API docs — low | done
```

---

### `/task <id>`
Full details for a single task.

**Example**
```
/task 3
```
**Response**
```
Task #3
Title:    Set up CI/CD pipeline
Project:  #1 Andromeda Backend
Priority: high
Status:   in_progress
Start:    2025-02-01
Due:      2025-03-15
```

---

## Read — Members

### `/members <projectId>`
List all members of a project with their roles.

**Example**
```
/members 1
```
**Response**
```
Members of project #1 (3)
───────────────────────
@javier — owner
@alfredo — manager
@santiago — member
```

---

## Read — Sprints

### `/sprints <projectId>`
List all sprints for a project with status and date range.

**Example**
```
/sprints 1
```
**Response**
```
Sprints for project #1 (2)
───────────────────────
[1] Sprint 1 — completed | 2025-01-15 → 2025-01-29
[2] Sprint 2 — active | 2025-01-29 → 2025-02-12
```

---

## Read — Users

### `/users`
List every registered user.

**Example**
```
/users
```
**Response**
```
Users (3)
───────────────────────
[1] @javier — Javier García
[2] @alfredo — Alfredo López
[3] @santiago — Santiago Quiroz
```

---

### `/user <id>`
Details for a single user.

**Example**
```
/user 2
```
**Response**
```
User #2
Name:     Alfredo López
Username: @alfredo
Email:    alfredo@example.com
Phone:    +521234567890
```

---

## Write — Create Project

### `/newproject <name> [| description] [| status]`

Creates a new project. Description and status are optional; status defaults to `active`.  
Use ` | ` (pipe with spaces) to separate fields — this allows spaces in the name and description.

**Minimal (name only)**
```
/newproject Andromeda v2
```
**Response**
```
Project created!
ID:     4
Name:   Andromeda v2
Status: active
```

---

**With description**
```
/newproject Andromeda v2 | Complete backend rewrite
```
**Response**
```
Project created!
ID:     4
Name:   Andromeda v2
Status: active
```

---

**With description and status**
```
/newproject Andromeda v2 | Complete backend rewrite | paused
```
**Response**
```
Project created!
ID:     4
Name:   Andromeda v2
Status: paused
```

---

**Invalid status**
```
/newproject Andromeda v2 | desc | unknown
```
**Response**
```
Invalid status 'unknown'. Valid: active, paused, completed, cancelled
```

---

## Write — Create Task

### `/newtask <projectId> | <title> [| priority] [| status]`

Creates a task inside a project. Priority defaults to `medium`, status to `todo`.

**Minimal**
```
/newtask 1 | Fix login redirect bug
```
**Response**
```
Task created!
ID:       7
Title:    Fix login redirect bug
Project:  #1 Andromeda Backend
Priority: medium
Status:   todo
```

---

**With priority**
```
/newtask 1 | Fix login redirect bug | high
```
**Response**
```
Task created!
ID:       7
Title:    Fix login redirect bug
Project:  #1 Andromeda Backend
Priority: high
Status:   todo
```

---

**Fully specified**
```
/newtask 1 | Fix login redirect bug | high | in_progress
```
**Response**
```
Task created!
ID:       7
Title:    Fix login redirect bug
Project:  #1 Andromeda Backend
Priority: high
Status:   in_progress
```

---

**Project not found**
```
/newtask 99 | Some task
```
**Response**
```
Project #99 not found.
```

---

## Write — Update Task Status

### `/taskstatus <taskId> <status>`

**Example**
```
/taskstatus 7 done
```
**Response**
```
Task #7 updated.
Title:  Fix login redirect bug
Status: in_progress → done
```

---

**Invalid status**
```
/taskstatus 7 finished
```
**Response**
```
Invalid status 'finished'. Valid: todo, in_progress, review, done
```

---

## Write — Update Task Priority

### `/taskpriority <taskId> <priority>`

**Example**
```
/taskpriority 7 critical
```
**Response**
```
Task #7 updated.
Title:    Fix login redirect bug
Priority: medium → critical
```

---

## Write — Update Project Status

### `/projectstatus <projectId> <status>`

**Example**
```
/projectstatus 1 completed
```
**Response**
```
Project #1 updated.
Name:   Andromeda Backend
Status: active → completed
```

---

## Write — Add Member

### `/addmember <projectId> <userId> [role]`

Adds a user to a project. Role defaults to `member`.

**Default role**
```
/addmember 1 3
```
**Response**
```
Member added!
Project: #1 Andromeda Backend
User:    @santiago
Role:    member
```

---

**With explicit role**
```
/addmember 1 3 manager
```
**Response**
```
Member added!
Project: #1 Andromeda Backend
User:    @santiago
Role:    manager
```

---

**Already a member**
```
/addmember 1 3
```
**Response**
```
@santiago is already a member of project #1.
```

---

## Allowed Values

| Field | Allowed values |
|---|---|
| Project status | `active` `paused` `completed` `cancelled` |
| Task status | `todo` `in_progress` `review` `done` |
| Task priority | `low` `medium` `high` `critical` |
| Member role | `owner` `manager` `member` |
