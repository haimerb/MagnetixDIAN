# MagnetixDIAN — Manual Técnico

## 1. Stack y estructura

| Capa | Tecnología |
| --- | --- |
| Backend | Java 17 (Temurin) · Spring Boot 3.3.5 · Flyway · Spring Security 6 |
| BD | PostgreSQL 16 (Docker local / Supabase Session pooler en producción) |
| Excel/XML | Apache POI · DOM + XSD local |
| Firma / PDF / QR | XMLDSig + BouncyCastle · OpenPDF · ZXing |
| Frontend | React 18 · Vite · MUI 6.5 · TypeScript · pnpm |
| Tests | JUnit 5 + MockMvc · Vitest · Playwright |
| CI/CD | GitHub Actions · gitleaks · Cloudflare Workers · Render |

```
backend/src/main/java/com/magnetixdian/
  config/           SecurityConfig (JWT, CORS, roles, fail-fast)
  security/         JwtAuthFilter, ApiKeyAuthFilter, AutorizacionEmpresa, RateLimitFilter
  application/      Servicio* (pagos, facturación, causación, contabilización, registro…)
  domain/rule/      ReglaValidacion + RuleEngine (reglas @Component)
  domain/dian/      ModoOperacionDian y ModoOperacionDianResolver (contratos compartidos)
  domain/pago/      PasarelaPago (puerto)
  infrastructure/   POI, JAXB/DOM, JPA, crypto/CifradorAes, pago/*
  interfaces/rest/  Controllers · interfaces/dto/ Records (nunca entidades JPA)
```

`open-in-view=false`: los controllers devuelven DTOs. Las reglas nuevas se registran como
`@Component` y el `RuleEngine` las detecta automáticamente.

## 2. Flujo completo y modos DIAN

`venta → pago → verificación → wizard DIAN → primera factura → asiento → exógena`.

| Modo | Factura a terceros | PT requerido |
| --- | --- | --- |
| `PROPIO_INTERNO` | No | No |
| `ADQUIRIDO_CLIENTE` (default) | No | No |
| `PT_MAGNETIXDIAN` | Sí | Sí (MagnetixDIAN) |
| `PT_EXTERNO` | Sí | Sí (tercero) |

`ModoOperacionDianResolver` es de **solo lectura**: determina `permiteFacturarATerceros`,
`requiereHabilitacionProveedorTecnologico` y `modoPorDefectoParaCliente`.

La exógena usa formatos 1125 v13, 2278 v1 y 5253 v2 con validación
`xsd-local-2025` (`prevalidacionOficial=false`). **No se promete prevalidación oficial.**

## 3. Seguridad HTTP

`SecurityConfig` define, en este orden:

1. `OPTIONS /**` y health → `permitAll`.
2. Swagger/OpenAPI → `permitAll`.
3. `POST /api/pagos/webhook/**` y `POST /api/integraciones/webhook/**` → `permitAll`
   (la autenticidad se verifica con HMAC en la pasarela, no con JWT).
4. `GET /api/planes` y `GET /api/branding/{publicSlug}` → `permitAll` (un solo segmento).
5. `POST /api/registro`, `/verificar`, `/reenviar` (y alias `/api/auth/registro/*`) → `permitAll`.
6. `/api/registro/invitaciones/**`, `/api/auth/me`, `/api/auth/change-password` → autenticados.
7. `/api/admin/**` → `hasRole("ADMIN")`.
8. `/api/auth/**` restante (login, refresh, forgot/reset, accept-invite) → `permitAll`.
9. `anyRequest()` → autenticado (Bearer JWT o `X-API-Key`).

CORS usa `allowedOriginPatterns` con `http://localhost:5173`, `http://localhost:4173`,
`https://magnetixdian.hbarbetti-ing.workers.dev`, `https://*.workers.dev`,
`https://magnetixdian.pages.dev` y `https://*.pages.dev`, más `CORS_ORIGINS`.
Se permiten `GET, POST, PUT, PATCH, DELETE, OPTIONS` y credenciales.

