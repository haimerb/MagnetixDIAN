import axios from 'axios'
import { finalizarPeticion, iniciarPeticion } from './progreso'

const TOKEN_KEY = 'magnetixdian_token'
const REFRESH_KEY = 'magnetixdian_refresh'

export const EVENTO_ERROR_API = 'magnetixdian:error'
export const EVENTO_SESION = 'magnetixdian:sesion'

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

export type FormatoExogena = {
  codigo: string
  descripcion: string
  estado: string
  activo: boolean
  version: string | null
  xsd: string | null
  validacion: string | null
  generadorXml: string | null
  encabezados: string[]
}

export type FormatoDetectado = {
  codigo: string
  hoja: string
  estado: string
  activo: boolean
  filas: number
  encabezados: string[]
  datos: Record<string, string>[]
  medioMagneticoId: number | null
}

export type ImportacionMultiFormato = {
  anioGravable: number | null
  formatos: FormatoDetectado[]
  hojasOmitidas: string[]
  formatosPersistidos: number
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

export type ValidacionEstructura = Omit<Validacion, 'id'> & { id: number | null }

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
  pedidoId: number | null
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

export async function importarMultiformato(empresaId: number, anioGravable: number, archivo: File) {
  const form = new FormData()
  form.append('anioGravable', String(anioGravable))
  form.append('archivo', archivo)
  const { data } = await api.post<ImportacionMultiFormato>(
    `/mediomagnetico/${empresaId}/importar-multiformato`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return data
}

export async function validarDatosJson(medioId: number) {
  const { data } = await api.post<ValidacionEstructura>(`/mediomagnetico/${medioId}/validar-datos-json`)
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

export type RegistroEmpresaRequest = {
  nit: string
  razonSocial: string
  nombre: string
  email: string
  username: string
  password: string
  aceptaTerminos: boolean
  website?: string
  planCode: string
}

export type RegistroEmpresaResponse = {
  empresaId: number
  usuarioId: number
  username: string
  mensaje: string
}

export async function registrarEmpresa(solicitud: RegistroEmpresaRequest) {
  const { data } = await api.post<RegistroEmpresaResponse>('/auth/registro', solicitud, {
    skipGlobalError: true,
  })
  return data
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

export async function obtenerPago(empresaId: number, referencia: string) {
  const { data } = await api.get<Pago>(`/empresas/${empresaId}/pagos/${encodeURIComponent(referencia)}`)
  return data
}

export async function reconciliarPago(empresaId: number, referencia: string) {
  const { data } = await api.post<Pago>(
    `/empresas/${empresaId}/pagos/${encodeURIComponent(referencia)}/reconciliar`,
    undefined,
    { skipGlobalError: true },
  )
  return data
}

export type MetodoPago = 'CARD' | 'NEQUI' | 'PSE' | 'BREB_QR' | 'DAVIPLATA'

export async function iniciarCheckout(
  empresaId: number,
  planCodigo: string,
  proveedor: string,
  metodo: MetodoPago = 'CARD',
) {
  const { data } = await api.post<Pago>(
    `/empresas/${empresaId}/pagos/checkout`,
    { planCodigo, proveedor, metodo },
    { skipGlobalError: true },
  )
  return data
}

export type ProveedorIntegracion =
  | 'SIIGO'
  | 'ALEGRA'
  | 'ODOO'
  | 'WORLD_OFFICE'
  | 'HELISA'
  | 'SHOPIFY'
  | 'WOOCOMMERCE'
  | 'SAP_BUSINESS_ONE'
  | 'DYNAMICS_365'
  | 'NETSUITE'

export const PROVEEDORES_INTEGRACION: readonly ProveedorIntegracion[] = [
  'SIIGO',
  'ALEGRA',
  'ODOO',
  'WORLD_OFFICE',
  'HELISA',
  'SHOPIFY',
  'WOOCOMMERCE',
  'SAP_BUSINESS_ONE',
  'DYNAMICS_365',
  'NETSUITE',
]

export type CapacidadIntegracion =
  | 'SYNC_CLIENTES'
  | 'SYNC_PRODUCTOS'
  | 'EMITIR_FACTURA'
  | 'CONSULTAR_ESTADO'
  | 'REGISTRAR_PAGO'
  | 'RECIBIR_WEBHOOK'

export type EstadoIntegracion = 'CONECTADO' | 'DESCONFIGURADO' | 'ERROR' | 'SINCRONIZANDO'

export type IntegracionConfig = {
  id: number | null
  proveedor: ProveedorIntegracion
  slug: string
  activo: boolean
  estado: EstadoIntegracion
  configurada: boolean
  capacidades: CapacidadIntegracion[]
  ultimoError: string | null
  ultimaSincronizacion: string | null
  webhookToken: string | null
}

export type IntegracionConfigRequest = {
  activo: boolean
  credenciales: Record<string, string>
  configuracion: Record<string, string>
}

export type IntegracionEvento = {
  id: number
  proveedor: string
  tipo: string
  estado: string
  intentos: number
  proximoIntento: string | null
  ultimoError: string | null
  creadoEn: string
}

export type ResultadoIntegracion = {
  exitoso: boolean
  mensaje: string
  idExterno: string | null
  estado: string
  detalles: Record<string, string>
}

export type TipoSincronizacionIntegracion = 'CLIENTE' | 'PRODUCTO' | 'FACTURA'

export async function listarIntegraciones(empresaId: number) {
  const { data } = await api.get<IntegracionConfig[]>(`/empresas/${empresaId}/integraciones`)
  return data
}

export async function guardarIntegracion(
  empresaId: number,
  proveedor: ProveedorIntegracion,
  solicitud: IntegracionConfigRequest,
) {
  const { data } = await api.put<IntegracionConfig>(
    `/empresas/${empresaId}/integraciones/${proveedor}`,
    solicitud,
    { skipGlobalError: true },
  )
  return data
}

export async function probarIntegracion(empresaId: number, proveedor: ProveedorIntegracion) {
  const { data } = await api.post<ResultadoIntegracion>(
    `/empresas/${empresaId}/integraciones/${proveedor}/probar`,
    undefined,
    { skipGlobalError: true },
  )
  return data
}

export async function sincronizarIntegracion(
  empresaId: number,
  proveedor: ProveedorIntegracion,
  tipo: TipoSincronizacionIntegracion,
  datos: Record<string, unknown> = {},
) {
  const { data } = await api.post<ResultadoIntegracion>(
    `/empresas/${empresaId}/integraciones/${proveedor}/sincronizar`,
    { tipo, datos },
    { skipGlobalError: true },
  )
  return data
}

export async function listarEventosIntegracion(empresaId: number) {
  const { data } = await api.get<IntegracionEvento[]>(`/empresas/${empresaId}/integraciones/eventos`)
  return data
}

export async function reintentarEventoIntegracion(empresaId: number, eventoId: number) {
  const { data } = await api.post<IntegracionEvento>(
    `/empresas/${empresaId}/integraciones/eventos/${eventoId}/reintentar`,
    undefined,
    { skipGlobalError: true },
  )
  return data
}

export type DocumentoRadian = {
  id: number
  empresaId: number
  direccion: string
  tipoDocumento: string
  cufeCude: string
  consecutivo: string
  nitContraparte: string
  razonSocialContraparte: string
  fecha: string
  subtotal: number
  impuestos: number
  retenciones: number
  total: number
  estadoRadian: string
  estadoCausacion: string
  estadoContabilizacion: string
  asientoContableId: number | null
  createdAt: string
}

export type TraerDianTokenRequest = {
  token: string
  direccion?: string
  fechaDesde: string
  fechaHasta: string
  grupo?: string
  nitTercero?: string
  guardarToken?: boolean
}

export type ResultadoTraerDian = {
  origen: 'TOKEN' | 'ARCHIVO'
  archivo: string
  documentos: number
  mensaje: string
  registros: DocumentoRadian[]
}

export async function traerDocumentosDianPorToken(empresaId: number, solicitud: TraerDianTokenRequest) {
  const { data } = await api.post<ResultadoTraerDian>(
    `/empresas/${empresaId}/dian/documentos/token`,
    solicitud,
  )
  return data
}

export async function traerDocumentosDianPorArchivo(
  empresaId: number,
  archivo: File,
  fechaDesde: string,
  fechaHasta: string,
  direccion?: string,
  grupo?: string,
  nitTercero?: string,
) {
  const form = new FormData()
  form.append('archivo', archivo)
  form.append('fechaDesde', fechaDesde)
  form.append('fechaHasta', fechaHasta)
  if (direccion) form.append('direccion', direccion)
  if (grupo) form.append('grupo', grupo)
  if (nitTercero) form.append('nitTercero', nitTercero)
  const { data } = await api.post<ResultadoTraerDian>(
    `/empresas/${empresaId}/dian/documentos/archivo`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return data
}

export type AsientoContableLinea = {
  id: number
  codigoCuenta: string
  nombreCuenta: string
  descripcion: string | null
  debito: number
  credito: number
}

export type AsientoContable = {
  id: number
  numero: string
  fecha: string
  tipoEvento: string
  descripcion: string
  totalDebito: number
  totalCredito: number
  lineas: AsientoContableLinea[]
}

export type ConciliacionLinea = {
  documentoId: number
  consecutivo: string
  asientoId: number | null
  resultado: string
  severidad: string
  campo: string
  valorDocumento: number
  valorAsiento: number | null
  diferencia: number
  mensaje: string
}

export type ResultadoConciliacion = {
  conciliacionId: number
  empresaId: number
  fechaDesde: string | null
  fechaHasta: string | null
  totalDocumentos: number
  coincidencias: number
  diferencias: number
  documentosSinAsiento: number
  valorDiferencia: number
  lineas: ConciliacionLinea[]
}

export type MovimientoContable = {
  codigoCuenta: string
  nombreCuenta: string
  fecha: string
  numeroAsiento: string
  descripcion: string | null
  debito: number
  credito: number
  saldo: number
  centroCostoId: number | null
  terceroId: number | null
}

export type SaldoCuenta = {
  codigo: string
  nombre: string
  tipo: string
  naturaleza: string
  saldoInicial: number
  debito: number
  credito: number
  saldoFinal: number
}

export type BalanceComprobacion = {
  cuentas: SaldoCuenta[]
  totalSaldoInicial: number
  totalDebito: number
  totalCredito: number
  totalSaldoFinal: number
}

export type GrupoEstado = {
  nombre: string
  cuentas: SaldoCuenta[]
  total: number
}

export type EstadoFinanciero = {
  titulo: string
  fechaDesde: string | null
  fechaHasta: string | null
  grupos: GrupoEstado[]
  total: number
}

export type FlujoEfectivo = {
  fechaDesde: string | null
  fechaHasta: string | null
  movimientos: MovimientoContable[]
  entradas: number
  salidas: number
  neto: number
}

export type TarifaImpuesto = {
  id: number
  impuestoCodigo: string
  nombre: string
  anio: number
  conceptoCodigo: string
  porcentaje: number
  baseMinUvt: number
  baseMaxUvt: number
  activo: boolean
}

export type ResponsabilidadFiscal = {
  id: number
  empresaId: number
  terceroId: number | null
  impuestoCodigo: string
  codigo: string
  desde: string
  hasta: string | null
}

export type CausacionImpuesto = {
  id: number
  empresaId: number
  impuestoCodigo: string
  origen: string
  origenId: number
  terceroId: number | null
  periodo: string
  fecha: string
  conceptoCodigo: string
  baseImponible: number
  tarifa: number
  valor: number
  estado: string
  valorPagado: number
  observaciones: string | null
}

export type DeclaracionImpuesto = {
  id: number
  empresaId: number
  impuestoCodigo: string
  anioGravable: number
  periodo: string
  fechaDesde: string
  fechaHasta: string
  fechaLimite: string
  estado: string
  totalCausado: number
  totalPagado: number
  saldo: number
}

export type CalendarioTributario = {
  id: number
  impuestoCodigo: string
  anioGravable: number
  periodicidad: string
  periodo: string
  fechaLimite: string
}

export type ResultadoCalculoImpuesto = {
  impuestoCodigo: string
  nombreImpuesto: string
  anio: number
  conceptoCodigo: string
  base: number
  uvt: number
  baseMinima: number
  baseMaxima: number
  baseGravable: number
  tarifa: number
  aplica: boolean
  valor: number
}

export type ConceptoExogena = {
  formato: string
  codigo: string
  descripcion: string
  codCpt: number
}

export type SolicitudGeneracionExogena = {
  formato?: string
  anioGravable: number
  numeroEnvio: number
  fechaEnvio: string
  fechaInicial: string
  fechaFinal: string
  concepto: string
  encabezados: string[]
  filas: Record<string, string>[]
}

export type ResultadoGeneracionExogena = {
  formato: string
  version: string
  descripcion: string
  registros: number
  xml: string
  validacion: string
  prevalidacionOficial: boolean
}

export type ResultadoValidacionExogena = {
  formato: string
  valido: boolean
  mensaje: string
  prevalidacionOficial: boolean
}

export type MapeoColumna = {
  campo: string
  cabeceraOrigen: string
  indice: number
  confianza: number
  formatoDetectado: string
}

export type VistaPreviaArchivo = {
  archivoId: string
  nombreArchivo: string
  formatoDetectado: string
  cabeceras: string[]
  filasEjemplo: string[][]
  sugerencias: MapeoColumna[]
  confianza: number
  totalFilas: number
  contenidoBase64: string
}

export type RespuestaVistaPrevia = VistaPreviaArchivo

export type ConfirmacionImportacion = {
  archivoId: string | null
  nombreArchivo: string
  formatoDetectado: string
  contenidoBase64: string | null
  columnas: MapeoColumna[]
}

export type ResultadoImportacionUniversal = {
  filasLeidas: number
  filasImportadas: number
  filasOmitidas: number
  errores: number
  mensajes: string[]
}

export type ResultadoCausacionRadian = {
  documentoId: number
  causacionId: number
  estado: string
  fechaCausacion: string
  total: number
  mensaje: string | null
}

export type ItemMigracionContable = {
  id: number
  etapa: string
  externalId: string
  estado: string
  error: string | null
  payload: string | null
  createdAt: string
}

export type MigracionContableDto = {
  id: number
  proveedor: ProveedorIntegracion
  etapas: string[]
  estado: string
  exitosos: number
  omitidos: number
  errores: number
  total: number
  mensaje: string | null
  createdAt: string
}

export type ResultadoMigracionDto = {
  exitosos: number
  omitidos: number
  errores: number
  total: number
  items: ItemMigracionContable[]
}

export type Branding = {
  empresaId: number
  publicSlug: string
  displayName: string | null
  logoUrl: string | null
  primaryColor: string
  accentColor: string
  fontFamily: string
  invoiceFooter: string | null
  whatsappNumber: string | null
  whatsappTemplate: string | null
  pwaThemeColor: string
  pwaBackgroundColor: string
}

export type BrandingRequest = Pick<
  Branding,
  | 'publicSlug'
  | 'displayName'
  | 'logoUrl'
  | 'primaryColor'
  | 'accentColor'
  | 'fontFamily'
  | 'invoiceFooter'
  | 'whatsappNumber'
  | 'whatsappTemplate'
  | 'pwaThemeColor'
  | 'pwaBackgroundColor'
>

export type EntregaWhatsAppResponse = {
  empresaId: number
  facturaId: number
  numero: string
  enlace: string
  mensaje: string
  enviado: boolean
}

export async function listarDocumentosRadian(empresaId: number) {
  const { data } = await api.get<DocumentoRadian[]>(`/empresas/${empresaId}/radian/documentos`)
  return data
}

export async function importarDocumentosRadian(empresaId: number, archivo: File, direccion?: string) {
  const form = new FormData()
  form.append('archivo', archivo)
  if (direccion) form.append('direccion', direccion)
  const { data } = await api.post<DocumentoRadian[]>(
    `/empresas/${empresaId}/radian/documentos/importar`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return data
}

export async function causarDocumentoRadian(empresaId: number, documentoId: number) {
  const { data } = await api.post<ResultadoCausacionRadian>(
    `/empresas/${empresaId}/radian/documentos/${documentoId}/causar`,
    {},
  )
  return data
}

export async function contabilizarDocumentoRadian(empresaId: number, documentoId: number) {
  const { data } = await api.post<AsientoContable>(
    `/empresas/${empresaId}/radian/documentos/${documentoId}/contabilizar`,
  )
  return data
}

export async function listarAsientosContables(empresaId: number) {
  const { data } = await api.get<AsientoContable[]>(`/empresas/${empresaId}/contabilidad/asientos`)
  return data
}

export type FiltrosConciliacion = {
  fechaDesde?: string
  fechaHasta?: string
  direccion?: string
  tipoDocumento?: string
}

export async function conciliarContabilidad(empresaId: number, filtros: FiltrosConciliacion = {}) {
  const params = new URLSearchParams()
  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor) params.set(clave, valor)
  })
  const sufijo = params.size > 0 ? `?${params.toString()}` : ''
  const { data } = await api.post<ResultadoConciliacion>(
    `/empresas/${empresaId}/contabilidad/conciliacion${sufijo}`,
  )
  return data
}

export type FiltrosReporteContable = {
  fechaDesde?: string
  fechaHasta?: string
  codigoCuenta?: string
  centroCostoId?: number
  terceroId?: number
  pagina?: number
  tamano?: number
}

function paramsReporte(filtros: FiltrosReporteContable) {
  const params = new URLSearchParams()
  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor != null && valor !== '') params.set(clave, String(valor))
  })
  return params
}

