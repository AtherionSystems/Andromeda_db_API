# Andromeda DB API — Documentación general del proyecto

> Documento maestro de onboarding. Si eres nuevo en el proyecto, **empieza por aquí**.
> Después salta a la documentación de cada módulo según lo que necesites tocar.

---

## 1. ¿Qué es Andromeda?

Andromeda es el **backend (API REST)** del sistema de gestión ágil de proyectos de Atherion Systems. Expone una API para administrar proyectos, sprints, historias de usuario, tareas, deuda técnica, métricas y miembros de equipo, e incluye dos canales de interacción adicionales:

- **API REST** consumida por el frontend (una SPA en React).
- **Bot de Telegram** con lenguaje natural + IA, para consultas y operaciones rápidas sin abrir la app.
- **Capa de IA / RAG** que responde preguntas abiertas sobre el estado del proyecto usando los datos reales almacenados en Oracle.

El repositorio es: `https://github.com/AtherionSystems/Andromeda_db_API`.
El artefacto Maven es `com.atherion.andromeda:andromeda-backend` (versión `0.0.1-SNAPSHOT`).

---

## 2. Stack tecnológico

| Capa | Tecnología | Notas |
|---|---|---|
| Lenguaje | Java 17 | |
| Framework | Spring Boot 4.0.5 | Web, Data JPA, Validation, Security, OAuth2 Resource Server, Actuator |
| Persistencia | Spring Data JPA + Hibernate | `ddl-auto=none` — Hibernate **nunca** modifica el esquema |
| Base de datos | Oracle Autonomous Database (Cloud) | Región `mx-queretaro-1`. Usa **VECTOR** de Oracle 23ai/26ai para RAG |
| Migraciones | Flyway 10 (Oracle) | Presente en el repo pero **deshabilitado** en runtime (ver §6) |
| Seguridad | Spring Security + JJWT 0.12.6 + OAuth2 (OCI IAM) | Dos perfiles: `dev` y `prod` (ver doc de seguridad) |
| Bot | Telegram Bots API 6.9.7.1 | Long-polling |
| IA | API de Gemini (compatible con OpenAI) | Chat + embeddings de 3072 dimensiones |
| Build | Maven (`mvnw` incluido) | Lombok como annotation processor |
| Contenedor | Docker (multi-stage) | Imagen runtime sobre `eclipse-temurin:17-jre-alpine` |
| Orquestación | Kubernetes (Oracle OKE) | Imagen publicada en OCIR |
| Calidad | Qodana + GitHub Actions | CI, tests unitarios y análisis estático |

---

## 3. Arquitectura de alto nivel

```
                 ┌─────────────────────────────────────────────┐
   React SPA ───▶│                                             │
                 │            ANDROMEDA BACKEND (Spring Boot)   │
  Telegram   ───▶│                                             │
                 │  Controllers ─▶ Services ─▶ Repositories ─▶ JPA
                 │       │             │                        │
                 │       │             ├─▶ AiService (Gemini)   │
                 │       │             └─▶ RAG (vector store)   │
                 │  SecurityFilterChain (perfil dev / prod)     │
                 └───────────────┬─────────────────────────────┘
                                 │ JDBC + Oracle Wallet (mTLS)
                                 ▼
                    Oracle Autonomous Database
            (tablas de negocio + CONVERSATION_SESSIONS + andromeda_vectors)
```

Capas dentro de `com.atherion.andromeda`:

| Paquete | Responsabilidad |
|---|---|
| `controllers/` | Endpoints REST. Validan entrada, delegan a servicios y devuelven DTOs |
| `services/` | Lógica de negocio. Única capa que orquesta repositorios + reglas |
| `repositories/` | Interfaces Spring Data JPA (acceso a datos) |
| `model/` | Entidades JPA (mapeo a tablas Oracle) |
| `dto/` | Objetos de request/response (separan la API del modelo interno) |
| `projections/` | Proyecciones de Spring Data para consultas de KPIs/dashboard |
| `config/` | Configuración: seguridad, CORS, propiedades de IA |
| `security/` | JWT (perfil dev) y sincronización de usuarios OAuth (perfil prod) |
| `telegram/` | Bot, manejador de comandos, router de IA, sesión conversacional |
| `util/` | Utilidades compartidas de controllers |

