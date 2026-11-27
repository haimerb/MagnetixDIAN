# MagnetixDIAN

ERP contable colombiano con **flujo completo de venta a cumplimiento**: venta → pago →
verificación → asistente (wizard) DIAN → primera factura → asiento contable → exógena.
Incluye facturación electrónica, RADIAN, contabilidad, impuestos, integraciones y PWA.

> Costo cero: el producto se ejecuta con el plan gratuito de **Cloudflare Workers** (frontend),
> **Render** (backend) y **Supabase** (PostgreSQL). No se requiere licencia de un proveedor
> tecnológico (PT) para usar el modo propio o adquirido. El modo **PT MagnetixDIAN** requiere
> habilitación ante la DIAN; el costo de esa habilitación depende de la DIAN, no del software.

## Flujo completo (de la venta a la exógena)

| Paso | Acción en la app | Resultado |
| --- | --- | --- |
| 1. Venta | Registro self-service o alta por administrador; selección de plan | Empresa + usuario administrador |
| 2. Pago | Checkout Wompi (`CARD`, `NEQUI`, `PSE`, `BREB_QR`, `DAVIPLATA`) o MercadoPago (simulado solo en desarrollo) | Evento firmado en `POST /api/pagos/webhook/{proveedor}` |
| 3. Verificación | Verificación del pago y del correo; token de un solo uso | Suscripción activa y acceso habilitado |
| 4. Wizard DIAN | Selección de modo DIAN, resolución, prefijo, rango, *test set* y notificación al adquirente | Configuración FE de la empresa |
| 5. Primera factura | Emisión FEV v1.9, firma, CUFE/CUDE, PDF/QR y entrega | Factura emitida y entregada (WhatsApp Click-to-Chat o correo) |
| 6. Asiento | Causación RADIAN o asiento manual, con validación débito=crédito | Asiento contable conciliado |
| 7. Exógena | Generación de formatos oficiales activos y validación **XSD local** | Archivo listo para presentar en la DIAN |

La validación de exógena es **local con XSD** (`xsd-local-2025`, `prevalidacionOficial=false`).
**No** se promete prevalidación oficial ni presentación automática ante la DIAN.

## Métodos de pago Wompi

`BREB_QR` y `DAVIPLATA` se crean con `POST /v1/transactions` y tokens de aceptación de Wompi;
ambos exigen `aceptaTerminos=true`. Daviplata requiere además `documentType`,
`documentNumber` y redirige a la URL OTP alojada por Wompi en `payment_method.extra.url`.

- `BREB_QR`: el backend hace *long polling* de `GET /v1/transactions/{id}` hasta obtener
  `payment_method.qr_image`/`qr_id` o `payment_method.extra.qr_image`/`qr_id`. La imagen base64
  se devuelve al frontend en `qrImage`; en BD se guarda la referencia compacta `wompi-qr:{qrId}`.
- Configuración: `WOMPI_API_TOKEN`, `WOMPI_QR_POLL_ATTEMPTS` (10),
  `WOMPI_QR_POLL_INTERVAL_MS` (500) y `WOMPI_BREB_QR_SANDBOX_STATUS` (`APPROVED`; solo sandbox).
- El estado final se confirma por webhook firmado o por consulta manual al endpoint de pago.

## Modos de operación DIAN

| Modo | Cuándo usarlo | Factura a terceros | Requiere habilitación PT |
| --- | --- | --- | --- |
| `PROPIO_INTERNO` | Pruebas internas / tenant propio | No | No |
| `ADQUIRIDO_CLIENTE` | El cliente ya adquirió su propio software | No | No (default para clientes) |
| `PT_MAGNETIXDIAN` | MagnetixDIAN actúa como proveedor tecnológico | Sí | Sí |
| `PT_EXTERNO` | El cliente usa un PT de terceros | Sí | Sí (del tercero) |

La resolución la aplica `ModoOperacionDianResolver` (contrato compartido, solo lectura).

## Checklist de habilitación como PT

