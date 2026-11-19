import axios from 'axios'
import { finalizarPeticion, iniciarPeticion } from './progreso'

const TOKEN_KEY = 'magnetixdian_token'
const REFRESH_KEY = 'magnetixdian_refresh'

export const EVENTO_ERROR_API = 'magnetixdian:error'

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipGlobalError?: boolean
  }
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/+$/, '') ?? ''

export const api = axios.create({
  baseURL: `${API_BASE}/api`,
  headers: { 'Content-Type': 'application/json' },
})

function mensajeDeError(error: unknown): string {
  const e = error as {
    response?: { data?: unknown }
    message?: string
  }
  const data = e?.response?.data
  if (typeof data === 'string' && data.trim()) return data
  if (data && typeof data === 'object') {
    const d = data as Record<string, unknown>
    const m = d.message ?? d.error ?? d.mensaje
    if (typeof m === 'string' && m.trim()) return m
  }
  if (typeof e?.message === 'string' && e.message.trim()) return e.message
  return 'Ocurrió un error inesperado.'
}

api.interceptors.request.use(
  (config) => {
    iniciarPeticion()
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    finalizarPeticion()
    return Promise.reject(error)
  },
)

let refrescoEnCurso: Promise<void> | null = null

function refrescarSesion(): Promise<void> {
  if (!refrescoEnCurso) {
    const refresh = localStorage.getItem(REFRESH_KEY)
    refrescoEnCurso = (async () => {
      if (!refresh) throw new Error('Sin token de refresco')
      const { data } = await axios.post(`${API_BASE}/api/auth/refresh`, { refreshToken: refresh })
      localStorage.setItem(TOKEN_KEY, data.accessToken)
      localStorage.setItem(REFRESH_KEY, data.refreshToken)
    })().finally(() => {
      refrescoEnCurso = null
    })
  }
  return refrescoEnCurso
}

api.interceptors.response.use(
  (response) => {
    finalizarPeticion()
    return response
  },
  async (error) => {
    finalizarPeticion()
    const original = error.config as { _retry?: boolean } & typeof error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      if (localStorage.getItem(REFRESH_KEY)) {
        try {
          await refrescarSesion()
          return api(original)
        } catch {
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(REFRESH_KEY)
          window.location.assign('/login')
          return Promise.reject(error)
        }
      }
    }
    if (error.response?.status !== 401 && !original?.skipGlobalError) {
      window.dispatchEvent(new CustomEvent(EVENTO_ERROR_API, { detail: mensajeDeError(error) }))
    }
    return Promise.reject(error)
  },
)

export function session() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return { username: payload.sub, roles: payload.roles as string[], empresaId: null }
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

