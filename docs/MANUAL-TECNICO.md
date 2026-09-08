# MagnetixDIAN — Manual Técnico

Sistema de generación de **medios magnéticos (información exógena)** de la DIAN, formatos **1001 v10** (pagos/abonos y retenciones) y **1002 v10** (créditos, descuentos y notas). Flujo: carga de Excel → validación → reporte → XML → marcar presentado + normalización de terceros.

## 1. Stack

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje backend | Java (Temurin) | 17 |
| Framework backend | Spring Boot | 3.3.5 |
| ORM / migraciones | Spring Data JPA / Flyway | 10.20.1 |
| Base de datos | PostgreSQL (Docker) | 16 |
| Autenticación | JWT (jjwt) | 0.12.6 |
| Excel | Apache POI | 5.3.0 |
| XML | DOM (JDK) + XSD propio (1001 y 1002) | — |
| Health | Spring Boot Actuator | 3.3.5 |
| Swagger | springdoc-openapi | 2.6.0 |
| Frontend | React / Vite / MUI | 18 / 5 / 6.5 |

**Requerimiento de JDK 17:** el Maven global del sistema usa JDK 8. Todos los builds exigenn la variable de entorno:

```bash
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
```

## 2. Arquitectura del código

```
backend/src/main/java/com/magnetixdian/
  interfaces/rest/     Controllers REST (MedioMagneticoController, AuthController)
  interfaces/dto/      Records de respuesta (NUNCA entidades JPA — open-in-view=false)
  application/         Servicios de caso de uso (AuthService, JwtService, ServicioMedioMagnetico, ServicioValidacion)
  domain/              ReglaValidacion (nterfaz), RuleEngine, reglas, CatalogoConceptos, UvtManager
  infrastructure/      ImportadorExcel (POI), GeneradorXml1001 (DOM), persistencia JPA, notificación
  security/            JwtAuthFilter, CustomUserDetailsService, JwtService
  config/              SecurityConfig, WebConfig
```

**Principios:**

- **Reglas extensibles:** toda regla de negocio es un bean `@Component` que implementa `ReglaValidacion`. `RuleEngine` las detecta automáticamente por Spring y las ejecuta en orden. Nueva regla = nueva clase, **nunca** tocar el engine.
- **DTOs en el contrato API:** las respuestas serializan records, no entidades (evita proxys LAZY con `open-in-view: false`).
- **Java 17 estricto:** no usar features de Java 21 (ej. pattern matching en `switch`).
- **Filas de detalle:** `ValidacionJpa` tiene `@OneToMany(cascade = CascadeType.ALL)` para persistir `DetalleErrorJpa`.

## 3. Modelo de datos

Migraciones Flyway en `backend/src/main/resources/db/migration/`:

| Tabla | Propósito |
|---|---|
| `role` / `usuario` / `usuario_role` | Seguridad y roles (ADMIN, OPERADOR) |
| `empresa` | Cliente/dedarante (NIT, régimen, gran contribuyente) |
| `tercero` | Beneficiarios de pagos (normalizados por NIT) |
| `medio_magnetico` | Encabezado de reporte (formato, año gravable, estado) |
| `operacion` | Detalle formato 1001 (pagos/abonos, retenciones, IVA) |
| `uvt_vigencia` | Valor UVT por año (2024: 47.065; 2025: 49.799; 2026: 52.374) |
| `regla` | Catálogo de reglas (código, severidad, mensaje) |
| `validacion` / `detalle_error` | Corridas de validación y errores detectados |
| `calendario_dian` | Fechas límite por rol NIT para avisos |
| `notificacion` | Jobs de recordatorio de vencimiento |

**Estados de `medio_magnetico`:** `BORRADOR` → `CARGADO` → `VALIDADO` | `VALIDADO_CON_ERRORES` → `XML_GENERADO` → `PRESENTADO`. Solo se permite `PRESENTADO` desde `VALIDADO`/`VALIDADO_CON_ERRORES`/`XML_GENERADO`.