`JwtAuthFilter` y `ApiKeyAuthFilter` solo pueblan el `SecurityContext`; la autorización por
empresa se hace en cada controller con `AutorizacionEmpresa.verificar(empresaId, auth)`.

## 4. Fail-fast y cifrado

### Fail-fast

`SecurityConfig.validarConfiguracionSegura(Environment)` se ejecuta durante la creación del
contexto (`ValidadorArranqueSeguro`). Con `SEGURIDAD_FAIL_FAST=true` exige, sin reflejar
valores en logs ni excepciones:

```
JWT_SECRET, CRYPTO_KEY, CRYPTO_KEY_ID, FRONTEND_URL,
WOMPI_EVENTS_SECRET, MERCADOPAGO_WEBHOOK_SECRET
```

También rechaza secretos de ejemplo (`base64-de-*`, `change-me`, `cambiar`, la clave dev, …).
En desarrollo (`false`) se registra una advertencia y se permite la pasarela `SIMULADO`;
en producción no se cae a `SIMULADO` ni a secretos por defecto.

### Cifrado AES-GCM (`CifradorAes`)

- Clave activa: `CRYPTO_KEY` con identificador `CRYPTO_KEY_ID`.
- Claves anteriores: `CRYPTO_EXTRA_KEYS=id1=secreto1,id2=secreto2` (también acepta `:`).
- `cifrar` → `enc:v2:{keyId}:{base64(iv||ciphertext+tag)}` con IV aleatorio de 12 bytes y tag 128.
- `descifrar` soporta `enc:` (legacy: prueba activa + claves extra) y `enc:v2:` (selecciona por id).
- Cualquier manipulación del sobre (GCM tag) lanza `IllegalStateException`.
- No se registran plaintext, claves ni identificadores de clave; `log.warn` es genérico.
- Rotación: conservar la clave anterior en `CRYPTO_EXTRA_KEYS` mientras existan datos `enc:`
  o `enc:v2` con ese id; escribir con la nueva clave migrará progresivamente.

Tests: `backend/src/test/java/com/magnetixdian/seguridad/`
(`CifradorAesTest`, `ConfiguracionSeguraTest`, `SecurityConfigRulesTest`).

## 5. API REST (principales)

| Área | Endpoints |
| --- | --- |
| Medios magnéticos | Bajo `/api/mediomagnetico`: `GET /formatos`, `/plantilla`, `/empresa/{id}[/paginado]`; `POST /{empresaId}/cargar`, `/{empresaId}/importar-multiformato`; `POST /{medioMagneticoId}/validar`, `/validar-datos-json`, `/generar-xml`, `/presentar`, `/aplicar-dv`; `GET /{medioMagneticoId}/reporte`, `/terceros`, `/xml` |
| Facturación | `/api/empresas/{id}/facturas/*`, `POST /factura-rapida`, entrega WhatsApp |
| RADIAN | `/api/empresas/{id}/radian/documentos` (importar/listar/causar/contabilizar) |
| Contabilidad | `/api/empresas/{id}/contabilidad/asientos`, `/conciliacion`, `/impuestos/*`, `/exogena/*` |
| Pagos | `POST /api/empresas/{id}/pagos/checkout`, `GET /pagos/{referencia}`, `POST /api/pagos/webhook/{proveedor}` |
| Suscripciones | `GET /api/planes`, `/api/empresas/{id}/suscripcion` |
| Registro | `POST /api/registro`, `/verificar`, `/reenviar`; invitaciones protegidas |
| Integraciones | `/api/empresas/{id}/integraciones/*`, `POST /api/integraciones/webhook/{proveedor}/{token}` |
| Admin | `/api/admin/**` (solo `ROLE_ADMIN`): empresas, usuarios, auditoría, transmisión, calendario |
| Branding/PWA | `GET /api/branding/{publicSlug}` (público), `GET|PUT /api/empresas/{id}/branding` |
| Health | `GET /actuator/health` |

