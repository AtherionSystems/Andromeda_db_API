# Andromeda Backend API

API REST en Spring Boot para gestión de proyectos y tareas, respaldada por Oracle Cloud Database, con un bot de Telegram integrado para interacciones rápidas.

> 📘 **Onboarding (ES):** para una visión completa del proyecto en español, empieza por [`docs/OVERVIEW.md`](docs/OVERVIEW.md).

---

## Tabla de Contenidos

- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Primeros Pasos](#primeros-pasos)
- [Variables de Entorno](#variables-de-entorno)
- [Base de Datos](#base-de-datos)
- [Resumen de la API](#resumen-de-la-api)
- [Bot de Telegram](#bot-de-telegram)
- [Documentación](#documentación)

---

## Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | Oracle Cloud (Autonomous Database) |
| Migraciones | Flyway 10 |
| Bot | Telegram Bots API 6.9.7.1 |
| Build | Maven |

---

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/atherion/andromeda/
│   │   ├── controllers/     Controladores REST
│   │   ├── dto/             DTOs de request y response
│   │   ├── model/           Entidades JPA
│   │   ├── repositories/    Repositorios de Spring Data
│   │   ├── services/        Lógica de negocio
│   │   └── telegram/        Bot de Telegram (bot, handler, registrar)
│   └── resources/
│       ├── application.properties
│       └── db/migration/    Migraciones SQL de Flyway
└── test/
```

---

## Primeros Pasos

**Prerrequisitos:** Java 17, Maven, archivos del wallet de Oracle.

```bash
# Clonar
git clone <repo-url>
cd Andromeda_db_API

# Copiar y completar las variables de entorno (ver abajo)
cp .env.example .env

# Ejecutar
./mvnw spring-boot:run
```

La API arranca en `http://localhost:8080`.

---

## Variables de Entorno

| Variable | Descripción |
|---|---|
| `DB_USERNAME` | Usuario de la base de datos Oracle |
| `DB_PASSWORD` | Contraseña de la base de datos Oracle |
| `WALLET_PATH` | Ruta al directorio del wallet de Oracle |
| `WALLET_TRUSTSTORE_PASSWORD` | Contraseña del truststore del wallet |
| `WALLET_KEYSTORE_PASSWORD` | Contraseña del keystore del wallet |
| `TELEGRAM_BOT_TOKEN` | Token de @BotFather |
| `TELEGRAM_BOT_USERNAME` | Nombre de usuario del bot (sin @) |

---

## Base de Datos

El esquema es administrado completamente por Flyway. Hibernate está configurado en modo `validate` — nunca modifica el esquema.

Los archivos de migración se encuentran en `src/main/resources/db/migration/`.

**Tablas**

| Tabla | Descripción |
|---|---|
| `USER_TYPE` | Tipos de rol de usuario |
| `USERS` | Usuarios registrados |
| `PROJECTS` | Proyectos |
| `PROJECT_MEMBERS` | Membresía Proyecto ↔ Usuario con rol |
| `TASKS` | Tareas pertenecientes a un proyecto |
| `TASK_ASSIGNMENTS` | Asignaciones Tarea ↔ Usuario |
| `SPRINTS` | Sprints pertenecientes a un proyecto |
| `SPRINT_TASKS` | Relaciones Sprint ↔ Tarea |
| `LOGS` | Log de auditoría |

---

## Resumen de la API

| Grupo | Ruta base | Operaciones |
|---|---|---|
| Auth | `/api/auth` | Registro, login |
| Usuarios | `/api/users` | CRUD |
| Proyectos | `/api/projects` | CRUD |
| Tareas | `/api/projects/{id}/tasks` | CRUD |
| Sprints | `/api/projects/{id}/sprints` | CRUD |
| Tareas de Sprint | `/api/projects/{id}/sprints/{id}/tasks` (alias `/sprint_tasks`) | CRUD |
| Asignaciones de Tarea | `/api/projects/{id}/tasks/{id}/assignments` | Listar, asignar, eliminar |
| Miembros de Proyecto | `/api/project-members` | CRUD |
| Logs | `/api/logs` | Buscar, crear |

Referencia completa de endpoints con ejemplos de request/response: [`docs/API_ENDPOINTS.md`](docs/API_ENDPOINTS.md)

---

## Bot de Telegram

El bot conecta al arrancar mediante long-polling y responde a `/comandos`.  
Los comandos de escritura requieren vincular tu cuenta de Telegram primero con `/link`.

**Configuración:**

| Comando | Descripción |
|---|---|
| `/link <username> <password>` | Vincula tu cuenta de Telegram a tu usuario del sistema |

**Comandos de lectura** — no requieren autenticación:

| Comando | Descripción |
|---|---|
| `/projects` | Lista todos los proyectos |
| `/project <id>` | Detalle del proyecto |
| `/tasks <projectId>` | Tareas de un proyecto |
| `/task <id>` | Detalle de la tarea (incluye story points y horas) |
| `/members <projectId>` | Miembros del proyecto |
| `/sprints <projectId>` | Sprints del proyecto |
| `/sprinttasks <projectId>` | Tablero de sprint (últimos 2 sprints con responsables) |
| `/users` | Lista todos los usuarios |
| `/user <id>` | Detalle del usuario |

**Comandos de escritura** — requieren `/link`:

| Comando | Descripción |
|---|---|
| `/newproject <name> [| description] [| status]` | Crea un proyecto |
| `/newsprint <projectId> \| <name> [| goal] [| status] [| startDate] [| dueDate]` | Crea un sprint en un proyecto |
| `/newtask <projectId> \| <title> \| <estimatedHours> \| <storyPoints> [| priority] [| acceptanceCriteria]` | Crea una tarea (máx. 4 h; si excede se rechaza con sugerencia de división) |
| `/assigntask <sprintId> <taskId>` | Agrega tarea al sprint, la marca `in_progress`, auto-asigna desarrollador |
| `/addsprinttask <sprintId> <taskId>` | Alias de `/assigntask` |
| `/completetask <taskId> <actualHours>` | Marca la tarea `done` y registra horas reales |
| `/taskstatus <taskId> <status>` | Actualiza el estado de la tarea |
| `/taskpriority <taskId> <priority>` | Actualiza la prioridad de la tarea |
| `/projectstatus <projectId> <status>` | Actualiza el estado del proyecto |
| `/addmember <projectId> <userId> [role]` | Agrega usuario al proyecto |

Referencia completa de comandos con ejemplos de respuesta: [`docs/TELEGRAM_COMMANDS.md`](docs/TELEGRAM_COMMANDS.md)

---

## Documentación

| Archivo | Contenido |
|---|---|
| [`docs/OVERVIEW.md`](docs/OVERVIEW.md) | **Onboarding (ES)** — visión general del proyecto y punto de entrada |
| [`docs/MODELO_DE_DATOS.md`](docs/MODELO_DE_DATOS.md) | **(ES)** Tablas, jerarquía ágil, enums, relaciones y migraciones V1–V7 |
| [`docs/SEGURIDAD_Y_AUTENTICACION.md`](docs/SEGURIDAD_Y_AUTENTICACION.md) | **(ES)** Perfiles dev/prod, JWT, OAuth2/OCI IAM, JWKS, CORS |
| [`docs/IA_Y_RAG.md`](docs/IA_Y_RAG.md) | **(ES)** Capa de IA, comandos AI, RAG y memoria conversacional |
| [`docs/BOT_TELEGRAM.md`](docs/BOT_TELEGRAM.md) | **(ES)** Arquitectura del bot, router de intención y catálogo de comandos |
| [`docs/DESPLIEGUE_E_INFRAESTRUCTURA.md`](docs/DESPLIEGUE_E_INFRAESTRUCTURA.md) | **(ES)** Docker, OCIR, OKE/k8s, OCI DevOps, GitHub Actions |
| [`docs/GUIA_DE_DESARROLLO.md`](docs/GUIA_DE_DESARROLLO.md) | **(ES)** Cómo ejecutar, estructura, convenciones y testing |
| [`docs/API_ENDPOINTS.md`](docs/API_ENDPOINTS.md) | Todos los endpoints REST con ejemplos de request/response |
| [`docs/TELEGRAM_COMMANDS.md`](docs/TELEGRAM_COMMANDS.md) | Todos los comandos del bot con respuestas esperadas |
| [`docs/DTO_REFACTORING.md`](docs/DTO_REFACTORING.md) | Refactorización de respuestas DTO — antes/después para Tareas, Historias de Usuario, Sprints y Asignaciones de Sprint |
