export {
  crearCheckoutBilling,
  estadoWebhookPago,
  obtenerPago as obtenerPagoErp,
  listarPagos,
  reconciliarPago,
} from '../client'
export type {
  CheckoutBillingRequest,
  CheckoutBillingResponse,
  EstadoWebhookPago,
  Pago,
  PagoCheckoutRequest,
} from '../client'

export const PROVEEDORES_CHECKOUT = [
  { value: 'WOMPI', label: 'Wompi' },
  { value: 'MERCADOPAGO', label: 'Mercado Pago' },
] as const

export type ProveedorCheckout = (typeof PROVEEDORES_CHECKOUT)[number]['value']

export function esPagoAprobado(estado: string | null | undefined): boolean {
  return estado === 'APROBADO' || estado === 'APPROVED' || estado === 'PAID'
}