export async function listarLibroDiario(empresaId: number, filtros: FiltrosReporteContable = {}) {
  const { data } = await api.get<Pagina<AsientoContable>>(
    `/empresas/${empresaId}/contabilidad/reportes/libro-diario?${paramsReporte(filtros)}`,
  )
  return data
}

export async function listarLibroMayor(empresaId: number, filtros: FiltrosReporteContable = {}) {
  const { data } = await api.get<Pagina<MovimientoContable>>(
    `/empresas/${empresaId}/contabilidad/reportes/libro-mayor?${paramsReporte(filtros)}`,
  )
  return data
}

export async function listarAuxiliaresContables(empresaId: number, filtros: FiltrosReporteContable = {}) {
  const { data } = await api.get<Pagina<MovimientoContable>>(
    `/empresas/${empresaId}/contabilidad/reportes/auxiliares?${paramsReporte(filtros)}`,
  )
  return data
}

export async function obtenerBalanceComprobacion(
  empresaId: number,
  fechaDesde?: string,
  fechaHasta?: string,
) {
  const filtros: FiltrosReporteContable = { fechaDesde, fechaHasta }
  const { data } = await api.get<BalanceComprobacion>(
    `/empresas/${empresaId}/contabilidad/reportes/balance-comprobacion?${paramsReporte(filtros)}`,
  )
  return data
}

