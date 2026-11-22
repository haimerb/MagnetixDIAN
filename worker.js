const HEALTH_TIMEOUT_MS = 90_000

const PWA_ASSETS = new Map([
  ['/manifest.webmanifest', {
    'Content-Type': 'application/manifest+json; charset=utf-8',
    'Cache-Control': 'public, max-age=3600',
  }],
  ['/sw.js', {
    'Content-Type': 'application/javascript; charset=utf-8',
    'Cache-Control': 'no-cache',
    'Service-Worker-Allowed': '/',
  }],
  ['/icon.svg', {
    'Content-Type': 'image/svg+xml',
    'Cache-Control': 'public, max-age=86400',
  }],
])

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