---

## 4. Modelo de dominio (jerarquía ágil)

El dominio sigue una jerarquía ágil de cinco niveles más entidades de soporte:

```
Project
  └── Capability
        └── Feature
              └── User Story  ──┐
                    └── Task    │
                                ├── (asignación a Sprint vía Sprint Story Assignment)
Sprint ─────────────────────────┘
  ├── Sprint Retrospective (1:1)
  └── Story Spillover (historias que se "derraman" a otro sprint)

Entidades transversales: User, UserType, ProjectMember, TaskAssignment,
Technical Debt, User Story Dependency, Log.
```

El detalle completo de tablas, columnas, enums (CHECK constraints) y relaciones está en **[`MODELO_DE_DATOS.md`](MODELO_DE_DATOS.md)**.

---

## 5. Mapa de la API REST

Todos los endpoints viven bajo `/api`. Resumen de grupos (rutas base reales tomadas de los controllers):

| Grupo | Ruta base | Controller |
|---|---|---|
| Autenticación | `/api/auth` | `AuthController` (solo perfil dev) |
| Usuarios | `/api/users` | `UserController` |
| Proyectos | `/api/projects` (alias `/projects`) | `ProjectController` |
| Miembros de proyecto | `/api/project-members` | `ProjectMembersController` |
| Capabilities | `/api/projects/{projectId}/capabilities` | `CapabilitiesController` |
| Features | `.../capabilities/{capabilityId}/features` | `FeaturesController` |
| Historias de usuario | `.../features/{featureId}/stories` | `UserStoriesController` |
| Historias de un proyecto / work items | `/api/projects/{projectId}/work-items` | `ProjectWorkItemsController` |
| Dependencias de historias | `/api/projects/{projectId}/stories/{storyId}/dependencies` | `UserStoryDependenciesController` |
| Tareas | `/api/projects/{projectId}/tasks` | `TasksController` |
| Asignaciones de tareas | `.../tasks/{taskId}/assignments` | `TaskAssignmentsController` |
| Sprints | `/api/projects/{projectId}/sprints` | `SprintsController` |
| Historias en sprint | `.../sprints/{sprintId}/user_stories` | `SprintStoryAssignmentsController` |
| Tareas en sprint | (board de sprint) | `SprintTasksController` |
| Retrospectivas | `.../sprints/{sprintId}/retrospective` | `SprintRetrospectivesController` |
| Story spillovers | `/api/projects/{projectId}/story-spillovers` | `StorySpilloversController` |
| Deuda técnica | `/api/projects/{projectId}/technical-debt` | `TechnicalDebtController` |
| Logs | (auditoría) | `LogController` |
| Dashboard / KPIs | `/api/dashboard` | `DashboardController` |
| IA | `/api/ai` | `AiNotifyController` |
| RAG (admin) | `/api/admin/rag` | `RagController` |
| JWKS | `/.well-known/jwks.json` | `JwksController` |
| Health | `/health`, `/actuator/health` | `HealthController` / Actuator |

La referencia completa con cuerpos de request/response está en **[`API_ENDPOINTS.md`](API_ENDPOINTS.md)** (en inglés, generada por el equipo).

---

## 6. Base de datos y migraciones

- El esquema se define con **Flyway** (`src/main/resources/db/migration/V1__…` a `V7__…`), pero **Flyway está deshabilitado en runtime** (`spring.flyway.enabled=false`). Los scripts se usan como **fuente de verdad / referencia** y se aplican manualmente sobre la Autonomous Database. Hibernate corre en modo `validate`/`none` y nunca altera el esquema.
- Conexión a Oracle ADB vía **wallet** (mTLS): truststore/keystore `.jks` montados desde `WALLET_PATH`.
- La tabla `andromeda_vectors` usa el tipo nativo `VECTOR(3072, FLOAT32)` de Oracle 23ai+ para búsqueda semántica (RAG).

