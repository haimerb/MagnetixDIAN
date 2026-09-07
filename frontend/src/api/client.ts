import axios from 'axios'

const TOKEN_KEY = 'magnetixdian_token'
const REFRESH_KEY = 'magnetixdian_refresh'

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as { _retry?: boolean } & typeof error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      const refresh = localStorage.getItem(REFRESH_KEY)
      if (refresh) {
        try {
          const { data } = await axios.post('/api/auth/refresh', { refreshToken: refresh })
          localStorage.setItem(TOKEN_KEY, data.accessToken)
          localStorage.setItem(REFRESH_KEY, data.refreshToken)
          return api(original)
        } catch {
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(REFRESH_KEY)
          window.location.assign('/login')
        }
      }
    }
    return Promise.reject(error)
  },
)

export function session() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return { username: payload.sub, roles: payload.roles as string[] }
  } catch {
    return null
  }
}

export function saveTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

export type LoginResponse = {
  accessToken: string
  refreshToken: string
  username: string
  roles: string[]
}

export type CargaResultado = {
  medioMagneticoId: number
  registrosCargados: number
}

export type DetalleError = {
  linea: number | null
  campo: string | null
  valorEsperado: string | null
  valorEncontrado: string | null
  mensaje: string
  severidad: 'ERROR' | 'ADVERTENCIA' | 'INFO'
}

export type Validacion = {
  id: number
  medioMagneticoId: number
  estado: string
  erroresTotal: number
  advertenciasTotal: number
  registrosValidados: number
  inicio: string
  fin: string | null
  errores: DetalleError[]
}

export type MedioMagnetico = {
  id: number
  empresaId: number
  empresaNit: string
  empresaRazonSocial: string
  formato: string
  versionFormato: string
  anioGravable: number
  estado: string
  fechaLimite: string | null
  createdAt: string
  updatedAt: string
}