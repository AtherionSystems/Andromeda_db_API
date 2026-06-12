# Bot de Telegram

El bot conecta por **long-polling** al arrancar la aplicación y responde a comandos. Mezcla comandos clásicos (`/comando args`) con lenguaje natural (resuelto por el `AiIntentRouter`). El catálogo de comandos con ejemplos de respuesta está en [`TELEGRAM_COMMANDS.md`](TELEGRAM_COMMANDS.md).

---

## 1. Arquitectura (paquete `telegram/`)

| Clase | Rol |
|---|---|
| `TelegramBotProperties` | Propiedades `telegram.bot.token` / `telegram.bot.username` |
| `TelegramBotRegistrar` | Registra el bot en la Telegram Bots API al iniciar |
| `AndromedaBot` | Entrada de updates de Telegram; decide entre comando directo o NLU |
| `BotCommandHandler` | Implementa cada comando (lectura y escritura), arma las respuestas |
| `AiIntentRouter` | Traduce lenguaje natural → comando (ver [`IA_Y_RAG.md`](IA_Y_RAG.md)) |
| `EntityResolver` | Resuelve nombres de entidades a IDs |
| `ConversationSession` / `ConversationSessionManager` | Contexto y memoria por usuario |
| `ConversationSession` (entidad) | Persistencia en `CONVERSATION_SESSIONS` |
| `ChatMessage` | Record `(role, content)` para el historial |

> Para desarrollo local usa un **bot distinto** al de producción. Telegram solo permite una sesión de long-polling por token; correr dos a la vez con el mismo token produce el error **409 Conflict**.

---

## 2. Catálogo de comandos

### Vinculación / identidad
| Comando | Descripción |
|---|---|
| `/link <username> <password>` | Vincula tu cuenta de Telegram a tu usuario del sistema (perfil dev) |
| `/linkoci` | Vinculación vía identidad OCI IAM |
| `/whoami` | Muestra el usuario vinculado |

### Lectura (no requieren vínculo)
| Comando | Descripción |
|---|---|
| `/projects` | Lista todos los proyectos |
| `/project <id>` | Detalle de un proyecto |
| `/capabilities <projectId>` | Capabilities de un proyecto |
| `/capability <id>` | Detalle de capability |
| `/features <capabilityId>` | Features de una capability |
| `/feature <id>` | Detalle de feature |
| `/projectstories <projectId>` | Todas las historias de un proyecto |
| `/userstories <featureId>` | Historias de una feature |
| `/userstory <id>` | Detalle de historia |
| `/tasks <projectId>` | Tareas de un proyecto |
| `/task <id>` | Detalle de tarea (incluye story points y horas) |
| `/members <projectId>` | Miembros del proyecto |
| `/sprints <projectId>` | Sprints del proyecto |
| `/sprinttasks <projectId>` | Tablero de sprint (últimos 2 sprints, con responsables) |
| `/users` | Lista de usuarios |
| `/user <id>` | Detalle de usuario |

### Escritura (requieren `/link`)
| Comando | Descripción |
|---|---|
| `/newproject <name> [\| description] [\| status]` | Crea un proyecto y **agrega automáticamente al creador como `owner`** |
| `/newsprint <projectId> \| <name> [\| goal] [\| status] [\| startDate] [\| dueDate]` | Crea un sprint |
| `/newtask <projectId> \| <title> \| <estimatedHours> [\| priority]` | Crea una tarea (máx 4 h; si excede, se rechaza y se sugiere dividir) |
| `/assigntask <sprintId> <taskId>` | Agrega tarea al sprint, la marca `in_progress` y auto-asigna desarrollador |
| `/addsprinttask <sprintId> <taskId>` | Alias de `/assigntask` |
| `/assignuser <taskId> <userId>` | Asigna un usuario (persona) a una tarea; re-indexa el vector RAG automáticamente |
| `/completetask <taskId> <actualHours>` | Marca la tarea `done` y registra horas reales |
| `/taskstatus <taskId> <status>` | Cambia el estado de una tarea |
| `/taskpriority <taskId> <priority>` | Cambia la prioridad |
| `/projectstatus <projectId> <status>` | Cambia el estado de un proyecto |
| `/addmember <projectId> <userId> [role]` | Agrega un usuario al proyecto (requiere ser manager/owner) |

### IA y utilidades
| Comando | Descripción |
|---|---|
| `/suggest <projectId>` | Sugerencias de IA para el proyecto |
| `/analyze <projectId>` | Análisis de salud del proyecto |
| `/fix <taskId>` | Guía de IA para resolver una tarea |
| `/rag_query` | Pregunta abierta sobre el proyecto (disparado por el router) |
| `/pingai` | Prueba de conectividad con la IA |
| `/ping` / `/health` | Estado del bot/servicio |
| `/help` | Ayuda con los comandos |

---

## 3. Interacción usando lenguaje natural

El usuario no necesita recordar IDs ni comandos exactos. El `AiIntentRouter` soporta tanto comandos de lectura como de escritura:

**Lectura**
- "muéstrame las tareas del proyecto Andromeda" → `/tasks <projectId>`
- "¿qué historias están en progreso?" → `/rag_query` (RAG sobre el proyecto activo)
- "y ese sprint, ¿cuál es su meta?" → usa el historial y el contexto activo de la sesión

**Escritura**
- "Add a new task for project RAG Test called 'Fix login' with 2 hours and high priority" → `/newtask <projectId> | Fix login | 2 | high`
- "Assign the task 'Fix login' to user 27" → `/assignuser <taskId> 27`
- "mark task 15 as done" → `/taskstatus 15 done`

El router inyecta en el *system prompt* del LLM la lista de proyectos, capabilities, features, historias, tareas **y miembros** del proyecto activo, para que pueda resolver nombres propios a IDs numéricos sin que el usuario tenga que conocerlos.

El contexto activo (proyecto/feature/historia/tarea) y el historial se mantienen en la sesión y persisten entre reinicios (ver [`IA_Y_RAG.md`](IA_Y_RAG.md) §4).