Constraint SQL importante: `validacion` no permite operaciones duplicadas por línea (`UNIQUE (medio_magnetico_id, linea)`); la recarga de Excel limpia antes (`@Modifying(flushAutomatically = true, clearAutomatically = true)`).

## 4. Formatos — layout del Excel cargado

Columnas (A–N), primera fila de encabezados, datos desde la fila 2. **Los formatos 1001 y 1002 comparten layout** (14 columnas); cambia el catálogo de conceptos y la interpretación de `VALOR_PAGO`:

| # | Columna | Tipo | Regla de negocio |
|---|---|---|---|
| 1 | `TIPO_DOC` | texto (NIT, CC, NITCE) | obligatorio |
| 2 | `NUMERO_DOC` | número/texto | obligatorio |
| 3 | `DV` | número | dígito de verificación del NIT |
| 4 | `CONCEPTO` | número | debe existir en catálogo (1001 o 1002) |
| 5 | `VALOR_PAGO` | número | obligatorio |
| 6 | `RET_RENTA` | número | ≥ 0 |
| 7 | `RET_IVA` | número | ≥ 0 |
| 8 | `RET_ICA` | número | ≥ 0 |
| 9 | `RET_TIMBRE` | número | ≥ 0 |
| 10 | `IVA_PAGADO` | número | ≥ 0 |
| 11 | `IVA_DESC` | número | ≥ 0 |
| 12 | `GASTO` | número | ≥ 0 |
| 13 | `COSTO` | número | ≥ 0 |
| 14 | `NOTA_CREDITO` | número | ≥ 0 |

La **plantilla editable** se descarga desde la app (`GET /api/mediomagnetico/plantilla?formato=`), con filas de ejemplo que el importador omite.

Archivos de prueba regenerables desde `backend/tools/`:
- `GenXlsx.java` → `target/ejemplo-1001.xlsx` (5 registros con **7 errores / 1 advertencia**).
- `GenXlsxLimpio.java` → `target/ejemplo-1001-limpio.xlsx` (5 registros **sin errores** → genera XML válido).
- `GenXlsx1002.java` → `target/ejemplo-1002.xlsx` (3 registros 1002 **sin errores**).

Ver §7 para el procedimiento de compilación de los generadores.

## 5. Catálogo de conceptos

`CatalogoConceptos` mantiene listas por formato:

| Formato | Conceptos de ejemplo |
|---|---|
| **1001** | 2004 servicios, 2005 honorarios, 2007 comisiones, 2008 arrendamiento, 2009 intereses, 2012 compras, 2018 salarios, 2025 dividendos, 2036 exterior, 2050 salud, 2071 regalías, 2073 construcción, 2080 prestaciones, 2086 servicios públicos, 2090 vigilancia, 2091 técnicos |
| **1002** | 7001 devoluciones ventas, 7002 descuentos otorgados, 7003 rebajas, 7004 notas crédito, 7005 devoluciones compras, 7006 descuentos recibidos, 7007 notas débito, 7008 diferencias de cambio |

Implementadas en `domain/rule/` y catalogadas en BD (V2 seed):

| Código | Regla | Severidad |
|---|---|---|
| `DV_NIT` | Dígito de verificación del NIT (módulo 11, algoritmo DIAN) | ERROR |
| `OBLIGATORIEDAD` | Campos obligatorios (tipo doc, número doc, concepto, valor pago) | ERROR |
| `CONCEPTO_VALIDO` | Concepto existe en el catálogo del formato | ERROR |
| `CONSISTENCIA_VALORES` | Valores numéricos no negativos y retenciones ≤ pago | ERROR |
| `TOPE_3_UVT` | Pagos menores a 3 UVT del año (aviso) | ADVERTENCIA |

Límite de reporte: 3 UVT × 49.799 (2025) = **149.397 COP**.

## 6. API REST

