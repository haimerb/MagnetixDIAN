export {
  crearCheckoutEmpresa,
  estadoWebhookPago,
  obtenerPago as obtenerPagoErp,
  listarPagos,
  reconciliarPago,
} from '../client'
export type { EstadoWebhookPago, Pago, PagoCheckoutRequest } from '../client'

export const PASARELA_PRUEBA = 'SIMULADO'

export function esPagoAprobado(estado: string | null | undefined): boolean {
  return estado === 'APROBADO' || estado === 'APPROVED' || estado === 'PAID'
}
