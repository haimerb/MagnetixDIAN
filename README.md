# MagnetixDIAN

Generación y presentación de **información exógena DIAN** (medios magnéticos) — formato 1001 v10 (pagos/abonos y retenciones).

Carga un Excel con las operaciones del período, valida contra las reglas DIAN (dígito de verificación del NIT, conceptos del catálogo, topes en UVT), genera el reporte de validación y descarga el **XML listo para presentar en MUISCA**.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 17 · Spring Boot 3.3.5 · Flyway · Spring Security (JWT) |
| Base de datos | PostgreSQL 16 (Docker) |
| Excel / XML | Apache POI · DOM + XSD propio |
| Frontend | React 18 · Vite · MUI 6.5 · React Router |
| Documentación | Swagger/OpenAPI |

## Estructura (monorepo)

```
backend/    API REST + reglas de negocio + generación XML
frontend/   SPA React (Vite) con proxy /api → :8080
docker/     docker-compose (db, backend, frontend)
k8s/        Manifiestos Kubernetes (namespace factelect)
docs/       Manual técnico, manual de usuario y ejemplos
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
cd frontend && npm install && npm run dev
```

Checks previos a commit: backend `mvn clean package`; frontend `npm run lint` (0 errores) y `npm run build`. Convenciones en [`AGENTS.md`](AGENTS.md).

## Documentación

- [`docs/MANUAL-TECNICO.md`](docs/MANUAL-TECNICO.md) — arquitectura, modelo de datos, reglas, API, despliegue.
- [`docs/MANUAL-USUARIO.md`](docs/MANUAL-USUARIO.md) — uso de la aplicación paso a paso.
- [`docs/ejemplos/`](docs/ejemplos/) — XML de salida y respuestas JSON de la API.

## Roadmap

- [ ] Suite de pruebas (JUnit) para el motor de reglas.
- [ ] Soporte multiformato (1002, 2276…) y marco normativo por año.
- [ ] Panel de administración (empresas, usuarios, calendario).
- [ ] CI/CD en GitHub Actions (build + lint + tests) y despliegue K8s.

Ver issues del repo para el backlog detallado.