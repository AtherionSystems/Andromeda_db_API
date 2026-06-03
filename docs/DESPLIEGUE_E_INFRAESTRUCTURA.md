# Despliegue e infraestructura

El backend se conteneriza con Docker, se publica en **OCIR** (Oracle Cloud Infrastructure Registry) y se ejecuta en **Oracle Kubernetes Engine (OKE)**. La base de datos es Oracle Autonomous Database accedida por wallet (mTLS).

---

## 1. Docker

### `Dockerfile` (multi-stage)
- **Stage build:** `eclipse-temurin:17-jdk-alpine`. Copia `mvnw`, `.mvn`, `pom.xml`, descarga dependencias offline, copia `src` y ejecuta `./mvnw package -DskipTests`.
- **Stage runtime:** `eclipse-temurin:17-jre-alpine`. Crea un usuario no-root `andromeda`, copia el jar `andromeda-backend-*.jar`, expone el puerto **8080** y ejecuta `java -jar app.jar`.

### `docker-compose.yml`
Servicio `api`: build local, mapea `8080:8080`, carga variables desde `.env`, **sobrescribe `WALLET_PATH=/app/wallet`** y monta el wallet local (`./Wallet_andromedadb`) como volumen de solo lectura en `/app/wallet`. `restart: unless-stopped`.

### `.dockerignore` / `.gitattributes`
Excluyen archivos innecesarios del contexto de build y normalizan saltos de línea (relevante para `mvnw` en builds Linux).

---

## 2. Build y publicación (OCI DevOps) — `build_spec.yaml`

Pipeline de OCI DevOps (`version 0.1`, `runAs root`):

- Variables: `REGION=mx-queretaro-1`, `OCIR_SERVER=mx-queretaro-1.ocir.io`, `TENANCY_NAMESPACE=axieboiigznv`, `REPO_NAME=andromeda-backend`, `IMAGE_TAG=latest`.
- Pasos: (1) `docker build` con tag `${OCIR_SERVER}/${TENANCY_NAMESPACE}/${REPO_NAME}:${IMAGE_TAG}`; (2) `docker login` a OCIR (usuario `axieboiigznv/...`, password = `${AUTH_TOKEN}`) y `docker push`.
- Artefacto de salida: la imagen Docker en OCIR.

---

## 3. Kubernetes (OKE) — `k8s/andromeda-deployment.yaml`

- **Namespace:** `andromeda`.
- **Deployment** `andromeda`: 1 réplica, imagen `mx-queretaro-1.ocir.io/axieboiigznv/andromeda-backend:latest`, puerto 8080.
  - `imagePullSecrets: ocir-secret` para autenticar el pull desde OCIR.
  - Variables desde el secret `andromeda-secrets` (`DB_USERNAME`, `DB_PASSWORD`, `WALLET_TRUSTSTORE_PASSWORD`, `WALLET_KEYSTORE_PASSWORD`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`) y `WALLET_PATH=/app/wallet`.
  - **Wallet** montado desde el secret `db-wallet-secret` en `/app/wallet` (solo lectura).
  - **Probes** (`readiness` y `liveness`) sobre `GET /actuator/health`.
- **Service** `andromeda-service`: tipo `LoadBalancer`, puerto 80 → targetPort 8080.

> Secrets de Kubernetes requeridos antes de desplegar: `andromeda-secrets` (credenciales/config), `db-wallet-secret` (archivos del wallet) y `ocir-secret` (pull desde OCIR).

### Scripts de operación (`scripts/`)
- `andromeda-bringup.sh` — levantar/desplegar el entorno.
- `andromeda-undeploy.sh` — bajar el despliegue.

`ocir-secret.txt` documenta cómo crear el secret de pull de OCIR.

---

## 4. Integración continua (GitHub Actions) — `.github/workflows/`

| Workflow | Propósito |
|---|---|
| `ci.yml` | Pipeline general de integración continua |
| `unit-tests.yml` | Ejecuta la batería de pruebas unitarias |
| `qodana.yml` | Análisis estático de código con JetBrains **Qodana** |

La configuración de Qodana está en `qodana.yaml`; los resultados de una corrida se guardan en `qodana.sarif.json`.

---

## 5. Base de datos — Oracle Autonomous Database

- URL TCPS a `adb.mx-queretaro-1.oraclecloud.com:1522`, service `..._andromedadb_tp`, con `ssl_server_dn_match=yes`.
- Autenticación **mTLS por wallet**: truststore/keystore `.jks` referenciados vía propiedades Hikari (`javax.net.ssl.trustStore` / `keyStore`) usando `WALLET_PATH` + contraseñas.
- Hibernate en modo `none`/`validate`; el esquema se gestiona aparte (ver [`MODELO_DE_DATOS.md`](MODELO_DE_DATOS.md)).

---

## 6. Perfiles y configuración de arranque

| Perfil | Cómo activarlo | Seguridad |
|---|---|---|
| `dev` / default | sin `SPRING_PROFILES_ACTIVE` (o `=dev`) | JWT interno + `/api/auth/login` |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | OAuth2 / OCI IAM, sin login local |

Configuraciones de ejecución del IDE en `.run/`: `Andromeda (local).run.xml` y `Andromeda (prod).run.xml`.

### Documentos de despliegue históricos (HTML, en la raíz)
- `AndromedaDEPLOY.html`, `AndromedaRESET.html`, `andromeda_oke_ops_guide.html` — guías operativas de despliegue/reset y operación en OKE.
- `AI_BOT_EXECUTIVE_SUMMARY.txt` — resumen ejecutivo del bot de IA.
- `OCI_IAM_OAUTH2_FRONTEND_INTEGRATION.txt` — integración del frontend con OCI IAM.