export async function obtenerBalanceGeneral(empresaId: number, fechaCorte: string) {
  const { data } = await api.get<EstadoFinanciero>(
    `/empresas/${empresaId}/contabilidad/reportes/balance-general?fechaCorte=${fechaCorte}`,
  )
  return data
}

export async function obtenerEstadoResultados(
  empresaId: number,
  fechaDesde: string,
  fechaHasta: string,
) {
  const filtros: FiltrosReporteContable = { fechaDesde, fechaHasta }
  const { data } = await api.get<EstadoFinanciero>(
    `/empresas/${empresaId}/contabilidad/reportes/estado-resultados?${paramsReporte(filtros)}`,
  )
  return data
}

export async function obtenerFlujoEfectivo(empresaId: number, fechaDesde: string, fechaHasta: string) {
  const filtros: FiltrosReporteContable = { fechaDesde, fechaHasta }
  const { data } = await api.get<FlujoEfectivo>(
    `/empresas/${empresaId}/contabilidad/reportes/flujo-efectivo?${paramsReporte(filtros)}`,
  )
  return data
}

export async function listarTarifasImpuesto(empresaId: number, anio?: number) {
  const sufijo = anio != null ? `?anio=${anio}` : ''
  const { data } = await api.get<TarifaImpuesto[]>(`/empresas/${empresaId}/impuestos/tarifas${sufijo}`)
  return data
}

