# Deploy gratuito — Cloudflare Workers + Render + Supabase

Arquitectura de **costo cero**:

```
Navegador → Cloudflare Worker (SPA + proxy /api + PWA + keep-alive)
                         │
                         ▼
              Render free (Spring Boot, JVM -Xmx256m)
                         │
                         ▼
        Supabase free (PostgreSQL 16, Session pooler IPv4)
```

La URL del frontend es `https://magnetixdian.<cuenta>.workers.dev` (o
`https://magnetixdian.pages.dev`). Backend: `https://magnetixdian-backend.onrender.com`.

## 1. Base de datos (Supabase)

1. Crear proyecto free en la región deseada.
2. Usar **Session pooler (IPv4)**: `aws-0-<REGION>.pooler.supabase.com:5432`,
   usuario `postgres.<ref>`. La conexión directa es IPv6-only y no funciona desde Render.
3. `DB_URL` debe incluir `?sslmode=require`. Flyway usa `baseline-on-migrate=true` porque
   el schema `public` ya existe.
4. No commitear contraseñas: `DB_PASSWORD` se configura como secreto en Render.

## 2. Backend (Render)

1. New → Blueprint → seleccionar `render.yaml`.
2. Completar los secretos marcados `sync: false`. **No** usar valores de ejemplo.
3. `SEGURIDAD_FAIL_FAST=true`: la aplicación falla al arrancar si falta cualquiera de:

   | Variable | Uso |
   | --- | --- |
   | `JWT_SECRET` | Firma de tokens (base64 de 64 bytes) |
   | `CRYPTO_KEY` | Cifrado AES-GCM de credenciales/tokens (32 bytes) |
   | `CRYPTO_KEY_ID` | Identificador de la clave activa (p. ej. `primary`) |
   | `FRONTEND_URL` | Enlaces de correo y invitaciones |
   | `WOMPI_EVENTS_SECRET` | Firma de webhooks de Wompi |
   | `MERCADOPAGO_WEBHOOK_SECRET` | Firma de webhooks de MercadoPago |

4. Rotación de claves: mantener `CRYPTO_KEY` anterior en `CRYPTO_EXTRA_KEYS` con otro
   `CRYPTO_KEY_ID` (formato `id=secreto,id2=secreto2`). Los valores cifrados con `enc:`
   y `enc:v2:` siguen siendo legibles. **Nunca regenerar `CRYPTO_KEY` sin conservar la anterior.**
5. `PAGOS_EXIGIR_FIRMA_WEBHOOK=true` e `INTEGRACIONES_EXIGIR_FIRMA_WEBHOOK=true` (fail-closed).
   No se permite pasarela `SIMULADO` ni secretos de ejemplo en producción: la app no arranca.
6. `CORS_ORIGINS` debe incluir `https://magnetixdian.<cuenta>.workers.dev` y
   `https://magnetixdian.pages.dev` (el worker ya los incluye por defecto en `SecurityConfig`).
7. Healthcheck: `GET /actuator/health`.

## 3. Frontend (Cloudflare Worker)

`wrangler.toml` compila Vite (`pnpm install --frozen-lockfile && pnpm run build`), sirve
`frontend/dist` y `worker.js`:

- `/api/*` se proxya a `BACKEND_ORIGIN` (Render).
- Rutas cliente devuelven `index.html` (`not_found_handling = single-page-application`).
- `run_worker_first` garantiza cabeceras propias para `/manifest.webmanifest`, `/sw.js` e `/icon.svg`.

| Ruta | Content-Type | Cache-Control |
| --- | --- | --- |
| `/sw.js` | `application/javascript; charset=utf-8` | `no-cache, no-store, must-revalidate` + `Service-Worker-Allowed: /` |
| `/manifest.webmanifest` | `application/manifest+json; charset=utf-8` | `public, max-age=3600` |
| `/icon.svg` | `image/svg+xml` | `public, max-age=86400` |

### Invalidación del service worker

- `frontend/public/sw.js` usa `const CACHE = 'magnetixdian-__CACHE_VERSION__'`.
- En cada deploy, `deploy.yml` usa `cloudflare/wrangler-action@v4` con Wrangler 4
  y la entrada `vars: SW_VERSION`, enlazada al SHA del commit; equivale a
  `wrangler deploy --var SW_VERSION:<sha>`.