Webhooks: firma HMAC-SHA-256 sobre `timestamp + "." + body`, tolerancia
`PAGOS_WEBHOOK_TOLERANCE_SECONDS=300`, ids de evento idempotentes. En Render,
`PAGOS_EXIGIR_FIRMA_WEBHOOK=true` e `INTEGRACIONES_EXIGIR_FIRMA_WEBHOOK=true`.

Métodos Wompi: `BREB_QR` y `DAVIPLATA` usan `POST /v1/transactions` con
`acceptance_token` de `GET /merchants/info`, `signature` y `aceptaTerminos=true`.
`BREB_QR` hace polling de `GET /v1/transactions/{id}` hasta `qr_image`/`qr_id` en
`payment_method` o `payment_method.extra`; la imagen se entrega en `qrImage` y la referencia
persistida es `wompi-qr:{qrId}`.
Daviplata redirige a `payment_method.extra.url` (OTP alojado por Wompi). El
`payment_description` de Daviplata se recorta a 30 caracteres y el de QR a 64.

## 6. Base de datos y migraciones

Flyway V1–V33. V1 esquema, V2 UVT/reglas/calendario, V3 admin + empresa demo,
V17 datos JSON, V18 RADIAN, V19 contabilidad, V20 migración contable, V21 mapeo,
V22 branding, V23 seguridad/auditoría, V24–V25 núcleo contable e impuestos,
V26 billing/comercial, V27 onboarding, V28 ERP transaccional, V29 motor tributario/exógena,
V30 núcleo contable oficial, V31 firma de webhook billing, V32 asientos pendientes,
V33 activos fijos y depreciación.

```bash
cd docker && docker compose up -d db
# Reset total si cambia una migración ya aplicada:
docker compose down -v && docker compose up -d db
```

Demo: `admin` / `admin123`, empresa MAGNETIXDIAN SAS (NIT 900123456, id=1).
Si `mvn clean` borra `backend/target/ejemplo-1001.xlsx`, regenerarlo desde
`backend/tools/GenXlsx.java` con JDK 17 (ver `AGENTS.md`).

## 7. Desarrollo y quality gates

```bash
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17" && export PATH="$JAVA_HOME/bin:$PATH"
cd backend && MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn spring-boot:run
MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn clean package

cd ../frontend
pnpm install --frozen-lockfile
pnpm run lint -- --max-warnings=0
pnpm run build          # tsc -b + vite build
pnpm run test           # Vitest
pnpm run test:e2e       # Playwright (requiere backend + preview)
```

Reglas: Grid2 desde `@mui/material/Grid2`, colores del tema (no hardcoded), sin `key={index}`,
sin `useEffect` para derivaciones (usar `useMemo`), sin `console.log`, `pnpm` (nunca npm).

CI: `backend`, `frontend`, `e2e` y `seguridad` son requeridos, más `required-checks`.
`seguridad` ejecuta `git diff --check`, gitleaks con `--redact` y `mvn -q clean package`.
No se imprimen secretos en logs.

## 8. Despliegue

Ver [`DEPLOY-FREE.md`](DEPLOY-FREE.md). Resumen:

- **BD:** Supabase con Session pooler IPv4; `baseline-on-migrate: true`.
- **Backend:** Render, `render.yaml`, `SEGURIDAD_FAIL_FAST=true`, healthcheck `/actuator/health`.
- **Frontend:** Cloudflare Worker; `worker.js` proxya `/api`, fija cabeceras PWA e inyecta
  `SW_VERSION` (SHA del commit) en `frontend/public/sw.js` para invalidar su caché.
- **Deploy:** `deploy.yml` falla si faltan secretos o si el backend no devuelve `UP`.
- Keep-alive del plan free (cron del worker + GitHub Action): mitigación, no SLA.