export async function listarResponsabilidadesFiscales(empresaId: number) {
  const { data } = await api.get<ResponsabilidadFiscal[]>(
    `/empresas/${empresaId}/impuestos/responsabilidades`,
  )
  return data
}

export async function listarCausacionesImpuesto(empresaId: number, periodo?: string) {
  const sufijo = periodo ? `?periodo=${periodo}` : ''
  const { data } = await api.get<CausacionImpuesto[]>(
    `/empresas/${empresaId}/impuestos/causaciones${sufijo}`,
  )
  return data
}

export async function listarDeclaracionesImpuesto(empresaId: number, anio?: number) {
  const sufijo = anio != null ? `?anio=${anio}` : ''
  const { data } = await api.get<DeclaracionImpuesto[]>(
    `/empresas/${empresaId}/impuestos/declaraciones${sufijo}`,
  )
  return data
}

export async function listarCalendarioTributario(empresaId: number, anio?: number) {
  const sufijo = anio != null ? `?anio=${anio}` : ''
  const { data } = await api.get<CalendarioTributario[]>(
    `/empresas/${empresaId}/impuestos/calendario${sufijo}`,
  )
  return data
}

export async function calcularImpuesto(
  empresaId: number,
  solicitud: { impuestoCodigo: string; anio: number; conceptoCodigo: string; base: number },
) {
  const { data } = await api.post<ResultadoCalculoImpuesto>(
    `/empresas/${empresaId}/impuestos/calcular`,
    solicitud,
  )
  return data
}