Base: `/api`. Swagger: `http://localhost:8080/swagger-ui.html`.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Login `{username, password}` → `{accessToken, refreshToken, username, roles}` |
| POST | `/api/auth/refresh` | Renovar token con `{refreshToken}` |
| GET | `/api/mediomagnetico/plantilla?formato=` | Descarga la plantilla Excel (1001/1002) |
| POST | `/api/mediomagnetico/{empresaId}/cargar` | Multipart: `formato`, `anioGravable`, `archivo` (Excel) |
| POST | `/api/mediomagnetico/{id}/validar?anioGravable=` | Ejecuta validación → `ValidacionProcesoDto` |
| GET | `/api/mediomagnetico/{id}/reporte` | Última corrida de validación |
| POST | `/api/mediomagnetico/{id}/generar-xml` | Genera XML (1001 o 1002 según el formato) |
| GET | `/api/mediomagnetico/{id}/xml` | Descarga el XML (attachment) |
| POST | `/api/mediomagnetico/{id}/presentar` | Marca como presentado (solo si validado) |
| GET | `/api/mediomagnetico/{id}/terceros` | Resumen de terceros + sugerencias de DV |
| POST | `/api/mediomagnetico/{id}/aplicar-dv` | Aplica los DV correctos a los NIT |
| GET | `/api/mediomagnetico/empresa/{empresaId}` | Lista medios magnéticos de la empresa |
| GET | `/api/admin/empresas` · POST · PUT · DELETE | CRUD de empresas (rol ADMIN) |
| GET | `/api/admin/usuarios` · POST · PUT estado | CRUD de usuarios (rol ADMIN) |
| GET | `/api/admin/calendario` · POST · DELETE | Calendario DIAN (rol ADMIN) |
| GET | `/actuator/health` · `/actuator/health/liveness` · `/actuator/health/readiness` | Health checks (sin auth) |

**Seguridad:** salvo `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**` y `/actuator/health/**`, todo exige `Authorization: Bearer <jwt>`. `/api/admin/**` exige además `ROLE_ADMIN`. CORS habilitado para `localhost:5173/4173`.

**Respuestas de error** (`GlobalExceptionHandler`): `400` argumentos inválidos, `409` estado inconsistente (ej. generar XML sin validar/corrección), `404` no encontrado, JSON limpio.

## 7. Tareas de desarrollo

```bash
# Levantar BD (contenedor magnetixdian-db)
cd docker && docker compose up -d db

# Backend: correr en :8080
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn spring-boot:run

# Backend: build con tests
MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn clean package

# Regenerar los Excel de prueba (requiere dependencias de compilación, se ejecuta desde backend/)
mvn -q compile dependency:copy-dependencies -DincludeScope=runtime
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp "target/dependency/*" -d "target/generated" \
  tools/GenXlsx.java tools/GenXlsxLimpio.java tools/GenXlsx1002.java
"$JAVA_HOME/bin/java" -cp "target/classes;target/dependency/*;target/generated" GenXlsx
"$JAVA_HOME/bin/java" -cp "target/classes;target/dependency/*;target/generated" GenXlsxLimpio
"$JAVA_HOME/bin/java" -cp "target/classes;target/dependency/*;target/generated" GenXlsx1002

# Matar el backend si quedó corriendo
netstat -ano | grep :8080 && taskkill //PID <pid> //F
```

**Cambios a migraciones:** si se modifican `V1`/`V2`/`V3`, recrear el volumen:

```bash
docker compose down -v && docker compose up -d db
```

## 8. Despliegue

### Docker Compose (desarrollo/preproducción)

```bash
cd docker && docker compose up -d --build
```

- `db` → `localhost:5432` / `magnetixdian`
- `backend` → `localhost:8080` (env vars via compose; volumen `uploads`)
- `frontend` (Nginx) → `localhost:5173`, proxya `/api` a `backend:8080`

### Kubernetes

Manifiestos en `k8s/` (namespace `magnetixdian`):

```bash
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/db.yaml
kubectl apply -f k8s/backend.yaml
kubectl apply -f k8s/frontend.yaml
```

El `Deployment` de backend usa `spring.config` con las mismas variables env que Composer; el secreto `magnetixdian-jwt` provee `JWT_SECRET`.

## 9. Variables de entorno

