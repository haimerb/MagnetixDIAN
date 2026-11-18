# MagnetixDIAN

Plataforma de cumplimiento tributario DIAN con **dos módulos**:

1. **Información exógena (medios magnéticos)** — formatos **1001 v10** (pagos/abonos y retenciones) y **1002 v10** (créditos, descuentos y notas). Carga un Excel, valida contra las reglas DIAN (dígito de verificación del NIT, conceptos del catálogo, topes en UVT), genera el reporte y descarga el **XML listo para presentar en MUISCA**.
2. **Facturación electrónica (FEV v1.9)** — emisión de facturas (01), notas crédito (91) y notas débito (92) con **CUFE/CUDE** (SHA-384), **UBL 2.1**, firma **XAdES-EPES**, PDF, QR y transmisión por canal (simulado / PAC / DIAN SOAP).

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

Checks previos a commit: backend `mvn clean package`; frontend `pnpm run lint` (0 errores), `pnpm run test` y `pnpm run build`.

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

Ver issues del repo para el backlog detallado.