export async function listarFormatosExogena(empresaId: number) {
  const { data } = await api.get<FormatoExogena[]>(`/empresas/${empresaId}/exogena/formatos`)
  return data
}

export async function listarConceptosExogena(empresaId: number, formato: string) {
  const { data } = await api.get<ConceptoExogena[]>(
    `/empresas/${empresaId}/exogena/formatos/${encodeURIComponent(formato)}/conceptos`,
  )
  return data
}

export async function generarExogena(
  empresaId: number,
  formato: string,
  solicitud: SolicitudGeneracionExogena,
) {
  const { data } = await api.post<ResultadoGeneracionExogena>(
    `/empresas/${empresaId}/exogena/${encodeURIComponent(formato)}/generar`,
    solicitud,
  )
  return data
}

export async function validarXmlExogena(empresaId: number, formato: string, xml: string) {
  const { data } = await api.post<ResultadoValidacionExogena>(
    `/empresas/${empresaId}/exogena/${encodeURIComponent(formato)}/validar-xml`,
    { xml },
  )
  return data
}

export async function migrarContabilidad(empresaId: number, proveedor: ProveedorIntegracion) {
  const { data } = await api.post<MigracionContableDto>(
    `/empresas/${empresaId}/integraciones/${proveedor}/migrar-contabilidad`,
  )
  return data
}

export async function obtenerReporteMigracion(
  empresaId: number,
  proveedor: ProveedorIntegracion,
  migracionId: number,
) {
  const { data } = await api.get<ResultadoMigracionDto>(
    `/empresas/${empresaId}/integraciones/${proveedor}/migraciones/${migracionId}/reporte`,
  )
  return data
}

export async function reintentarMigracion(
  empresaId: number,
  proveedor: ProveedorIntegracion,
  migracionId: number,
) {
  const { data } = await api.post<MigracionContableDto>(
    `/empresas/${empresaId}/integraciones/${proveedor}/migraciones/${migracionId}/reintentar`,
  )
  return data
}