Detalle de cada migración (incluyendo las peculiaridades de Oracle Autonomous con columnas LOB que motivaron V3/V4) en **[`MODELO_DE_DATOS.md`](MODELO_DE_DATOS.md)**.

---

## 7. Seguridad — dos perfiles

| Perfil | Mecanismo | Token | Login |
|---|---|---|---|
| `dev` (default) | `JwtAuthFilter` + `JwtUtil` (HMAC-SHA256) | JWT interno firmado con `JWT_SECRET` | `POST /api/auth/login` |
| `prod` | OAuth2 Resource Server contra **OCI IAM (IDCS)** | JWT RS256 validado con `jwks.json` (kid `SIGNING_KEY`) | Delegado 100% a OCI IAM; **no existe `/api/auth/login`** |

En `prod`, `OAuthUserSyncFilter` + `UserSyncService` sincronizan automáticamente cada usuario de OCI IAM con la tabla `USERS` (por `iam_sub`/email). Detalle completo en **[`SEGURIDAD_Y_AUTENTICACION.md`](SEGURIDAD_Y_AUTENTICACION.md)**.

---

## 8. IA, RAG y bot de Telegram

- **IA generativa:** `AiService` habla con Gemini vía su endpoint compatible con OpenAI (`/chat/completions`). Comandos del bot como `/suggest`, `/analyze`, `/fix` usan esta capa.
- **RAG (Retrieval-Augmented Generation):** indexa historias, tareas y sprints como embeddings de 3072 dims en `andromeda_vectors` y responde preguntas abiertas filtrando por el proyecto activo. Ver **[`RAG.md`](RAG.md)**.
- **Memoria conversacional:** el bot recuerda contexto (proyecto/feature/tarea activos) e historial multi-turno, persistido en `CONVERSATION_SESSIONS`. Ver **[`AI_MEMORY.md`](AI_MEMORY.md)**.
- **Router de intención:** `AiIntentRouter` traduce lenguaje natural al comando más cercano (o a `/rag_query`).

Arquitectura del bot y catálogo de comandos: **[`BOT_TELEGRAM.md`](BOT_TELEGRAM.md)** y **[`TELEGRAM_COMMANDS.md`](TELEGRAM_COMMANDS.md)**.

---

## 9. Cómo correr el proyecto en local

**Prerrequisitos:** Java 17, Maven (o el wrapper `./mvnw`), y los archivos del wallet de Oracle.

```bash
git clone https://github.com/AtherionSystems/Andromeda_db_API.git
cd Andromeda_db_API

cp .env.example .env      # rellenar credenciales (ver tabla de variables)

./mvnw spring-boot:run    # arranca en http://localhost:8080 (perfil dev por defecto)
```

Para correr el perfil de producción localmente: `SPRING_PROFILES_ACTIVE=prod`.

**Variables de entorno principales** (ver `.env.example` para la lista completa):

| Variable | Descripción |
|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de Oracle ADB |
| `WALLET_PATH` | Ruta al directorio del wallet de Oracle |
| `WALLET_TRUSTSTORE_PASSWORD` / `WALLET_KEYSTORE_PASSWORD` | Contraseñas del wallet |
| `JWT_SECRET` | Secreto Base64 (≥ 32 bytes) para JWT en perfil dev |
| `TELEGRAM_BOT_TOKEN` / `TELEGRAM_BOT_USERNAME` | Credenciales del bot (usar un bot distinto en dev para evitar el error 409) |
| `AGENT_AI_ENABLED` / `AGENT_AI_BASE_URL` / `AGENT_AI_API_KEY` / `AGENT_AI_MODEL` / `AGENT_AI_EMBEDDING_MODEL` | Configuración de IA / Gemini |
| `OAUTH2_ISSUER_URI` | Issuer de OCI IAM (perfil prod) |

Guía de desarrollo completa (estructura, convenciones, testing): **[`GUIA_DE_DESARROLLO.md`](GUIA_DE_DESARROLLO.md)**.

---

## 10. Despliegue