1. Resolución de habilitación vigente y número prefijo/rango autorizados por la DIAN.
2. `softwareId` y `testSetId` aprobados en el entorno de habilitación.
3. Certificado PKCS#12 vigente configurado por empresa (nunca en variables de entorno).
4. Pruebas de emisión en habilitación y definición de notificación al adquirente.
5. `DIAN_URL_HABILITACION` / `DIAN_URL_PRODUCCION` y timeouts configurados.
6. Verificar que el plan contratado permita facturar a terceros (`PT_*`).

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 17 · Spring Boot 3.3.5 · Flyway · Spring Security (JWT) |
| BD | PostgreSQL 16 (Docker local / Supabase en producción) |
| Excel/XML | Apache POI · DOM + XSD local |
| Firma/PDF/QR | XML Digital Signature + BouncyCastle · OpenPDF · ZXing |
| Frontend | React 18 · Vite · MUI 6.5 · TypeScript |
| Tests | JUnit + MockMvc · Vitest · Playwright |
| CI/CD | GitHub Actions · gitleaks · Cloudflare Workers · Render |

## Estructura

```
backend/    API REST, reglas, generación XML/UBL, firma, transmisión, pagos y cifrado
frontend/   SPA React (Vite), proxy /api → :8080, PWA
docker/     docker-compose (BD local)
docs/       Manual técnico, manual de usuario, guía de deploy y ejemplos
worker.js   Proxy /api + cabeceras PWA + invalidación de caché del service worker
```

## Inicio rápido

```bash
cd docker && docker compose up -d --build
# Frontend http://localhost:5173 · Backend http://localhost:8080 · Swagger /swagger-ui.html
# Demo: admin / admin123
```

Desarrollo sin Compose:

```bash
cd docker && docker compose up -d db
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17" && export PATH="$JAVA_HOME/bin:$PATH"
cd backend && MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn spring-boot:run
cd ../frontend && pnpm install --frozen-lockfile && pnpm run dev
```

Verificación completa (orden obligatorio):

```bash
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17" && export PATH="$JAVA_HOME/bin:$PATH"
cd backend && MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn clean package
cd ../frontend && pnpm install --frozen-lockfile && pnpm run lint -- --max-warnings=0 && pnpm run build
```

## Seguridad y variables de entorno

- `SEGURIDAD_FAIL_FAST=true` en producción: el backend **no arranca** si faltan
  `JWT_SECRET`, `CRYPTO_KEY`, `CRYPTO_KEY_ID`, `FRONTEND_URL`, `WOMPI_EVENTS_SECRET` o
  `MERCADOPAGO_WEBHOOK_SECRET`, ni si se usan secretos de ejemplo. En desarrollo se permite
  la pasarela `SIMULADO` con advertencia en logs (sin secretos).
- Cifrado AES-GCM con rotación: `CRYPTO_KEY`, `CRYPTO_KEY_ID`, `CRYPTO_EXTRA_KEYS`;
  formatos `enc:` (legacy) y `enc:v2:{keyId}:{base64}`.
- Endpoints públicos: `POST /api/pagos/webhook/**`, `POST /api/registro`,
  `/api/registro/verificar`, `/api/registro/reenviar`, `GET /api/planes`,
  `GET /api/branding/{publicSlug}`, health y auth. Invitaciones y `/api/admin/**` protegidas.
- CORS: orígenes `*.workers.dev` y `*.pages.dev` además de localhost (configurable con
  `CORS_ORIGINS`). Ver `.env.example` para la lista completa.

## Quality gates

`backend`, `frontend`, `e2e` y `seguridad` son checks requeridos en GitHub Actions
(`git diff --check`, gitleaks con salida redactada, ESLint con 0 warnings, build y tests).
El deploy falla si faltan secretos; nunca publica verde.

## Documentación

- [`docs/MANUAL-USUARIO.md`](docs/MANUAL-USUARIO.md) — flujo completo paso a paso.
- [`docs/MANUAL-TECNICO.md`](docs/MANUAL-TECNICO.md) — arquitectura, API, configuración y seguridad.
- [`docs/DEPLOY-FREE.md`](docs/DEPLOY-FREE.md) — deploy gratuito y checklist de producción.
- [`docs/ejemplos/`](docs/ejemplos/) · [`docs/arquitectura.png`](docs/arquitectura.png)