export async function vistaPreviaArchivo(empresaId: number, archivo: File) {
  const form = new FormData()
  form.append('archivo', archivo)
  const { data } = await api.post<VistaPreviaArchivo>(
    `/empresas/${empresaId}/importaciones/vista-previa`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return data
}

export async function confirmarImportacion(empresaId: number, solicitud: ConfirmacionImportacion) {
  const { data } = await api.post<ResultadoImportacionUniversal>(
    `/empresas/${empresaId}/importaciones/confirmar`,
    solicitud,
  )
  return data
}

export async function listarMapeos(empresaId: number) {
  const { data } = await api.get<MapeoColumna[]>(
    `/empresas/${empresaId}/importaciones/mapeos`,
  )
  return data
}

export async function entregarFacturaWhatsapp(
  empresaId: number,
  facturaId: number,
  numero?: string,
  mensaje?: string,
) {
  const { data } = await api.post<EntregaWhatsAppResponse>(
    `/empresas/${empresaId}/facturacion/${facturaId}/entregar-whatsapp`,
    { numero, mensaje },
  )
  return data
}

export async function obtenerBranding(empresaId: number) {
  const { data } = await api.get<Branding>(`/empresas/${empresaId}/branding`, {
    skipGlobalError: true,
  })
  return data
}

export async function guardarBranding(empresaId: number, solicitud: BrandingRequest) {
  const { data } = await api.put<Branding>(`/empresas/${empresaId}/branding`, solicitud)
  return data
}

export async function obtenerBrandingPublico(publicSlug: string) {
  const { data } = await api.get<Branding>(`/branding/${encodeURIComponent(publicSlug)}`, {
    skipGlobalError: true,
  })
  return data
}

export type ModoOperacionDian =
  | 'PROPIO_INTERNO'
  | 'ADQUIRIDO_CLIENTE'
  | 'PT_MAGNETIXDIAN'
  | 'PT_EXTERNO'

export type EstadoHabilitacionDian =
  | 'BORRADOR'
  | 'TEST_SET_EN_CURSO'
  | 'TEST_SET_APROBADO'
  | 'PRODUCCION'
  | 'RECHAZADO'

export type AuditoriaDian = {
  id: number
  accion: string
  detalle: string
  usuario: string | null
  createdAt: string
}

export type HabilitacionDian = {
  empresaId: number
  modo: ModoOperacionDian
  estado: EstadoHabilitacionDian
  resolucionNumero: string | null
  resolucionFecha: string | null
  prefijo: string | null
  rangoDesde: number | null
  rangoHasta: number | null
  testSetId: string | null
  softwareId: string | null
  notificarAdquirente: boolean
  certificadoNombre: string | null
  switchoverEn: string | null
  aprobadoEn: string | null
  auditoria: AuditoriaDian[]
}

export type SolicitudHabilitacionDian = {
  modo: ModoOperacionDian
  resolucionNumero: string
  resolucionFecha: string
  prefijo: string
  rangoDesde: number | null
  rangoHasta: number | null
  testSetId: string
  softwareId: string
  notificarAdquirente: boolean
}

export type DocumentoTestDian = {
  trackId: string
  tipoDocumento: string
  descripcion: string
  estado: 'PENDIENTE' | 'ENVIADO' | 'APROBADO' | 'RECHAZADO'
  mensaje: string | null
}

export type ResultadoTestSetDian = {
  testSetId: string
  estado: EstadoHabilitacionDian
  total: number
  aprobados: number
  rechazados: number
  documentos: DocumentoTestDian[]
}

export type SolicitudSwitchoverDian = {
  confirmacion: 'APRUEBO_SWITCHOVER'
  testSetId: string
  resolucionNumero: string
  observaciones?: string
}

export type PagoCheckoutRequest = {
  planCodigo: string
  proveedor: string
  metodo: MetodoPago
  empresaId?: number
  email?: string
}

export type CheckoutBillingRequest = {
  planCodigo: string
  proveedor: string
  correo: string
  empresaId?: number
}

export type CheckoutBillingResponse = {
  checkoutUrl: string
  referencia: string
  monto: number
  moneda: string
  estado: string
}

export type EstadoWebhookPago = {
  referencia: string
  proveedor: string
  estado: string
  firmado: boolean
  eventId: string | null
  procesadoEn: string | null
  comprobanteNumero: string | null
  mensaje: string | null
}

export type VerificacionEmail = {
  empresaId: number | null
  estadoTenant: string
  emailVerificado: boolean
  mensaje: string
}

export type ReenviarVerificacionResponse = {
  enviado: boolean
  mensaje: string
  token: string | null
}

export type Invitacion = {
  id: number
  email: string
  empresaId: number
  rol: string
  estado: string
  expiraEn: string | null
}

export type CuentaPuc = {
  codigo: string
  nombre: string
  naturaleza: 'DEBITO' | 'CREDITO'
  tipo: string
  activa: boolean
}

export type PeriodoContable = {
  id: number
  anio: number
  mes: number
  estado: 'ABIERTO' | 'CERRADO'
  fechaCierre: string | null
}

export type AsientoFacturaRequest = {
  facturaId: number
  nota: string | null
}

export type DetalleValidacionExogena = {
  formato: string
  campo: string
  mensaje: string
  severidad: 'ERROR' | 'ADVERTENCIA' | 'INFO'
}

export type ResultadoFormatoExogena = {
  formato: string
  descripcion: string
  registros: number
  xml: string
  excel: string | null
  validacion: string
  errores: DetalleValidacionExogena[]
  prevalidacionOficial: false
}

export type SolicitudGenerarTodoExogena = {
  anioGravable: number
  numeroEnvio: number
  fechaEnvio: string
  fechaInicial: string
  fechaFinal: string
  formatos: string[]
}

export type ResultadoGenerarTodoExogena = {
  anioGravable: number
  totalFormatos: number
  resultados: ResultadoFormatoExogena[]
  prevalidacionOficial: false
}

export type OrdenVentaErpRequest = {
  numero: string
  clienteId: string | null
  clienteNombre: string
  fecha: string
  subtotal: number
  impuestos: number
  total: number
  moneda: string | null
  observaciones: string | null
}

export type OrdenVentaErp = {
  id: number
  empresaId: number
  numero: string
  clienteId: string | null
  clienteNombre: string
  fecha: string
  estado: string
  subtotal: number
  impuestos: number
  total: number
  moneda: string | null
  observaciones: string | null
  createdAt: string
  updatedAt: string
}

export type PedidoErpRequest = {
  numero: string
  ordenVentaId: number
  clienteId: string | null
  clienteNombre: string
  fecha: string
  subtotal: number
  impuestos: number
  total: number
  observaciones: string | null
}

export type PedidoErp = {
  id: number
  empresaId: number
  ordenVentaId: number
  consecutivo: string
  clienteId: string | null
  cliente: string
  estado: string
  subtotal: number
  impuestos: number
  total: number
  fecha: string
  facturaDianId: string | null
  observaciones: string | null
}

export type CompraErpRequest = {
  numero: string
  proveedorId: string | null
  proveedorNombre: string
  fecha: string
  subtotal: number
  impuestos: number
  total: number
  observaciones: string | null
}

export type CompraErp = {
  id: number
  empresaId: number
  numero: string
  proveedorId: string | null
  proveedorNombre: string
  fecha: string
  estado: string
  subtotal: number
  impuestos: number
  total: number
  observaciones: string | null
  createdAt: string
  updatedAt: string
}

export type RecepcionLineaErpRequest = {
  productoId: string
  descripcion: string | null
  cantidad: number
  costoUnitario: number
}

export type RecepcionErpRequest = {
  numero: string
  fecha: string
  bodega: string
  observaciones: string | null
  lineas: RecepcionLineaErpRequest[]
}

export type RecepcionLineaErp = {
  id: number
  productoId: string
  descripcion: string | null
  cantidad: number
  costoUnitario: number
  total: number
}

export type RecepcionErp = {
  id: number
  empresaId: number
  compraId: number
  numero: string
  fecha: string
  bodega: string
  estado: string
  total: number
  observaciones: string | null
  createdAt: string
  updatedAt: string
  lineas: RecepcionLineaErp[]
}

export type MovimientoInventarioErpRequest = {
  tipo: 'ENTRADA' | 'SALIDA'
  bodega: string
  productoId: string
  descripcion: string | null
  cantidad: number
  costoUnitario: number | null
  fecha: string
  referenciaTipo: string | null
  referenciaId: string | null
  observaciones: string | null
}

export type MovimientoInventarioErp = {
  id: number
  empresaId: number
  tipo: string
  bodega: string
  productoId: string
  descripcion: string | null
  cantidad: number
  costoUnitario: number | null
  costoPromedio: number
  existenciaAnterior: number
  existenciaNueva: number
  fecha: string
  referenciaTipo: string | null
  referenciaId: string | null
  estado: string
  observaciones: string | null
  createdAt: string
}

export type ExistenciaInventarioErp = {
  empresaId: number
  bodega: string
  productoId: string
  existencia: number
  costoPromedio: number
  updatedAt: string
}

export type CuentaBancariaErpRequest = {
  banco: string
  numeroCuenta: string
  tipoCuenta: string
  moneda: string
  saldoInicial: number
  activa: boolean
}

export type CuentaBancariaErp = {
  id: number
  empresaId: number
  banco: string
  numeroCuenta: string
  tipoCuenta: string
  moneda: string
  saldo: number
  activa: boolean
  estado: string
  createdAt: string
  updatedAt: string
}

export type PagoProveedorErpRequest = {
  compraId: number | null
  cuentaBancariaId: number
  proveedorId: string | null
  proveedorNombre: string | null
  fecha: string
  valor: number
  medioPago: string | null
  referencia: string | null
  observaciones: string | null
}

export type PagoProveedorErp = {
  id: number
  empresaId: number
  compraId: number | null
  cuentaBancariaId: number
  proveedorId: string | null
  proveedorNombre: string | null
  fecha: string
  valor: number
  medioPago: string | null
  referencia: string | null
  estado: string
  observaciones: string | null
  createdAt: string
}

export type CobroClienteErpRequest = {
  pedidoId: number | null
  cuentaBancariaId: number
  clienteId: string | null
  clienteNombre: string | null
  fecha: string
  valor: number
  medioPago: string | null
  referencia: string | null
  observaciones: string | null
}

export type CobroClienteErp = {
  id: number
  empresaId: number
  pedidoId: number | null
  cuentaBancariaId: number
  clienteId: string | null
  clienteNombre: string | null
  fecha: string
  valor: number
  medioPago: string | null
  referencia: string | null
  estado: string
  observaciones: string | null
  createdAt: string
}

export type MovimientoTesoreriaErp = {
  id: number
  empresaId: number
  cuentaBancariaId: number
  tipo: string
  fecha: string
  valor: number
  saldoAnterior: number
  saldoNuevo: number
  referenciaTipo: string | null
  referenciaId: string | null
  estado: string
  descripcion: string | null
  createdAt: string
}

export async function crearCheckoutBilling(solicitud: CheckoutBillingRequest) {
  const { data } = await api.post<CheckoutBillingResponse>('/pagos/checkout', solicitud, {
    skipGlobalError: true,
  })
  return data
}

export async function estadoWebhookPago(referencia: string) {
  const { data } = await api.get<EstadoWebhookPago>(
    `/pagos/${encodeURIComponent(referencia)}/webhook-estado`,
    { skipGlobalError: true },
  )
  return data
}

export async function verificarEmail(token: string) {
  const { data } = await api.get<VerificacionEmail>(
    `/registro/verificar?token=${encodeURIComponent(token)}`,
    { skipGlobalError: true },
  )
  return data
}

export async function reenviarVerificacionEmail(email: string) {
  const { data } = await api.post<ReenviarVerificacionResponse>(
    '/registro/reenviar',
    { email },
    { skipGlobalError: true },
  )
  return data
}

export async function listarInvitaciones(empresaId: number) {
  const { data } = await api.get<Invitacion[]>(`/empresas/${empresaId}/invitaciones`)
  return data
}

export async function obtenerConfigFeEmpresa(empresaId: number) {
  const { data } = await api.get<ConfigFe>(`/facturacion/config/${empresaId}`)
  return data
}

export async function guardarConfigFeEmpresa(empresaId: number, solicitud: ConfigFe) {
  const { data } = await api.put<ConfigFe>(`/facturacion/config/${empresaId}`, solicitud)
  return data
}

export async function obtenerHabilitacionDian(empresaId: number) {
  const { data } = await api.get<HabilitacionDian>(`/empresas/${empresaId}/dian/habilitacion`)
  return data
}

export async function guardarHabilitacionDian(empresaId: number, solicitud: SolicitudHabilitacionDian) {
  const { data } = await api.put<HabilitacionDian>(
    `/empresas/${empresaId}/dian/habilitacion`,
    solicitud,
  )
  return data
}

export async function listarDocumentosTestDian(empresaId: number) {
  const { data } = await api.get<DocumentoTestDian[]>(`/empresas/${empresaId}/dian/test-set/documentos`)
  return data
}

export async function enviarDocumentosTestSet(empresaId: number, documentos: DocumentoTestDian[]) {
  const { data } = await api.post<ResultadoTestSetDian>(
    `/empresas/${empresaId}/dian/test-set/documentos`,
    { documentos },
  )
  return data
}

export async function consultarEstadoTestSet(empresaId: number, testSetId: string) {
  const { data } = await api.get<ResultadoTestSetDian>(
    `/empresas/${empresaId}/dian/test-set/${encodeURIComponent(testSetId)}`,
  )
  return data
}

export async function switchoverProduccion(empresaId: number, solicitud: SolicitudSwitchoverDian) {
  const { data } = await api.post<HabilitacionDian>(
    `/empresas/${empresaId}/dian/switchover`,
    solicitud,
  )
  return data
}

export async function listarAuditoriaDian(empresaId: number) {
  const { data } = await api.get<AuditoriaDian[]>(`/empresas/${empresaId}/dian/auditoria`)
  return data
}

export async function listarCuentasPuc(empresaId: number) {
  const { data } = await api.get<CuentaPuc[]>(`/empresas/${empresaId}/contabilidad/puc`)
  return data
}

export async function listarPeriodosContables(empresaId: number) {
  const { data } = await api.get<PeriodoContable[]>(`/empresas/${empresaId}/contabilidad/periodos`)
  return data
}

export async function asentarFactura(empresaId: number, solicitud: AsientoFacturaRequest) {
  const { data } = await api.post<AsientoContable>(
    `/empresas/${empresaId}/contabilidad/asientos`,
    solicitud,
  )
  return data
}

export async function generarTodoExogena(empresaId: number, solicitud: SolicitudGenerarTodoExogena) {
  const { data } = await api.post<ResultadoGenerarTodoExogena>(
    `/empresas/${empresaId}/exogena/generar`,
    solicitud,
  )
  return data
}

export async function crearOrdenVentaErp(empresaId: number, solicitud: OrdenVentaErpRequest) {
  const { data } = await api.post<OrdenVentaErp>(`/empresas/${empresaId}/erp/ventas/ordenes`, solicitud)
  return data
}

export async function crearPedidoErp(empresaId: number, solicitud: PedidoErpRequest) {
  const { data } = await api.post<PedidoErp>(`/empresas/${empresaId}/erp/ventas/pedidos`, solicitud)
  return data
}

export async function aprobarPedidoErp(empresaId: number, pedidoId: number) {
  const { data } = await api.post<PedidoErp>(`/empresas/${empresaId}/erp/ventas/pedidos/${pedidoId}/aprobar`)
  return data
}

export async function emitirFactura(empresaId: number, solicitud: FacturaRequest) {
  const { data } = await api.post<ResultadoEmision>(`/facturacion/${empresaId}/emitir`, solicitud)
  return data
}

export async function listarPedidosErp(empresaId: number) {
  const { data } = await api.get<PedidoErp[]>(`/empresas/${empresaId}/erp/ventas/pedidos`)
  return data
}

export async function listarComprasErp(empresaId: number) {
  const { data } = await api.get<CompraErp[]>(`/empresas/${empresaId}/erp/compras/ordenes`)
  return data
}

export async function listarInventarioErp(empresaId: number) {
  const { data } = await api.get<MovimientoInventarioErp[]>(`/empresas/${empresaId}/erp/inventario/movimientos`)
  return data
}

export async function listarTesoreriaErp(empresaId: number) {
  const { data } = await api.get<MovimientoTesoreriaErp[]>(`/empresas/${empresaId}/erp/tesoreria/movimientos`)
  return data
}

export async function crearCompraErp(empresaId: number, solicitud: CompraErpRequest) {
  const { data } = await api.post<CompraErp>(`/empresas/${empresaId}/erp/compras/ordenes`, solicitud)
  return data
}

export async function recibirCompraErp(
  empresaId: number,
  compraId: number,
  solicitud: RecepcionErpRequest,
) {
  const { data } = await api.post<RecepcionErp>(
    `/empresas/${empresaId}/erp/compras/ordenes/${compraId}/recepciones`,
    solicitud,
  )
  return data
}

export async function listarRecepcionesErp(empresaId: number) {
  const { data } = await api.get<RecepcionErp[]>(`/empresas/${empresaId}/erp/compras/recepciones`)
  return data
}

export async function registrarMovimientoInventarioErp(
  empresaId: number,
  solicitud: MovimientoInventarioErpRequest,
) {
  const { data } = await api.post<MovimientoInventarioErp>(
    `/empresas/${empresaId}/erp/inventario/movimientos`,
    solicitud,
  )
  return data
}

export async function listarExistenciasErp(empresaId: number) {
  const { data } = await api.get<ExistenciaInventarioErp[]>(`/empresas/${empresaId}/erp/inventario/existencias`)
  return data
}

export async function crearCuentaBancariaErp(
  empresaId: number,
  solicitud: CuentaBancariaErpRequest,
) {
  const { data } = await api.post<CuentaBancariaErp>(
    `/empresas/${empresaId}/erp/tesoreria/cuentas-bancarias`,
    solicitud,
  )
  return data
}

export async function listarCuentasBancariasErp(empresaId: number) {
  const { data } = await api.get<CuentaBancariaErp[]>(`/empresas/${empresaId}/erp/tesoreria/cuentas-bancarias`)
  return data
}

export async function registrarPagoProveedorErp(
  empresaId: number,
  solicitud: PagoProveedorErpRequest,
) {
  const { data } = await api.post<PagoProveedorErp>(
    `/empresas/${empresaId}/erp/tesoreria/pagos-proveedor`,
    solicitud,
  )
  return data
}

export async function listarPagosProveedorErp(empresaId: number) {
  const { data } = await api.get<PagoProveedorErp[]>(`/empresas/${empresaId}/erp/tesoreria/pagos-proveedor`)
  return data
}

export async function registrarCobroClienteErp(
  empresaId: number,
  solicitud: CobroClienteErpRequest,
) {
  const { data } = await api.post<CobroClienteErp>(
    `/empresas/${empresaId}/erp/tesoreria/cobros-cliente`,
    solicitud,
  )
  return data
}

export async function listarCobrosClienteErp(empresaId: number) {
  const { data } = await api.get<CobroClienteErp[]>(`/empresas/${empresaId}/erp/tesoreria/cobros-cliente`)
  return data
}