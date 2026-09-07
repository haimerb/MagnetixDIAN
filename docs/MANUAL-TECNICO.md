# MagnetixDIAN — Manual Técnico

Sistema de generación de **medios magnéticos (información exógena)** de la DIAN, formato 1001 v10 (pagos/abonos y retenciones). MVP vertical: carga de Excel → validación → reporte → XML.

## 1. Stack

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje backend | Java (Temurin) | 17 |
| Framework backend | Spring Boot | 3.3.5 |
| ORM / migraciones | Spring Data JPA / Flyway | 10.20.1 |
| Base de datos | PostgreSQL (Docker) | 16 |
| Autenticación | JWT (jjwt) | 0.12.6 |
| Excel | Apache POI | 5.3.0 |
| XML | DOM (JDK) + XSD propio | — |
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

**Estados de `medio_magnetico`:** `BORRADOR` → `CARGADO` → `VALIDADO` | `VALIDADO_CON_ERRORES` → `XML_GENERADO` → `PRESENTADO`.

Constraint SQL importante: `validacion` no permite operaciones duplicadas por línea (`UNIQUE (medio_magnetico_id, linea)`); la recarga de Excel limpia antes (`@Modifying(flushAutomatically = true, clearAutomatically = true)`).

## 4. Formato 1001 — layout del Excel cargado

Columnas (A–N), primera fila de encabezados, datos desde la fila 2:

| # | Columna | Tipo | Regla de negocio |
|---|---|---|---|
| 1 | `TIPO_DOC` | texto (NIT, CC, NITCE) | obligatorio |
| 2 | `NUMERO_DOC` | número/texto | obligatorio |
| 3 | `DV` | número | dígito de verificación del NIT |
| 4 | `CONCEPTO` | número | debe existir en catálogo 1001 |
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

Archivo de prueba regenerable: `backend/target/ejemplo-1001.xlsx` (script en `backend/tools/GenXlsx.java`, ver §7).

## 5. Reglas de validación

Implementadas en `domain/rule/` y catalogadas en BD (V2 seed):

| Código | Regla | Severidad |
|---|---|---|
| `DV_NIT` | Dígito de verificación del NIT (módulo 11, algoritmo DIAN) | ERROR |
| `OBLIGATORIEDAD` | Campos obligatorios (tipo doc, número doc, concepto, valor pago) | ERROR |
| `CONCEPTO_VALIDO` | Concepto existe en el catálogo 1001 | ERROR |
| `CONSISTENCIA_VALORES` | Valores numéricos no negativos | ERROR |
| `TOPE_3_UVT` | Pagos menores a 3 UVT del año (aviso) | ADVERTENCIA |

Límite de reporte: 3 UVT × 49.799 (2025) = **149.397 COP**.

## 6. API REST

Base: `/api`. Swagger: `http://localhost:8080/swagger-ui.html`.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Login `{username, password}` → `{accessToken, refreshToken, username, roles}` |
| POST | `/api/auth/refresh` | Renovar token con `{refreshToken}` |
| POST | `/api/mediomagnetico/{empresaId}/cargar` | Multipart: `formato`, `anioGravable`, `archivo` (Excel) |
| POST | `/api/mediomagnetico/{id}/validar?anioGravable=` | Ejecuta validación → `ValidacionProcesoDto` |
| GET | `/api/mediomagnetico/{id}/reporte` | Última corrida de validación |
| POST | `/api/mediomagnetico/{id}/generar-xml` | Genera XML (devuelve el texto) |
| GET | `/api/mediomagnetico/{id}/xml` | Descarga el XML (attachment) |
| GET | `/api/mediomagnetico/empresa/{empresaId}` | Lista medios magnéticos de la empresa |

**Seguridad:** salvo `/api/auth/**`, `/swagger-ui/**` y `/v3/api-docs/**`, todo exige `Authorization: Bearer <jwt>`. CORS habilitado para `localhost:5173/4173`.

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

# Regenerar el Excel de prueba (requiere depdendencias de compilación, se ejecuta desde backend/)
mvn -q compile dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes;target/dependency/*" GenXlsx        # escribe target/ejemplo-1001.xlsx

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

No hay suite JUnit propia todavía (`mvn package` compila sin tests). El smoke test manual:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

curl -s http://localhost:8080/api/mediomagnetico/empresa/1 -H "Authorization: Bearer $TOKEN"
curl -s -F "formato=1001" -F "anioGravable=2025" -F "archivo=@target/ejemplo-1001.xlsx" \
  http://localhost:8080/api/mediomagnetico/1/cargar -H "Authorization: Bearer $TOKEN"
curl -s -X POST "http://localhost:8080/api/mediomagnetico/1/validar?anioGravable=2025" -H "Authorization: Bearer $TOKEN"
```

Con el ejemplo, la corrida esperada: **5 registros, 7 errores, 1 advertencia** (DV incorrectos, concepto 9999, cuantía menor a 3 UVT, valor negativo).