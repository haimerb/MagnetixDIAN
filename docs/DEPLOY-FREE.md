# Deploy gratuito — Cloudflare Workers + Render + Supabase

Deploy **alternativo e independiente** del existente (docker-compose / ZIP a GoDaddy).
No toca el flujo actual; es 100% gratis, sin tarjeta y sin fecha de expiración.

```
Navegador
   │
   ├─ https://magnetixdian.<cuenta>.workers.dev   → Cloudflare Worker (frontend React + proxy /api)
   │        │  /api/...  (proxy reenvía a Render)
   │        ▼
   │  https://magnetixdian-backend.onrender.com   → Render Free (Spring Boot, Docker, 512 MB)
   │        │  JDBC + Flyway
   │        ▼
   │  Postgres de Supabase (free, 500 MB)        → aws-0-<REGION>.pooler.supabase.com:5432
```

**Avisos honestos (free tier):**
- El backend de Render **se duerme tras 15 min sin tráfico**; la 1ª petición tras inactividad tarda ~30-60s en responder.
- Discos efímeros en Render: `uploads/` no persiste entre reinicios (los datos ya viven en Supabase).
- Supabase free: 500 MB de base, 1 proyecto activo. Suficiente para la app.
- Límites: 512 MB RAM backend (JVM acotada a 256 MB), 100 GB ancho de banda Render/mes.

---

## 1. Base de datos — Supabase (10 min)

1. Seguí <https://supabase.com> (email; **no pide tarjeta**) y un **New project**.
   - Name: `magnetixdian` · Database password: **anótala** (será `DB_PASSWORD`).
2. En el proyecto, botón **Connect → pestaña "Session pooler"** (necesaria para IPv4, requerida por Render):
   - Host: `aws-0-<REGION>.pooler.supabase.com` · Puerto `5432`
   - User: `postgres.<project-ref>`
3. No hay que crear tablas: **Flyway migra automáticamente (V1+V2+V3)** al primer arranque del backend.
   - La conexión directa (`db.<ref>.supabase.co`) es **solo IPv6** → no funciona desde Render.
   - El schema `public` viene ocupado → la app usa **baseline-on-migrate** (`application.yml`).

> Conexión JDBC final:
> `jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require`

---

## 2. Backend — Render (15 min)

Para que Render acepte el repo, primero súbelo a GitHub.

1. Cuenta en <https://render.com> con GitHub (sin tarjeta).
2. **New → Blueprint** → conectá el repo del proyecto.
3. Render lee `render.yaml` (raíz). Valores a cargar en el servicio `magnetixdian-backend`:
   - `DB_URL` → `jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require`
   - `DB_USER` → `postgres.<project-ref>`
   - `DB_PASSWORD` → la password del proyecto Supabase (**secreto**)
   - `JWT_SECRET` → `openssl rand -base64 64` (**secreto**)
   - `CORS_ORIGINS` → URL del frontend: `https://magnetixdian.<cuenta>.workers.dev,https://magnetixdian.pages.dev`
   - `UPLOAD_DIR` → `/tmp/uploads` (ya en render.yaml)
4. **Deploy** (la 1ª vez compila ~5-10 min).
5. Verificar:
   ```bash
   curl https://magnetixdian-backend.onrender.com/actuator/health   # {"status":"UP"}
   ```

> Sube primero el backend; es lo que crea las tablas vía Flyway.

---

## 3. Frontend — Cloudflare Workers con git + build (recomendado)

Usa el flujo **Workers → Connect to Git** (compila con `wrangler`). El repo incluye:

- `worker.js` — sirve el frontend y **proxya `/api/*`** a Render (mismo-origin → sin CORS).
- `wrangler.toml` — assets `frontend/dist`, `not_found_handling = single-page-application` (SPA),
  y sección `[build]` que compila Vite **automáticamente** antes de `npx wrangler deploy`.

Configuración en Cloudflare:

| Campo | Valor |
|---|---|
| Repositorio | `haimerb/MagnetixDIAN` |
| Deploy command | **dejar el default** `npx wrangler deploy` |

**No hace falta tocar el build command**: el `[build]` de `wrangler.toml` corre
`pnpm install --frozen-lockfile && pnpm run build` en `frontend/` antes de cada deploy.

