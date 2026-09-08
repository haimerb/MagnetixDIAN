export default {
  async fetch(request, env) {
    const url = new URL(request.url)

    if (url.pathname.startsWith('/api/')) {
      const backend = env.BACKEND_ORIGIN || 'https://magnetixdian-backend.onrender.com'
      const target = `${backend}${url.pathname}${url.search}`
      return fetch(new Request(target, request))
    }

    return env.ASSETS.fetch(request)
  },
}