- El workflow verifica que `/sw.js` devuelva `magnetixdian-<sha>` antes de marcar el deploy como verde.
- `frontend/src/main.tsx` registra el SW en `/sw-<hash-del-bundle>.js`; el Worker
  sirve el mismo asset en esa ruta y en `/sw.js`, de modo que la URL cambia en cada build.
- `worker.js` sustituye el placeholder por ese SHA; si no está disponible, usa el
  ETag de `index.html` o el hash `index-<hash>.js` del bundle, que cambia en cada
  build, y responde sin cachear el SW.
- Al activarse, el SW elimina las cachés `magnetixdian-*` anteriores y toma el control.

## 4. Variables de entorno

Plantilla completa: [`.env.example`](../.env.example). Grupos:

- **BD/servidor:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `UPLOAD_DIR`.
- **Seguridad:** `JWT_SECRET`, `CRYPTO_KEY`, `CRYPTO_KEY_ID`, `CRYPTO_EXTRA_KEYS`,
  `SEGURIDAD_FAIL_FAST`, `AUTH_EXPOSE_TOKEN=false`, `CORS_ORIGINS`, `FRONTEND_URL`.
- **Pagos:** `WOMPI_PUBLIC_KEY`, `WOMPI_INTEGRITY_SECRET`, `WOMPI_EVENTS_SECRET`,
  `WOMPI_API_TOKEN`, `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET`,
  `PAGOS_EXIGIR_FIRMA_WEBHOOK=true`, `PAGOS_WEBHOOK_TOLERANCE_SECONDS=300`.
- **DIAN:** `DIAN_URL_HABILITACION`, `DIAN_URL_PRODUCCION`, timeouts,
  `DIAN_CATALOGO_*`. Los tokens de usuario y certificados **no** son variables de entorno:
  se cifran por empresa.
- **Correo/PWA/observabilidad:** `MAIL_*`, `BRANDING_*`, `WHATSAPP_*`, `SENTRY_DSN`.
- **Frontend:** `VITE_API_BASE_URL=/api`, `VITE_SENTRY_DSN`.

## 5. CI y deploy automático

Checks requeridos en pull request: `backend`, `frontend`, `e2e`, `seguridad` y
`required-checks`. El job `seguridad` ejecuta `git diff --check`, gitleaks (con `--redact`,
sin secretos en logs) y `mvn -q clean package`. `frontend` ejecuta
`pnpm run lint -- --max-warnings=0` y `pnpm run build`.

`deploy.yml` (push a `main`) **falla** si falta `CLOUDFLARE_API_TOKEN`,
`CLOUDFLARE_ACCOUNT_ID` o `RENDER_DEPLOY_HOOK`; nunca publica verde. Tras el deploy espera
a que `/actuator/health` devuelva `"status":"UP"`; si el backend no arranca por secretos
faltantes, el workflow falla.

Secretos en GitHub → Settings → Secrets and variables → Actions:

| Secreto | Uso |
| --- | --- |
| `CLOUDFLARE_API_TOKEN` | Deploy del Worker |
| `CLOUDFLARE_ACCOUNT_ID` | Cuenta de Cloudflare |
| `RENDER_DEPLOY_HOOK` | Disparar deploy en Render |
| `GITLEAKS_LICENSE` | Solo si el repo pertenece a una organización |

El keep-alive (`.github/workflows/keep-alive.yml` y el cron del worker cada 5 min) mitiga el
sleep del plan free; **no es un SLA**.

## 6. Checklist de habilitación PT

1. Resolución de habilitación vigente, prefijo y rangos aprobados.
2. `softwareId` y `testSetId` aprobados en habilitación.
3. Certificado PKCS#12 vigente por empresa (cifrado en BD).
4. Emisiones de prueba en el entorno de habilitación y notificación al adquirente.
5. Plan con soporte para facturación a terceros (`PT_MAGNETIXDIAN` / `PT_EXTERNO`).
6. Webhooks de pago con firma activa y secretos presentes antes de habilitar el cobro.

## 7. Límites honestos

- La exógena se valida con **XSD local**; no se ofrece prevalidación oficial de la DIAN.
- La generación automática / aceptación tácita y los reintentos son mitigaciones, no garantía.
- Ningún secreto, token de DIAN ni certificado se guarda en el repositorio ni en variables del worker.