export type PerfilUsuario = {
  id: number
  username: string
  nombre: string
  email: string
  roles: string[]
  empresaId: number | null
  empresaNit: string | null
  empresaRazonSocial: string | null
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

export type EmpresaAdmin = {
  id: number
  nit: string
  razonSocial: string
  regimen: string
  tipoDocumento: string
  granContribuyente: boolean
  direccion: string | null
  ciudad: string | null
  departamento: string | null
  codigoDane: string | null
  email: string | null
  createdAt: string
}

export type UsuarioAdmin = {
  id: number
  username: string
  email: string
  nombre: string
  enabled: boolean
  roles: string[]
  empresaId: number | null
}

export type EnlaceAcceso = {
  enlace: string
  tipo: 'ACTIVACION' | 'RECUPERACION'
  usuario: string
}

export type CalendarioAdmin = {
  id: number
  anioGravable: number
  anioPresentacion: number
  tipoReporte: string
  rangoNitIni: string | null
  rangoNitFin: string | null
  fechaLimite: string
}

export type SugerenciaDv = {
  operacionId: number
  linea: number
  tipoDocumento: string
  numero: string
  dvActual: number | null
  dvEsperado: number
  concepto: string
  valorPago: number
}

export type TercerosResumen = {
  totalRegistros: number
  tercerosUnicos: number
  nitsConDvIncorrecto: number
  sugerenciasDv: SugerenciaDv[]
}

export type LineaFacturaRequest = {
  codigoProducto: string | null
  descripcion: string
  cantidad: number
  unidadMedida: string | null
  valorUnitario: number
  descuento: number
  ivaPct: number
  incPct: number
  codigoUnspsc: string | null
}

export type AdquirenteRequest = {
  tipoDocumento: string
  numeroDocumento: string
  digitoVerificacion: number | null
  nombre: string
  email: string | null
  direccion: string | null
  municipio: string | null
  regimen: string | null
  responsabilidadFiscal: string | null
}

export type FacturaRequest = {
  tipoDocumento: string
  adquirente: AdquirenteRequest
  lineas: LineaFacturaRequest[]
  observaciones: string | null
  formaPago: string | null
  medioPago: string | null
  tipoOperacion: string | null
  fechaVencimiento: string | null
  cufeReferencia: string | null
  numeroReferencia: string | null
}

export type FacturaResumen = {
  id: number
  tipoDocumento: string
  numeroCompleto: string
  cufe: string | null
  estado: string
  fechaEmision: string
  adquirenteNombre: string
  adquirenteNumDoc: string
  total: number
  totalPagar: number
  createdAt: string
}

export type FacturaDetalle = {
  id: number
  tipoDocumento: string
  prefijo: string | null
  numero: number
  numeroCompleto: string
  cufe: string | null
  cude: string | null
  ambiente: number
  estado: string
  fechaEmision: string
  horaEmision: string
  fechaVencimiento: string | null
  moneda: string
  formaPago: string
  medioPago: string
  adquirente: AdquirenteRequest
  lineas: LineaFacturaRequest[]
  subtotal: number
  totalImpuestos: number
  totalRetenciones: number
  total: number
  totalPagar: number
  observaciones: string | null
  qrUrl: string | null
  respuestaDian: string | null
  fechaValidacion: string | null
  createdAt: string
  eventos: FacturaEvento[]
}

export type FacturaEvento = {
  id: number
  tipoEvento: string
  descripcion: string
  estado: string
  fecha: string
}

export type ErrorValidacion = {
  codigo: string
  severidad: 'ERROR' | 'ADVERTENCIA' | 'INFO' | null
  mensaje: string | null
  linea: number | null
  campo: string | null
  valorEsperado: string | null
  valorEncontrado: string | null
}

export type ResultadoEmision = {
  exitoso: boolean
  id: number | null
  numeroCompleto: string | null
  cufe: string | null
  estado: string
  qrUrl: string | null
  mensaje: string | null
  errores: ErrorValidacion[]
}

export type ConfigFe = {
  ambiente: number
  softwareId: string | null
  softwarePin: string | null
  softwareSecurityCode: string | null
  testSetId: string | null
  resolucionNumero: string | null
  resolucionFecha: string | null
  prefijo: string | null
  rangoDesde: number | null
  rangoHasta: number | null
  rangoVigenciaDesde: string | null
  rangoVigenciaHasta: string | null
  canal: string
  pacProveedor: string | null
  pacBaseUrl: string | null
  pacApiKey: string | null
  certificadoNombre: string | null
  certificadoPassword: string | null
  certificadoPath: string | null
  responsabilidadFiscal: string
  codigoMunicipio: string | null
  activo: boolean
  tieneCertificado: boolean
  tienePacKey: boolean
  notificarAdquirente: boolean
}

export type CatalogoDian = {
  id: number
  tipo: string
  codigo: string
  descripcion: string
  activo: boolean
}

export type Pagina<T> = {
  contenido: T[]
  pagina: number
  tamano: number
  total: number
  totalPaginas: number
  primera: boolean
  ultima: boolean
}

export type EventoAuditoria = {
  id: number
  usuario: string | null
  accion: string
  entidad: string | null
  entidadId: string | null
  detalle: string | null
  ip: string | null
  createdAt: string
}

export type InvitacionResponse = {
  usuario: UsuarioAdmin
  enlace: string | null
}

export type EnvioFactura = {
  id: number
  facturaId: number
  numeroCompleto: string
  cufe: string | null
  canal: string
  estado: string
  intentos: number
  maxIntentos: number
  proximoIntento: string
  ultimoError: string | null
  actualizadoAt: string
}

export type ResultadoImportacion = {
  facturasEmitidas: number
  facturasRechazadas: number
  lineasImportadas: number
  errores: string[]
}

export type ReenvioFactura = {
  enviado: boolean
  destinatario: string
}

export async function reenviarFactura(empresaId: number, facturaId: number, destinatario: string) {
  const { data } = await api.post<ReenvioFactura>(
    `/facturacion/${empresaId}/${facturaId}/reenviar`,
    { destinatario },
    { skipGlobalError: true },
  )
  return data
}

export async function importarFacturas(empresaId: number, archivo: File) {
  const form = new FormData()
  form.append('archivo', archivo)
  const { data } = await api.post<ResultadoImportacion>(`/facturacion/${empresaId}/importar`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function generarEnlaceAcceso(id: number) {
  const { data } = await api.post<EnlaceAcceso>(`/admin/usuarios/${id}/enlace-acceso`)
  return data
}

export async function listarEnvios(estado?: string, pagina = 0, tamano = 20) {
  const { data } = await api.get<Pagina<EnvioFactura>>('/admin/transmision', {
    params: { page: pagina, size: tamano, estado: estado || undefined },
  })
  return data
}

export async function reintentarEnvio(id: number) {
  const { data } = await api.post<EnvioFactura>(`/admin/transmision/${id}/reintentar`)
  return data
}

export async function solicitarRecuperacion(identificador: string) {
  const { data } = await api.post<{ mensaje: string; enlace: string | null }>(
    '/auth/forgot-password',
    { identificador },
    { skipGlobalError: true },
  )
  return data
}

export async function restablecerPassword(token: string, password: string) {
  await api.post('/auth/reset-password', { token, password }, { skipGlobalError: true })
}

export async function aceptarInvitacion(token: string, password: string) {
  await api.post('/auth/accept-invite', { token, password }, { skipGlobalError: true })
}

export async function cambiarPassword(passwordActual: string, passwordNueva: string) {
  await api.post('/auth/change-password', { passwordActual, passwordNueva }, { skipGlobalError: true })
}

export type Plan = {
  id: number
  codigo: string
  nombre: string
  descripcion: string | null
  precioMensual: number
  moneda: string
  limiteFacturasMes: number
  limiteUsuarios: number
  orden: number
}

export type UsoSuscripcion = {
  facturasMes: number
  facturasRestantes: number | null
  facturasExcedidas: boolean
  usuarios: number
  usuariosRestantes: number | null
  usuariosExcedidos: boolean
}

export type Suscripcion = {
  id: number
  empresaId: number
  plan: Plan
  estado: string
  proveedor: string
  referenciaExterna: string | null
  periodoInicio: string
  periodoFin: string | null
  uso: UsoSuscripcion
}

export async function listarPlanes() {
  const { data } = await api.get<Plan[]>('/planes')
  return data
}

export async function obtenerSuscripcion(empresaId: number) {
  const { data } = await api.get<Suscripcion | null>(`/empresas/${empresaId}/suscripcion`)
  return data
}

export async function cambiarPlan(empresaId: number, planCodigo: string, proveedor?: string) {
  const { data } = await api.post<Suscripcion>(
    `/empresas/${empresaId}/suscripcion`,
    { planCodigo, proveedor },
    { skipGlobalError: true },
  )
  return data
}

export async function cancelarSuscripcion(empresaId: number) {
  await api.delete(`/empresas/${empresaId}/suscripcion`, { skipGlobalError: true })
}

export type ApiKey = {
  id: number
  nombre: string
  prefijo: string
  activo: boolean
  ultimoUso: string | null
  createdAt: string
}

export type ApiKeyCreada = {
  id: number
  nombre: string
  prefijo: string
  clave: string
}

export async function listarApiKeys(empresaId: number) {
  const { data } = await api.get<ApiKey[]>(`/empresas/${empresaId}/api-keys`)
  return data
}

export async function crearApiKey(empresaId: number, nombre: string) {
  const { data } = await api.post<ApiKeyCreada>(
    `/empresas/${empresaId}/api-keys`,
    { nombre },
    { skipGlobalError: true },
  )
  return data
}

export async function revocarApiKey(empresaId: number, id: number) {
  await api.delete(`/empresas/${empresaId}/api-keys/${id}`, { skipGlobalError: true })
}

export type Pago = {
  id: number
  referencia: string
  proveedor: string
  planCodigo: string
  monto: number
  moneda: string
  estado: string
  url: string | null
}

export async function listarPagos(empresaId: number) {
  const { data } = await api.get<Pago[]>(`/empresas/${empresaId}/pagos`)
  return data
}

export async function iniciarCheckout(empresaId: number, planCodigo: string, proveedor: string) {
  const { data } = await api.post<Pago>(
    `/empresas/${empresaId}/pagos/checkout`,
    { planCodigo, proveedor },
    { skipGlobalError: true },
  )
  return data
}