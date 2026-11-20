# MagnetixDIAN

Plataforma de cumplimiento tributario DIAN con **tres módulos**:

1. **Información exógena (medios magnéticos)** — formatos **1001 v10** (pagos/abonos y retenciones) y **1002 v10** (créditos, descuentos y notas). Carga un Excel, valida contra las reglas DIAN (dígito de verificación del NIT, conceptos del catálogo, topes en UVT), genera el reporte y descarga el **XML listo para presentar en MUISCA**.
2. **Facturación electrónica (FEV v1.9)** — emisión de facturas (01), notas crédito (91) y notas débito (92) con **CUFE/CUDE** (SHA-384), **UBL 2.1**, firma **XAdES-EPES**, PDF, QR y transmisión por canal (simulado / PAC / DIAN SOAP).
3. **Integraciones y pagos** — sincronización con ERP/contabilidad/e-commerce mediante webhooks firmados y checkout de suscripciones (simulado / Wompi / MercadoPago).

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 17 · Spring Boot 3.3.5 · Flyway · Spring Security (JWT) |
| Base de datos | PostgreSQL 16 (Docker / Supabase) |
| Excel / XML | Apache POI · DOM + XSD propio |
| Firma / PDF / QR | XML Digital Signature + BouncyCastle · OpenPDF · ZXing |
| Frontend | React 18 · Vite · MUI 6.5 · React Router |
| Tests | JUnit (backend) · Vitest + Testing Library (frontend) |
| Documentación | Swagger/OpenAPI |

## Estructura (monorepo)

```
backend/    API REST + reglas de negocio + generación XML/UBL + firma + transmisión
frontend/   SPA React (Vite) con proxy /api → :8080
docker/     docker-compose (db, backend, frontend)
docs/       Manual técnico, manual de usuario, ejemplos y diagrama de arquitectura
```

## Inicio rápido (Docker Compose)

```bash
cd docker
docker compose up -d --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080 · Swagger: http://localhost:8080/swagger-ui.html
- Usuario demo: `admin` / `admin123`

## Desarrollo local

Requiere **JDK 17** (en este equipo: `C:/Users/hbarb/AppData/Local/jdk17`). El Maven global usa JDK 8, por eso se enrutan `JAVA_HOME`/-`bin` explícitamente.

```bash
# BD
cd docker && docker compose up -d db

# Backend (→ :8080)
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn spring-boot:run

# Frontend (→ :5173, proxy /api → :8080)
cd frontend && pnpm install && pnpm run dev
```

Checks previos a commit (JDK 17 obligatorio; frontend con **pnpm**, no npm):

```bash
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn clean package

cd ../frontend
pnpm run lint        # 0 errores, 0 warnings
pnpm run build       # tsc -b + vite build
pnpm run test        # Vitest
pnpm run test:e2e    # Playwright (backend + preview)
```

## Integraciones y pagos

Catálogo de proveedores (configuración por empresa en Administración → Integraciones):

| Proveedor | Slug | Credenciales (plantillas vacías) |
|---|---|---|
| SIIGO | `siigo` | `username=` `accessKey=` `url=` |
| ALEGRA | `alegra` | `email=` `apiToken=` |
| ODOO | `odoo` | `url=` `db=` `username=` `password=` `webhookSecret=` |
| WORLD_OFFICE | `world-office` | `apiToken=` `username=` `password=` `webhookSecret=` |
| HELISA | `helisa` | `usuario=` `clave=` `url=` `sign=` `empresa=` `webhookSecret=` |
| SHOPIFY | `shopify` | `shop=` `accessToken=` `webhookSecret=` |
| WOOCOMMERCE | `woocommerce` | `url=` `consumerKey=` `consumerSecret=` `webhookSecret=` |
| SAP_BUSINESS_ONE | `sap-business-one` | `url=` `companyDB=` `username=` `password=` |
| DYNAMICS_365 | `dynamics-365` | `url=` `tenantId=` `clientId=` `clientSecret=` |
| NETSUITE | `netsuite` | `accountId=` `consumerKey=` `consumerSecret=` `tokenId=` `tokenSecret=` |

Las credenciales se guardan cifradas por empresa (no son variables de entorno). El webhook
`POST /api/integraciones/webhook/{proveedor}/{token}` exige `X-Integracion-Firma` y
`X-Integracion-Timestamp` (HMAC-SHA-256 sobre `timestamp + "." + body`); sin `webhookSecret`
se rechaza en Render (`INTEGRACIONES_EXIGIR_FIRMA_WEBHOOK=true`). Eventos idempotentes con
reintentos 1→5→15→60→240 min (máx. 5).

Métodos de pago vía Wompi: `CARD`, `NEQUI`, `PSE` y `BREB_QR` (QR dinámico del PSP, no conexión
directa DICE/MOL); `DAVIPLATA` próximamente. En Render, `PAGOS_EXIGIR_FIRMA_WEBHOOK=true`.
El keep-alive externo del plan free se documenta en `docs/DEPLOY-FREE.md` (mitigación, no SLA).

## Documentación

- [`docs/MANUAL-TECNICO.md`](docs/MANUAL-TECNICO.md) — arquitectura, modelo de datos, reglas, API, despliegue.
- [`docs/MANUAL-USUARIO.md`](docs/MANUAL-USUARIO.md) — uso de la aplicación paso a paso.
- [`docs/DEPLOY-FREE.md`](docs/DEPLOY-FREE.md) — despliegue gratuito (Cloudflare Workers + Render + Supabase).
- [`docs/ejemplos/`](docs/ejemplos/) — XML de salida y respuestas JSON de la API.
- [`docs/arquitectura.png`](docs/arquitectura.png) — diagrama de arquitectura.

## Roadmap

- [x] Suite de pruebas (JUnit) para el motor de reglas y la facturación.
- [x] Soporte multiformato (1001 y 1002) y marco normativo por año.
- [x] Panel de administración (empresas, usuarios, calendario).
- [x] Facturación electrónica: emisión, CUFE, UBL, firma XAdES, PDF, QR y transmisión.
- [x] CI en GitHub Actions (build + lint + tests).
- [x] Pruebas de frontend (Vitest + Testing Library).
- [x] Paginación server-side y manejo global de errores.
- [x] Publicación automática (deploy) tras merge a `main`.
- [x] Auth endurecida: refresh tokens con rotación, reset de contraseña, invitaciones, bloqueo y rate limit.
- [x] Auditoría de eventos y cola de reintentos de transmisión con backoff.
- [x] RADIAN: eventos DIAN 030-034 (CUDE) y aceptación tácita automática.
- [x] Importación masiva (CSV/Excel) y exportación contable.
- [x] Modo oscuro y duplicado de facturas.
- [x] Planes y suscripciones con límites de uso.
- [x] Claves de API por empresa (`X-API-Key`).
- [x] Pasarelas de pago (simulada / Wompi / MercadoPago) con webhooks y checkout.
- [x] Landing pública, registro self-service y onboarding al plan gratuito.
- [x] PAC REST configurable y transmisión DIAN directa con firma WS-Security X.509.
- [x] Webhooks con verificación de firma, frescura y reconciliación de pagos.
- [x] Configuración FE validada con certificados PKCS#12 y auditoría de cambios.
- [x] Integraciones con ERP/contabilidad/e-commerce (10 proveedores), webhooks firmados y reintentos.
- [x] Métodos de pago Wompi: tarjeta, Nequi, PSE y QR dinámico Bre-B.

Ver issues del repo para el backlog detallado.