| Variable | Default | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/magnetixdian?characterEncoding=UTF-8` | JDBC |
| `DB_USER` / `DB_PASSWORD` | `magnetixdian` / `magnetixdian` | credenciales BD |
| `JWT_SECRET` | valor base64 interno | firma JWT (cambiar en producción) |
| `JWT_EXPIRATION_MS` | `86400000` | vigencia access token |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | vigencia refresh token |
| `UPLOAD_DIR` | `./uploads` | archivos subidos |
| `NOTIFICATIONS_ENABLED` | `false` | envío de recordatorios |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail | SMTP |
| `REMINDER_CRON` | `0 0 9 * * *` | cron recordatorios |
| `GENERATOR_CRON` | `0 0 2 1 1 *` | cron generador programado |

## 10. Notificaciones y jobs

- `SchedulerJobs` consulta el calendario DIAN y, a 15/7/1 días del vencimiento, crea `notificacion` y envía correo si `NOTIFICATIONS_ENABLED=true`.
- `ServicioNotificaciones` + `NotificadorEmail` (SMTP) separan la lógica del transporte.

## 11. Pruebas y smoke test

Suite JUnit en `backend/src/test/java/` (rule engine + importador Excel). `mvn package` los ejecuta (BUILD SUCCESS requiere 0 fallos).

```bash
# Backend con tests
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn clean package
```

### CI (GitHub Actions)

`.github/workflows/ci.yml` ejecuta en cada push/PR a `main`/`develop`:
- **backend:** `mvn clean package` (JDK 17, cache Maven).
- **frontend:** `npm ci && npm run lint && npm run build`.

### Smoke test manual

Requerimientos: BD levantada (`docker compose up -d db`), backend en :8080, el excel de prueba generado.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

# 1) Cargar 1001 con errores y validar → esperado: 5 registros, 7 err, 1 adv
curl -s -F "formato=1001" -F "anioGravable=2025" -F "archivo=@target/ejemplo-1001.xlsx" \
  http://localhost:8080/api/mediomagnetico/1/cargar -H "Authorization: Bearer $TOKEN"
curl -s -X POST "http://localhost:8080/api/mediomagnetico/1/validar?anioGravable=2025" -H "Authorization: Bearer $TOKEN"

# 2) Normalización de DV (terceros) → sugerencias y aplicar correcciones
curl -s http://localhost:8080/api/mediomagnetico/1/terceros -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/mediomagnetico/1/aplicar-dv -H "Authorization: Bearer $TOKEN"

# 3) Cargar 1001 LIMPIO → validar (0 errores) → XML → marcar presentado
curl -s -F "formato=1001" -F "anioGravable=2025" -F "archivo=@target/ejemplo-1001-limpio.xlsx" \
  http://localhost:8080/api/mediomagnetico/1/cargar -H "Authorization: Bearer $TOKEN"
curl -s -X POST "http://localhost:8080/api/mediomagnetico/1/validar?anioGravable=2025" -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/mediomagnetico/1/generar-xml -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/mediomagnetico/1/presentar -H "Authorization: Bearer $TOKEN"

# 4) Formato 1002 (IMPORTANTE: empresa/formato/año distinto para no chocar con 1001)
curl -s -X POST http://localhost:8080/api/mediomagnetico/2/cargar \
  -F "formato=1002" -F "anioGravable=2025" -F "archivo=@target/ejemplo-1002.xlsx" \
  -H "Authorization: Bearer $TOKEN"

# 5) Administración (rol ADMIN)
curl -s http://localhost:8080/api/admin/empresas -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/admin/calendario?anioGravable=2025 -H "Authorization: Bearer $TOKEN"

# 6) Health (sin auth)
curl -s http://localhost:8080/actuator/health
```

Para el flujo 1002 end-to-end: crear una empresa nueva por admin, luego cargar + validar + generar XML; el generador XML 1002 valida contra `medio-magnetico-1002.xsd`.

Ejemplos de XML de referencia (golden): `docs/ejemplos/medio-magnetico-1001-golden.xml`, `medio-magnetico-1002-golden.xml`, `medio-magnetico-1001-ejemplo.xml` (con errores).