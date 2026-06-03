# Guía de desarrollo

Cómo trabajar en el código: requisitos, estructura, convenciones y pruebas.

---

## 1. Requisitos y arranque

- **Java 17** y **Maven** (o el wrapper `./mvnw` / `mvnw.cmd`).
- Archivos del **wallet de Oracle** y un `.env` (copiar de `.env.example`).

```bash
./mvnw spring-boot:run      # perfil dev por defecto, en http://localhost:8080
./mvnw test                 # corre la batería de pruebas
./mvnw package -DskipTests  # genera el jar en target/
```

Variables de entorno: ver [`OVERVIEW.md`](OVERVIEW.md) §9 y `.env.example`.

---

## 2. Estructura de paquetes (`com.atherion.andromeda`)

```
AndromedaBackendApplication   # @SpringBootApplication + @EnableAsync (punto de entrada)
HealthController              # /health
GlobalErrorController        # manejo global de errores

controllers/   # endpoints REST (uno por agregado)
services/      # lógica de negocio
repositories/  # interfaces Spring Data JPA
model/         # entidades JPA
dto/           # requests/responses; dto/dashboard para KPIs
projections/   # proyecciones de Spring Data para consultas de dashboard
config/        # SecurityConfig, CorsConfig, AiProperties
security/      # JwtUtil, JwtAuthFilter (dev); OAuthUserSyncFilter (prod)
telegram/      # bot, handler de comandos, router de IA, sesión
util/          # ControllerUtils
```

### Convenciones

- **DTOs en vez de entidades en la API.** Los controllers nunca exponen entidades JPA directamente; usan DTOs de request y response (a menudo con un método estático `from(...)`). El refactor que estableció este patrón está documentado en [`DTO_REFACTORING.md`](DTO_REFACTORING.md).
- **Lombok** para boilerplate (`@Getter/@Setter`, `@RequiredArgsConstructor`, `@Slf4j`). Está configurado como annotation processor y excluido del jar final.
- **Validación** con Jakarta Validation (`spring-boot-starter-validation`) en los DTOs de request.
- **Proyecciones** (`projections/`) para los KPIs del dashboard, evitando cargar entidades completas: `BurndownProjection`, `TeamVelocityProjection`, `HoursPerUserProjection`, `TaskDistributionProjection`, `UserTasksPerSprintProjection`.
- **Auditoría** (`created_by/at`, `updated_by/at`) presente en las tablas de negocio; ver [`MODELO_DE_DATOS.md`](MODELO_DE_DATOS.md).
- **Async** habilitado (`@EnableAsync`) para tareas en segundo plano (p. ej. notificaciones de IA).

---

## 3. Dashboard / KPIs

`DashboardController` (`/api/dashboard`) + `KpiService` + `KpiRepository` exponen métricas calculadas por proyecto/sprint, agregadas en `dto/dashboard/`:

- `BurndownKPI`, `TeamVelocityKPI`, `HoursPerUserKPI`, `TaskDistributionKPI`, `UserTasksPerSprintKPI`, y el agregador `DashboardResponse`.

---

## 4. Pruebas

- **28 clases de test** con **~184 métodos `@Test`** en `src/test/java/com/atherion/andromeda/`.
- Cubren controllers (Tasks, Sprints, UserStories, ProjectMembers, Dashboard, Capabilities, etc.), seguridad (`JwtUtilTest`, `JwtAuthFilterTest`, `AuthIntegrationTest`), integración (`ProjectControllerIntegrationTest`, `LogIntegrationTest`, `UserTypeIntegrationTest`) y el bot/IA (`AndromedaBotTest`, `BotCommandHandlerTest`, `ConversationMemoryTest`, `AiIntentRouterMemoryTest`, `TaskLifecycleBotTest`).
- Los tests de memoria del bot usan `@ExtendWith(MockitoExtension.class)` con mocks (no requieren contexto de Spring).
- El test de carga de contexto necesita un `jwt.secret` de prueba en `src/test/resources/application.properties`.

```bash
./mvnw test                                   # todos
./mvnw -Dtest=BotCommandHandlerTest test       # una clase
```

---

## 5. Calidad de código

- **Qodana** (`qodana.yaml`, workflow `.github/workflows/qodana.yml`) realiza análisis estático. El reporte SARIF de una corrida está en `qodana.sarif.json`.

---

## 6. Reglas del repositorio

- **El esquema no se cambia desde código.** Hibernate está en `ddl-auto=none`; cualquier cambio de esquema se hace con un nuevo script `V*.sql` aplicado manualmente.
- Mantén la separación controller → service → repository; la lógica de negocio vive en los services.
- Al exponer datos nuevos en la API, crea/actualiza un DTO en vez de devolver la entidad.
- Si agregas un origen de frontend, actualiza la lista de CORS en `CorsConfig` (ver [`SEGURIDAD_Y_AUTENTICACION.md`](SEGURIDAD_Y_AUTENTICACION.md) §3).
