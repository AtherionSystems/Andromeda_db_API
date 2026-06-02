# Seguridad y autenticación

El backend tiene **dos esquemas de autenticación distintos**, seleccionados por el perfil de Spring activo. La pieza central es `config/SecurityConfig.java`, que declara una `SecurityFilterChain` distinta para cada perfil.

| Perfil | Bean activo | Mecanismo | Origen del token |
|---|---|---|---|
| `dev` / default (`@Profile("!prod")`) | `defaultFilterChain` | JWT propio (HMAC-SHA256) vía `JwtAuthFilter` + `JwtUtil` | `POST /api/auth/login` |
| `prod` (`@Profile("prod")`) | `prodFilterChain` | OAuth2 Resource Server contra **OCI IAM (IDCS)**, JWT RS256 | OCI IAM (flujo Authorization Code + PKCE en el frontend) |

Ambas cadenas comparten: CSRF deshabilitado (API stateless), CORS vía `CorsConfig`, y `SessionCreationPolicy.STATELESS`.

---

## 1. Perfil `dev` (JWT interno)

Flujo:

1. El cliente hace `POST /api/auth/login` con usuario y contraseña.
2. El backend valida contra la BD (comparación de hash **BCrypt**).
3. Devuelve los datos del usuario + un **JWT firmado** (`LoginResponse`).
4. El cliente envía `Authorization: Bearer <token>` en cada request.
5. `JwtAuthFilter` valida el token en cada petición antes de llegar al controller.

Componentes:

- **`security/JwtUtil.java`** — `generateToken(User)` (claims: subject=username, `userId`, `userType`; firma HMAC-SHA256 con `JWT_SECRET`), `extractUsername(token)`, `isTokenValid(token)`.
- **`security/JwtAuthFilter.java`** — `OncePerRequestFilter`: lee el header `Authorization`, y si el token es válido coloca al usuario en el `SecurityContext`. Si falta o es inválido, el request sigue y lo bloquea la config de seguridad.
- **`dto/LoginResponse.java`** — respuesta plana con `token` + datos del usuario.

Endpoints públicos en dev: `/api/auth/**`, `/`, `/health`, `/actuator/health`. Todo lo demás requiere token.

Configuración:

```properties
jwt.secret=${JWT_SECRET}        # Base64 que decodifica a >= 32 bytes (256 bits)
jwt.expiration.ms=86400000      # 24 horas
```

> Nota: ante un token faltante, Spring Security responde **403** (no 401) por defecto al no haber `AuthenticationEntryPoint`. Funcionalmente bloquea igual.

Detalle histórico de esta capa: [`security-auth-guide.txt`](security-auth-guide.txt).

---

## 2. Perfil `prod` (OAuth2 con OCI IAM)

En producción la autenticación se **delega 100% a OCI IAM**. El backend **no expone `/api/auth/login`**; solo valida tokens emitidos por OCI IAM.

Flujo:

```
React SPA ──▶ OCI IAM (login)
          ◀── Authorization Code (+ PKCE)
React intercambia code por tokens
          ──▶ Backend prod:  Authorization: Bearer <access_token OCI>
Backend valida firma RS256 con jwks.json (kid=SIGNING_KEY) y devuelve el recurso
```

Detalles de `prodFilterChain`:

- `oauth2ResourceServer().jwt()` con un `JwtDecoder` **Nimbus** construido a partir de `src/main/resources/jwks.json` (clave pública RSA-256 de OCI IDCS, `kid="SIGNING_KEY"`, alg `RS256`).
- El validador solo verifica **timestamp** (`JwtTimestampValidator`); el `issuer` no se valida en el decoder (se construye con `DelegatingOAuth2TokenValidator` sin issuer).
- `JwtAuthFilter` (el de dev) se **desactiva** explícitamente con un `FilterRegistrationBean.setEnabled(false)`.
- Se añade `OAuthUserSyncFilter` después del `BearerTokenAuthenticationFilter`.
- Endpoints públicos: `OPTIONS /**` (preflight), `/`, `/health`, `/actuator/health`. El resto requiere token válido.
- Propiedad relacionada: `spring.security.oauth2.resourceserver.jwt.audiences=http://localhost:8080` (en `application-prod.properties`) y `OAUTH2_ISSUER_URI`.

### Sincronización de usuarios OAuth

`security/OAuthUserSyncFilter` + `services/UserSyncService` crean/vinculan el usuario local a partir del JWT de OCI:

- Lee `sub` (que en OCI es el email, p. ej. `a0157122@tec.mx`) y el claim `user_displayname`.
- `syncOAuthUser(iamSub, displayName)`:
  1. Busca por `iam_sub`; si existe, lo usa.
  2. Si no, busca por `email` (usuario legacy) y le vincula el `iam_sub`.
  3. Si tampoco, **crea** un usuario nuevo (`password_hash="OAUTH"`, `user_type_id=212` por defecto).
- Si el sync falla, **no bloquea** el request (se loguea el error y continúa).

El bot tiene además los comandos `/linkoci` y `/whoami` relacionados con esta identidad.

---

## 3. CORS (`config/CorsConfig.java`)

Se expone un `CorsConfigurationSource` (no un `CorsFilter`) para que Spring Security lo aplique dentro de su filter chain (evita procesar CORS dos veces).

- **Orígenes permitidos:** `http://localhost:5173`, `http://localhost:3000` y 4 IPs de instancias OCI (`159.54.154.149`, `140.84.181.23`, `163.192.143.43`, `160.34.209.27`).
- **Métodos:** GET, POST, PUT, PATCH, DELETE, OPTIONS (PATCH es necesario por los `@PatchMapping`).
- **Headers:** todos (`*`), incluido `Authorization`.
- **Exposed headers:** `Location` (para leer la URI de respuestas `201 Created`).
- **`allowCredentials=false`** — correcto para una API stateless con Bearer tokens (no cookies).
- Preflight cacheado 1 hora (`maxAge=3600`).

> Al añadir un origen nuevo (otra IP/instancia o dominio del frontend), recuerda actualizar esta lista.

---

## 4. JWKS endpoint

`JwksController` publica `GET /.well-known/jwks.json`, sirviendo el conjunto de claves públicas del backend. Combinado con `jwks.json` en resources, permite que terceros validen tokens emitidos por el sistema.

---

## 5. Variables de entorno relevantes

| Variable | Perfil | Notas |
|---|---|---|
| `JWT_SECRET` | dev | Base64 ≥ 32 bytes. Tokens de un entorno no sirven en otro |
| `OAUTH2_ISSUER_URI` | prod | Issuer de OCI IAM |
| `TELEGRAM_BOT_TOKEN` / `TELEGRAM_BOT_USERNAME` | ambos | Usar un bot distinto en dev para evitar el conflicto 409 de Telegram (solo una sesión long-poll por token) |

Para correr los tests, se necesita un `jwt.secret` de prueba en `src/test/resources/application.properties`.
