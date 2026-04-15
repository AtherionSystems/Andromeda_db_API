# Andromeda Bot — Telegram Command Reference

The bot responds only to messages that start with `/`. Unknown commands are silently ignored.  
Commands work in private chats and in groups (use the `@BotUsername` suffix in groups, e.g. `/projects@AndromedaBot`).

---

## Table of Contents

- [Info & Health](#info--health)
- [Account Linking](#account-linking)
- [Read — Projects](#read--projects)
- [Read — Tasks](#read--tasks)
- [Read — Members](#read--members)
- [Read — Sprints](#read--sprints)
- [Read — Sprint Board](#read--sprint-board)
- [Read — Users](#read--users)
- [Write — Create Project](#write--create-project)
- [Write — Create Task](#write--create-task)
- [Write — Assign Task to Sprint](#write--assign-task-to-sprint)
- [Write — Complete Task](#write--complete-task)
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
/newtask <projectId> | <title> | <estimatedHours> | <storyPoints> [| priority] [| acceptanceCriteria]
/assigntask <sprintId> <taskId>
/completetask <taskId> <actualHours>
/taskstatus <taskId> <status>
/taskpriority <taskId> <priority>
/projectstatus <projectId> <status>
/addmember <projectId> <userId> [role]

VALUES
Project status : active · paused · completed · cancelled
Task status    : todo · in_progress · review · done
Task priority  : low · medium · high · critical
Member role    : owner · manager · member
Max est. hours : 4.0 h per task

OTHER
/health                API health check
/ping                  Ping the bot
```

---

## Account Linking

All write commands require your Telegram account to be linked to a system user. Read commands work without linking.

### `/link <username> <password>`
Authenticate with your system credentials. The bot stores your Telegram user ID so subsequent write commands know who you are.

**Example**
```
/link santiago secret123
```
**Response**
```
Linked! Welcome, Santiago Quiroz (@santiago).
You can now use all write commands.
```

**Wrong credentials**
```
Invalid username or password.
```

**Already linked**
```
Already linked to @santiago. Welcome back, Santiago Quiroz!
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
[3] Set up CI/CD pipeline — high | in_progress | 5 pts | 3.0 h
[4] Write unit tests — medium | todo | 2 pts | 1.5 h
[5] Deploy to staging — high | todo | 5 pts | 4.0 h
[6] Update API docs — low | done | 1 pts | 1.0 h
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

## Read — Sprint Board

### `/sprinttasks <projectId>`
Displays the task board for the **last 2 sprints** of a project. Tasks are grouped by sprint and ordered by status (in_progress → review → todo → done) then priority. Assignees are JOINed from task assignments.

**Example**
```
/sprinttasks 1
```
**Response**
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

**No tasks found**
```
No tasks found in recent sprints for project #1.
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

### `/newtask <projectId> | <title> | <estimatedHours> | <storyPoints> [| priority] [| acceptanceCriteria]`

Creates a task inside a project. `estimatedHours` must be > 0 and **≤ 4.0** — tasks estimated above 4 h are rejected with a subdivision suggestion. Priority defaults to `medium`.

**Minimal**
```
/newtask 1 | Fix login redirect bug | 2 | 3
```
**Response**
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

**With priority and acceptance criteria**
```
/newtask 1 | Fix login redirect bug | 1.5 | 3 | high | Error message must be visible within 2 seconds
```
**Response**
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

**Exceeds 4-hour limit**
```
/newtask 1 | Big feature | 8 | 13
```
**Response**
```
This task is estimated at 8.0 h, which exceeds the 4 h limit.
Please split it into 2 subtasks of ≤ 4 h each and add them separately.
```

---

**Project not found**
```
/newtask 99 | Some task | 2 | 3
```
**Response**
```
Project #99 not found.
```

---

## Write — Assign Task to Sprint

### `/assigntask <sprintId> <taskId>`

Adds the task to the sprint, sets its status to `in_progress`, records the start date, and auto-assigns the calling developer to the task.

**Example**
```
/assigntask 2 7
```
**Response**
```
Task assigned to sprint!
Task:   #7 Fix login redirect bug
Sprint: #2 Sprint 2
Status: todo → in_progress
Dev:    @santiago
```

**Already in sprint**
```
Task #7 is already in sprint #2.
```

**Different projects**
```
Sprint #2 and task #7 belong to different projects.
```

---

## Write — Complete Task

### `/completetask <taskId> <actualHours>`

Marks the task as `done`, records the actual hours worked, and sets the completion timestamp.

**Example**
```
/completetask 7 1.5
```
**Response**
```
Task completed!
ID:     7
Title:  Fix login redirect bug
Status: in_progress → done
Est. hours:  2.0 h
Act. hours:  1.5 h  (-0.5 h)
```

**Over estimate**
```
/completetask 7 3.0
```
**Response**
```
Task completed!
ID:     7
Title:  Fix login redirect bug
Status: in_progress → done
Est. hours:  2.0 h
Act. hours:  3.0 h  (+1.0 h)
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
| estimatedHours | Any positive decimal ≤ 4.0 (e.g. `1`, `2.5`, `4`) |
| actualHours | Any positive decimal (e.g. `0.5`, `3.0`) |
| storyPoints | Any positive integer (e.g. `1`, `3`, `8`, `13`) |