> Si el proyecto se creó con una variable `VITE_API_BASE_URL` apuntando a Render, el navegador
> llama a Render **directo (cross-origin)** y ahí sí aplica CORS → por eso `CORS_ORIGINS` incluye
> el dominio del worker. Para evitar CORS del todo, borrar esa variable y redeployear (el proxy lo cubre).

URL final de la app: `https://magnetixdian.<cuenta>.workers.dev`

### Alternativa: Cloudflare Pages (flujo clásico)

Workers & Pages → **Pages → Create → Connect to Git** → Root directory `frontend`,
build `pnpm run build`, output `dist`, env `VITE_API_BASE_URL` = Render URL, y en Render
`CORS_ORIGINS` incluir `https://<proyecto>.pages.dev`.

---

## 4. Variables que intervienen

| Variable | Dónde | Valor |
|---|---|---|
| `VITE_API_BASE_URL` | Cloudflare (solo si NO usás el proxy) | `https://magnetixdian-backend.onrender.com` |
| `CORS_ORIGINS` | Render (env) | `https://magnetixdian.<cuenta>.workers.dev,https://magnetixdian.pages.dev` |
| `DB_URL` | Render (env) | `jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USER` / `DB_PASSWORD` | Render (env) | `postgres.<ref>` / password de Supabase |
| `JWT_SECRET` | Render (env) | base64 de 64 bytes (`openssl rand -base64 64`) |
| `CRYPTO_KEY` | Render (env) | secreto para cifrar credenciales DIAN en BD (`openssl rand -base64 32`) |

Seguridad: PostgreSQL de Supabase **no se expone** (solo via pooler con SSL); Render es el único
cliente. El backend no publica puerto externo; Cloudflare sirve los estáticos con HTTPS gratis.

---

## 5. Pruebas end-to-end

1. Abrir `https://magnetixdian.<cuenta>.workers.dev` → login `admin` / `admin123`.
2. Descargar plantilla 1001 y subir un Excel con datos (regenerarlo si hace falta:
   `backend/tools/GenXlsx.java` — ver AGENTS.md).
3. Ejecutar validación → errores/DV → corregir → generar XML → marcar presentado.
4. `/actuator/health` del backend debe seguir `UP` (migraciones aplicadas).

---

## 6. Cambios de código incluidos (retrocompatibles)

- **CORS configurable**: `SecurityConfig.java` + `application.yml` leen `CORS_ORIGINS`
  (default `localhost:5173,4173` → el flujo local **no cambia**).
- **URL de API configurable**: `frontend/src/api/client.ts` usa `VITE_API_BASE_URL`
  (default `/api` → el proxy de Vite local **no cambia**).
- **Flyway baseline**: `baseline-on-migrate + baseline-version 0` para schema `public` preexistente.
- `backend/Dockerfile.render` (JVM `-Xmx256m`), `render.yaml` (blueprint), `worker.js` + `wrangler.toml`
  (frontend) — solo los usan Render y Cloudflare.

Nada del flujo actual (docker-compose, ZIP a GoDaddy, CI) se ve afectado.

---

## 7. Deploy automático tras merge a `main`

El workflow `.github/workflows/deploy.yml` publica automáticamente al hacer push/merge a `main`
(también se puede lanzar a mano con *Run workflow*):

- **Frontend:** compila Vite y ejecuta `wrangler deploy` (Cloudflare Worker).
- **Backend:** dispara el *Deploy Hook* de Render.

Configurar estos **secrets** en GitHub (Settings → Secrets and variables → Actions):

| Secret | Descripción |
|---|---|
| `CLOUDFLARE_API_TOKEN` | Token con permiso *Edit Cloudflare Workers* |
| `CLOUDFLARE_ACCOUNT_ID` | ID de la cuenta de Cloudflare |
| `RENDER_DEPLOY_HOOK` | URL del Deploy Hook del servicio en Render (Settings → Deploy Hook) |

Si un secret no está definido, el job correspondiente se omite con un aviso (no falla). El backend
también puede desplegarse solo con `autoDeploy: true` de `render.yaml`.