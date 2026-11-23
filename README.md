# MagnetixDIAN

ERP contable centrado en la DIAN, organizado en **cuatro áreas**:

1. **Inicio** — estado del cumplimiento y accesos a los reportes exógenos clásicos.
2. **Facturación** — emisión FEV v1.9 (facturas y notas), validación, reportes, factura rápida y entrega por WhatsApp Click-to-Chat.
3. **Contabilidad** — **Asentar facturación** y el núcleo `v0.4.0`: traiga documentos de la DIAN por **token o archivo Excel/CSV/JSON/ZIP**, cáuselos, aséntelos y consulte PUC, periodos, asientos, libros, balances, impuestos, exógena y conciliación.
4. **Empresa** — usuarios, identidad visual, integraciones, suscripción y administración.

La información exógena mantiene los formatos clásicos **1001 v10** y **1002 v10** y añade los formatos oficiales activos **1125 v13**, **2278 v1** y **5253 v2**, con generador XML y validación XSD local (no prevalidador oficial).

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
- Flyway aplica automáticamente las migraciones **V1–V25** (V18–V23: RADIAN, contabilidad, migración contable, mapeo de archivos, branding y seguridad; V24–V25: núcleo contable base y módulo de impuestos).

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
pnpm exec playwright test --list
pnpm run test:e2e    # Playwright (backend + preview)
```

## Asentar facturación

Desde **Contabilidad → Asentar facturación** se traen documentos de la DIAN de dos formas:

- `POST /api/empresas/{empresaId}/dian/documentos/token`: envía el token/enlace DIAN, rango de fechas y filtros opcionales. El token se cifra en BD y nunca se devuelve ni se registra en logs.
- `POST /api/empresas/{empresaId}/dian/documentos/archivo`: recibe un Excel/CSV/JSON/ZIP exportado por la DIAN.

Variables de configuración del catálogo DIAN: `DIAN_CATALOGO_BASE_URL`, `DIAN_CATALOGO_TOKEN_URL`, `DIAN_CATALOGO_LISTADO_URL`, `DIAN_CATALOGO_HOSTS_PERMITIDOS`, `DIAN_CATALOGO_CONNECT_TIMEOUT_MS`, `DIAN_CATALOGO_READ_TIMEOUT_MS` y `DIAN_CATALOGO_MAX_BYTES`. El token de cada empresa **no** es una variable de entorno.

Los documentos traídos se causan y asientan en la misma pantalla; los asientos alimentan la conciliación y la información exógena.

## Núcleo contable, impuestos y exógena (`v0.4.0`)

`Contabilidad → Asientos contables` (`/app/contabilidad`) expone ocho pestañas: `PUC`, `Periodos`, `Asientos`, `Libros`, `Balances`, `Impuestos`, `Exógena` y `Conciliación`.

- **PUC y periodos:** el plan de cuentas 2025 se copia automáticamente a la empresa al contabilizar. No hay endpoints REST de consulta de PUC/periodos: la UI muestra estados vacíos honestos en lugar de datos falsos.
- **Asientos:** creación manual/de ajuste, reversión, cierre de periodo, numeración `AS-{empresa}-{YYYYMM}-{0001}` y validación de balance débito=crédito.
- **Libros y balances:** libro diario, libro mayor, auxiliares, balance de comprobación, balance general, estado de resultados y flujo de efectivo.
- **Impuestos:** tarifas vigentes por año, responsabilidades fiscales, causación, pagos, anulación, certificados, declaraciones en borrador y calendario tributario.
- **Exógena oficial:** formatos **1125**, **2278** y **5253** con `generar`, `conceptos` y `validar-xml`; la validación es `xsd-local-2025` (`prevalidacionOficial=false`).
- **Conciliación:** compara documentos DIAN con asientos por rango de fechas, dirección y tipo de documento.

Endpoints principales: `/api/empresas/{empresaId}/contabilidad/{asientos,reportes/*,conciliacion}`, `/api/empresas/{empresaId}/impuestos/*` y `/api/empresas/{empresaId}/exogena/{formatos,formatos/{formato}/conceptos,{formato}/generar,{formato}/validar-xml}`.

## Integraciones y pagos

Catálogo de proveedores (configuración por empresa en **Empresa → Integraciones**). El estado mostrado es honesto: `Activo` solo cuando hay conexión configurada y exitosa; si no, `Vía importación`, `Próximamente` o `Sin conexión directa`:

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
El keep-alive del plan free ya está automatizado en `.github/workflows/keep-alive.yml` (cada 10 min; mitigación, no SLA).

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
- [x] Asentar facturación: traer documentos de la DIAN por token o archivo, causarlos y asentarlos.
- [x] Contabilidad: plan de cuentas, asientos y conciliación.
- [x] Importación/migración con mapeo inteligente de columnas para proveedores externos.
- [x] Entrega de facturas por WhatsApp Click-to-Chat, branding por empresa y PWA instalable.
- [x] `v0.4.0`: periodos, PUC jerárquico, centros de costo, terceros, asientos con reversión/cierre y libros/estados financieros.
- [x] `v0.4.0`: módulo de impuestos con tarifas, causación, pagos, certificados, declaraciones y calendario.
- [x] `v0.4.0`: exógena DIAN oficial 1125/2278/5253 con generador XML y XSD local.
- [x] `v0.4.0`: UI de contabilidad, impuestos y exógena con E2E Playwright.
- [x] `v0.4.0`: JUnit, documentación y quality gates.

Ver issues del repo para el backlog detallado.
