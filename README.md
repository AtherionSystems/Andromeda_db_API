# Andromeda Backend API

Spring Boot REST API for project and task management, backed by Oracle Cloud Database, with an integrated Telegram bot for quick interactions.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Database](#database)
- [API Overview](#api-overview)
- [Telegram Bot](#telegram-bot)
- [Documentation](#documentation)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Persistence | Spring Data JPA + Hibernate |
| Database | Oracle Cloud (Autonomous Database) |
| Migrations | Flyway 10 |
| Bot | Telegram Bots API 6.9.7.1 |
| Build | Maven |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/atherion/andromeda/
│   │   ├── controllers/     REST controllers
│   │   ├── dto/             Request and response DTOs
│   │   ├── model/           JPA entities
│   │   ├── repositories/    Spring Data repositories
│   │   ├── services/        Business logic
│   │   └── telegram/        Telegram bot (bot, handler, registrar)
│   └── resources/
│       ├── application.properties
│       └── db/migration/    Flyway SQL migrations
└── test/
```

---

## Getting Started

**Prerequisites:** Java 17, Maven, Oracle wallet files.

```bash
# Clone
git clone <repo-url>
cd Andromeda_db_API

# Copy and fill in environment variables (see below)
cp .env.example .env

# Run
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

---

## Environment Variables

| Variable | Description |
|---|---|
| `DB_USERNAME` | Oracle database username |
| `DB_PASSWORD` | Oracle database password |
| `WALLET_PATH` | Path to the Oracle wallet directory |
| `WALLET_TRUSTSTORE_PASSWORD` | Truststore password from the wallet |
| `WALLET_KEYSTORE_PASSWORD` | Keystore password from the wallet |
| `TELEGRAM_BOT_TOKEN` | Token from @BotFather |
| `TELEGRAM_BOT_USERNAME` | Bot username (without @) |

---

## Database

Schema is managed entirely by Flyway. Hibernate is set to `validate` mode — it never modifies the schema.

Migration files live in `src/main/resources/db/migration/`.

**Tables**

| Table | Description |
|---|---|
| `USER_TYPE` | User role types |
| `USERS` | Registered users |
| `PROJECTS` | Projects |
| `PROJECT_MEMBERS` | Project ↔ User membership with role |
| `TASKS` | Tasks belonging to a project |
| `TASK_ASSIGNMENTS` | Task ↔ User assignments |
| `SPRINTS` | Sprints belonging to a project |
| `SPRINT_TASKS` | Sprint ↔ Task relationships |
| `LOGS` | Audit log |

---

## API Overview

| Group | Base path | Operations |
|---|---|---|
| Auth | `/api/auth` | Register, login |
| Users | `/api/users` | CRUD |
| Projects | `/api/projects` | CRUD |
| Tasks | `/api/projects/{id}/tasks` | CRUD |
| Task Assignments | `/api/projects/{id}/tasks/{id}/assignments` | List, assign, remove |
| Project Members | `/api/project-members` | CRUD |
| Logs | `/api/logs` | Search, create |

Full endpoint reference with request/response examples: [`docs/API_ENDPOINTS.md`](docs/API_ENDPOINTS.md)

---

## Telegram Bot

The bot connects on startup via long-polling and responds to `/commands`.

**Read commands** — query the database:

| Command | Description |
|---|---|
| `/projects` | List all projects |
| `/project <id>` | Project details |
| `/tasks <projectId>` | Tasks in a project |
| `/task <id>` | Task details |
| `/members <projectId>` | Project members |
| `/sprints <projectId>` | Project sprints |
| `/users` | List all users |
| `/user <id>` | User details |

**Write commands** — mutate the database:

| Command | Description |
|---|---|
| `/newproject <name> [| description] [| status]` | Create a project |
| `/newtask <projectId> \| <title> [| priority] [| status]` | Create a task |
| `/taskstatus <taskId> <status>` | Update task status |
| `/taskpriority <taskId> <priority>` | Update task priority |
| `/projectstatus <projectId> <status>` | Update project status |
| `/addmember <projectId> <userId> [role]` | Add user to project |

Full command reference with response examples: [`docs/TELEGRAM_COMMANDS.md`](docs/TELEGRAM_COMMANDS.md)

---

## Documentation

| File | Contents |
|---|---|
| [`docs/API_ENDPOINTS.md`](docs/API_ENDPOINTS.md) | All REST endpoints with request/response shapes |
| [`docs/TELEGRAM_COMMANDS.md`](docs/TELEGRAM_COMMANDS.md) | All bot commands with expected responses |