El backend se empaqueta en Docker, se publica en **OCIR** (Oracle Container Registry) y se despliega en **Oracle Kubernetes Engine (OKE)** con un `Deployment` + `Service` tipo LoadBalancer. El pipeline de build vive en `build_spec.yaml` (OCI DevOps) y hay workflows de GitHub Actions para CI, tests y Qodana.

Detalle completo (Dockerfile, k8s, OCIR, secretos, probes, scripts) en **[`DESPLIEGUE_E_INFRAESTRUCTURA.md`](DESPLIEGUE_E_INFRAESTRUCTURA.md)**.

---

## 11. Índice de documentación

### Documentos nuevos (panorama + detalle, en español)

| Documento | Contenido |
|---|---|
| [`OVERVIEW.md`](OVERVIEW.md) | **(este documento)** Panorama general y punto de entrada |
| [`MODELO_DE_DATOS.md`](MODELO_DE_DATOS.md) | Todas las tablas, jerarquía, enums, relaciones y migraciones V1–V7 |
| [`SEGURIDAD_Y_AUTENTICACION.md`](SEGURIDAD_Y_AUTENTICACION.md) | Perfiles dev/prod, JWT, OAuth2/OCI IAM, JWKS, CORS, sync de usuarios |
| [`IA_Y_RAG.md`](IA_Y_RAG.md) | Capa de IA (Gemini), comandos AI, RAG y memoria conversacional |
| [`BOT_TELEGRAM.md`](BOT_TELEGRAM.md) | Arquitectura del bot, router de intención, sesión y comandos |
| [`DESPLIEGUE_E_INFRAESTRUCTURA.md`](DESPLIEGUE_E_INFRAESTRUCTURA.md) | Docker, OCIR, OKE/k8s, OCI DevOps, GitHub Actions, scripts |
| [`GUIA_DE_DESARROLLO.md`](GUIA_DE_DESARROLLO.md) | Cómo correr, estructura, convenciones y testing |

### Documentos existentes (referencia técnica)

| Documento | Contenido | Idioma |
|---|---|---|
| [`API_ENDPOINTS.md`](API_ENDPOINTS.md) | Referencia completa de endpoints REST con ejemplos | EN |
| [`TELEGRAM_COMMANDS.md`](TELEGRAM_COMMANDS.md) | Catálogo de comandos del bot con respuestas | EN |
| [`RAG.md`](RAG.md) | Detalle técnico del sistema RAG | EN |
| [`AI_MEMORY.md`](AI_MEMORY.md) | Implementación de memoria del bot en 4 fases | ES |
| [`DTO_REFACTORING.md`](DTO_REFACTORING.md) | Refactor de DTOs de respuesta (antes/después) | EN |
| [`security-auth-guide.txt`](security-auth-guide.txt) | Guía original de la capa JWT (perfil dev) | EN |
| [`../OCI_IAM_OAUTH2_FRONTEND_INTEGRATION.txt`](../OCI_IAM_OAUTH2_FRONTEND_INTEGRATION.txt) | Integración del frontend React con OCI IAM | EN |

---

## 12. Estado del proyecto (historial de funcionalidades)

Las funcionalidades principales entregadas, según el historial de Git, son:

1. **Núcleo de gestión de proyectos** — proyectos, tareas, sprints, miembros, logs (migración V1).
2. **Campos ágiles** — estimación en horas, story points, criterios de aceptación, vinculación de Telegram (V2–V4).
3. **Jerarquía ágil completa** — capabilities, features, historias de usuario, dependencias, retrospectivas, story spillovers, deuda técnica (V5).
4. **Bot de Telegram con IA** — comandos de lectura/escritura, router de intención por lenguaje natural.
5. **Memoria del bot** — sesión y contexto persistente (V6).
6. **RAG** — búsqueda semántica sobre datos del proyecto (V7).
7. **Capa de seguridad JWT** (perfil dev) y luego **OAuth2 con OCI IAM** (perfil prod).
8. **Calidad y CI** — Qodana + GitHub Actions.
9. **Despliegue en OCI** — Docker, OCIR y Kubernetes (OKE).
