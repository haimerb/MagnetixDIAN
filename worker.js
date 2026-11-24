const HEALTH_TIMEOUT_MS = 90_000
const SW_PLACEHOLDER = '__CACHE_VERSION__'

const PWA_ASSETS = new Map([
  ['/manifest.webmanifest', {
    'Content-Type': 'application/manifest+json; charset=utf-8',
    'Cache-Control': 'public, max-age=3600',
  }],
  ['/sw.js', {
    'Content-Type': 'application/javascript; charset=utf-8',
    'Cache-Control': 'no-cache, no-store, must-revalidate',
    'Service-Worker-Allowed': '/',
  }],
  ['/icon.svg', {
    'Content-Type': 'image/svg+xml',
    'Cache-Control': 'public, max-age=86400',
  }],
])

function sanitizarVersion(valor) {
  return String(valor || '')
    .replace(/^W\//, '')
    .replace(/"/g, '')
    .replace(/[^a-zA-Z0-9._-]/g, '')
    .slice(0, 48)
}

async function versionDelBundle(request, env) {
  if (env.SW_VERSION && env.SW_VERSION !== 'dev') {
    return sanitizarVersion(env.SW_VERSION)
  }
  try {
    const index = await env.ASSETS.fetch(new Request(new URL('/index.html', request.url).toString(), request))
    if (index.ok) {
      const etag = index.headers.get('etag')
      if (etag) return sanitizarVersion(etag)
      const html = await index.text()
      const coincidencia = html.match(/assets\/index-([a-zA-Z0-9_-]+)\.js/)
      if (coincidencia) return sanitizarVersion(coincidencia[1])
    }
  } catch {
    // Sin versión disponible se usa un valor estable; el deploy inyecta SW_VERSION.
  }
  return 'sin-version'
}

async function servirServiceWorker(request, env, cabeceras) {
  const [asset, version] = await Promise.all([
    env.ASSETS.fetch(request),
    versionDelBundle(request, env),
  ])
  const cuerpo = (await asset.text()).replaceAll(SW_PLACEHOLDER, version)
  const headers = new Headers(asset.headers)
  for (const [nombre, valor] of Object.entries(cabeceras)) {
    headers.set(nombre, valor)
  }
  headers.set('X-Content-Type-Options', 'nosniff')
  return new Response(cuerpo, {
    status: asset.status,
    statusText: asset.statusText,
    headers,
  })
}

async function pingBackend(env) {
  const backend = env.BACKEND_ORIGIN || 'https://magnetixdian-backend.onrender.com'
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), HEALTH_TIMEOUT_MS)

  try {
    const response = await fetch(`${backend}/actuator/health`, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
        'User-Agent': 'MagnetixDIAN-KeepAlive/1.0',
      },
      signal: controller.signal,
    })
    console.info(`[keep-alive] ${new Date().toISOString()} -> ${response.status}`)
    if (!response.ok) {
      console.error(`[keep-alive] respuesta no OK: ${response.status}`)
    }
  } catch (error) {
    console.error(`[keep-alive] fallo: ${error?.message || error}`)
  } finally {
    clearTimeout(timeout)
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url)

    const pwaHeaders = PWA_ASSETS.get(url.pathname)
    if (pwaHeaders) {
      if (url.pathname === '/sw.js') {
        return servirServiceWorker(request, env, pwaHeaders)
      }
      const asset = await env.ASSETS.fetch(request)
      const headers = new Headers(asset.headers)
      for (const [name, value] of Object.entries(pwaHeaders)) {
        headers.set(name, value)
      }
      headers.set('X-Content-Type-Options', 'nosniff')
      return new Response(asset.body, {
        status: asset.status,
        statusText: asset.statusText,
        headers,
      })
    }

    if (url.pathname.startsWith('/api/')) {
      const backend = env.BACKEND_ORIGIN || 'https://magnetixdian-backend.onrender.com'
      const target = `${backend}${url.pathname}${url.search}`
      return fetch(new Request(target, request))
    }

    return env.ASSETS.fetch(request)
  },

  async scheduled(_event, env) {
    await pingBackend(env)
  },
}
