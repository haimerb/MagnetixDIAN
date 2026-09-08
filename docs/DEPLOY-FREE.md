# Deploy gratuito — Cloudflare Pages + Render + Supabase

Deploy **alternativo e independiente** del existente (docker-compose / ZIP a GoDaddy).
No toca el flujo actual; es 100% gratis, sin tarjeta y sin fecha de expiración.

```
Navegador
   │
   ├─ https://magnetixdian.pages.dev        → Cloudflare Pages (frontend React, build estático)
   │        │  /api/...  (proxy VITE_API_BASE_URL)
   │        ▼
   │  https://magnetixdian-backend.onrender.com   → Render Free (Spring Boot, Docker, 512 MB)
   │        │  JDBC + Flyway
   │        ▼
   │  Postgres de Supabase (free, 500 MB)   → db.<ref>.supabase.co:5432
```

**Avisos honestos (free tier):**
- El backend de Render **se duerme tras 15 min sin tráfico**; la 1ª petición tras inactividad tarda ~30-60s en responder.
- Discos efímeros en Render: `uploads/` no persiste entre reinicios (los datos ya viven en Supabase).
- Supabase free: 500 MB de base, 1 proyecto activo. Suficiente para la app.
- Límites: 512 MB RAM backend (JVM acotada a 256 MB), 100 GB ancho de banda Render/mes.

---

## 1. Base de datos — Supabase (10 min)

1. Crear cuenta en <https://supabase.com> (email; **no pide tarjeta**) y un **New project**.
   - Name: `magnetixdian` · Database password: **anótala** (será `DB_PASSWORD`).
   - Region: la más cercana a tus usuarios (ej. `South America (São Paulo)`).
2. En **Project Settings → Database → Connection string**: copia `host` (`db.<ref>.supabase.co`), puerto directo **5432**, user `postgres`.
3. No hay que crear tablas: **Flyway migra automáticamente (V1+V2+V3)** al primer arranque del backend.

> Conexión JDBC final:
> `jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?sslmode=require`

---

## 2. Backend — Render (15 min)

Necesitas subir el repo a GitHub primero (Render no acepta repos locales).

1. Cuenta en <https://render.com> con GitHub (sin tarjeta).
2. **New → Blueprint** → conéctate al repo del proyecto.
3. Render lee `render.yaml` (raíz del repo). Rellena en el dashboard del servicio `magnetixdian-backend`:
   - `DB_URL` → `jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?sslmode=require`
   - `DB_USER` → `postgres`
   - `DB_PASSWORD` → la password del proyecto Supabase (**secreto**)
   - `JWT_SECRET` → `openssl rand -base64 64` (**secreto**)
   - `CORS_ORIGINS` → `https://magnetixdian.pages.dev`
   - `UPLOAD_DIR` → `/tmp/uploads` (ya en render.yaml)
4. **Deploy** (la 1ª vez compila ~5-10 min).
5. Verificar:
   ```bash
   curl https://magnetixdian-backend.onrender.com/actuator/health   # {"status":"UP"}
   ```
   El healthcheck de Render también lo valida (`/actuator/health`).

> Sube primero el backend; es lo que crea las tablas vía Flyway.

---

## 3. Frontend — Cloudflare Workers con git + build (recomendado)

Usa el flujo de Cloudflare **Workers → Connect to Git** (compila con `wrangler`).
El repo incluye:

- `wrangler.toml` — sirve `frontend/dist` como MPA/SPA (`not_found_handling = single-page-application`),
  con variable `BACKEND_ORIGIN` apuntando al backend de Render.
- `worker.js` — reenvía `/api/*` al backend de Render. **Así no se necesita CORS ni `VITE_API_BASE_URL`**:
  el navegador habla solo con el dominio del worker y este proxya.

Configuración en Cloudflare:

| Campo | Valor |
|---|---|
| Repositorio | `haimerb/MagnetixDIAN` |
| **Build command** | `npm --prefix frontend run build && npx -y wrangler deploy` |
| Variable `VITE_API_BASE_URL` | **dejar de usar / eliminar** (el proxy cubre `/api`) |

Si tu proyecto final es `magnetixdian`, la URL será `https://magnetixdian.<cuenta>.workers.dev`.

### Alternativa: Cloudflare Pages (flujo clásico)
Workers & Pages → **Pages → Create → Connect to Git**:

| Campo | Valor |
|---|---|
| Framework preset | `Vite` |
| Root directory | `frontend` |
| Build command | `npm run build` |
| Build output directory | `dist` |
| Env `VITE_API_BASE_URL` | `https://magnetixdian-backend.onrender.com` |
| (`CORS_ORIGINS` en Render) | `https://magnetixdian.pages.dev` |

---

## 4. Variables que intervienen

| Variable | Dónde | Valor |
|---|---|---|
| `VITE_API_BASE_URL` | Cloudflare Pages (env) | `https://magnetixdian-backend.onrender.com` |
| `CORS_ORIGINS` | Render (env) | `https://<tu-proyecto>.pages.dev` |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Render (env) | Supabase (JDBC + SSL) |
| `JWT_SECRET` | Render (env) | base64 de 64 bytes (`openssl rand -base64 64`) |

Seguridad: PostgreSQL de Supabase **no se expone**; Render solo habla con él. El backend no publica puerto externo; Cloudflare Page sirve los estáticos con HTTPS gratis.

---

## 5. Pruebas end-to-end

1. Abrir `https://<tu-proyecto>.pages.dev` → login `admin` / `admin123`.
2. Descargar plantilla 1001 y subir un Excel con datos (regenerarlo si hace falta:
   `backend/tools/GenXlsx.java` — ver AGENTS.md).
3. Ejecutar validación → errores/DV → corregir → generar XML → marcar presentado.
4. `/actuator/health` del backend debe seguir `UP` después del primer arranque (migraciones aplicadas).

---

## 6. Cambios de código incluidos (retrocompatibles)

- **CORS configurable**: `SecurityConfig.java` + `application.yml` leen `CORS_ORIGINS`
  (default `localhost:5173,4173` → el flujo local **no cambia**).
- **URL de API configurable**: `frontend/src/api/client.ts` usa `VITE_API_BASE_URL`
  (default `/api` → el proxy de Vite local **no cambia**).
- `backend/Dockerfile.render` (JVM `-Xmx256m`) y `render.yaml` (blueprint) — solo los usa Render.

Nada del flujo actual (docker-compose, ZIP a GoDaddy, CI) se ve afectado.