## 9. Variables de entorno

| Grupo | Variables |
| --- | --- |
| BD | `DB_URL`, `DB_USER`, `DB_PASSWORD` |
| Seguridad | `JWT_SECRET`, `JWT_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `CRYPTO_KEY`, `CRYPTO_KEY_ID`, `CRYPTO_EXTRA_KEYS`, `SEGURIDAD_FAIL_FAST`, `AUTH_EXPOSE_TOKEN`, `CORS_ORIGINS`, `FRONTEND_URL` |
| Pagos | `WOMPI_PUBLIC_KEY`, `WOMPI_INTEGRITY_SECRET`, `WOMPI_EVENTS_SECRET`, `WOMPI_API_TOKEN`, `WOMPI_CHECKOUT_URL`, `WOMPI_API_URL`, `WOMPI_QR_POLL_ATTEMPTS`, `WOMPI_QR_POLL_INTERVAL_MS`, `WOMPI_BREB_QR_SANDBOX_STATUS`, `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET`, `PAGOS_EXIGIR_FIRMA_WEBHOOK`, `PAGOS_WEBHOOK_TOLERANCE_SECONDS` |
| Integraciones | `INTEGRACIONES_EXIGIR_FIRMA_WEBHOOK`, timeouts y reintentos |
| DIAN | `DIAN_URL_HABILITACION`, `DIAN_URL_PRODUCCION`, timeouts, `DIAN_CATALOGO_*` |
| Correo/PWA | `MAIL_*`, `BRANDING_*`, `WHATSAPP_*`, `NOTIFICATIONS_ENABLED` |
| Observabilidad | `SENTRY_DSN`, `SENTRY_ENVIRONMENT`, `SENTRY_TRACES_SAMPLE_RATE` |
| Frontend | `VITE_API_BASE_URL`, `VITE_SENTRY_DSN` |

Los tokens DIAN, certificados y credenciales de integraciones **no** son variables de entorno:
se guardan cifrados por empresa. Plantilla: [`.env.example`](../.env.example).

### Desbloqueo de emisión (estado `PRODUCCION`)

1. Guardar modo, resolución, prefijo, rangos, `softwareId` y `testSetId` desde el asistente.
2. Enviar el TEST SET con `POST /api/facturacion/config/{empresaId}/test-set`: 10 XML
   (8 facturas, 1 nota débito y 1 nota crédito, máximo 5 MB cada uno).
3. Consultar `GET /api/facturacion/config/{empresaId}/estado?trackId=...` hasta recibir
   `TEST_SET_APROBADO`.
4. Ejecutar `PATCH /api/facturacion/config/{empresaId}/switchover?trackId=...` con
   `confirmacion=APRUEBO_SWITCHOVER`, `testSetId` y `resolucionNumero`.
5. Verificar en `evento_auditoria` los eventos `TEST_SET_APROBADO` y
   `SWITCHOVER_PRODUCCION`. Un `CAMBIO_PRODUCCION_BLOQUEADO` indica que faltó alguno.
6. Con `SEGURIDAD_FAIL_FAST=true`, producción exige certificado DIAN, canales reales y WSDL
   oficiales; el canal `SIMULADO` se rechaza.

`estadoPermiteEmitir` solo acepta `PRODUCCION`; `TEST_SET_APROBADO` habilita el switchover,
no la emisión.

## 10. Checklist de habilitación PT

1. Resolución vigente, prefijo y rangos aprobados.
2. `softwareId` y `testSetId` en entorno de habilitación.
3. Certificado PKCS#12 por empresa, cifrado y rotado.
4. Pruebas de emisión y notificación al adquirente.
5. Plan con facturación a terceros y modo `PT_*` configurado.
6. Secretos de pago y firma de webhooks activos antes de cobrar.
7. Exógena validada localmente (XSD); presentación ante la DIAN la hace el usuario.
