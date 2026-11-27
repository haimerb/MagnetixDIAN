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
  MetodoPago,
  Pago,
  PagoCheckoutRequest,
} from '../client'

export const PROVEEDORES_CHECKOUT = [
  { value: 'WOMPI', label: 'Wompi' },
  { value: 'MERCADOPAGO', label: 'Mercado Pago' },
] as const

export type ProveedorCheckout = (typeof PROVEEDORES_CHECKOUT)[number]['value']

export const METODOS_CHECKOUT = [
  { value: 'CARD', label: 'Tarjeta de crédito o débito' },
  { value: 'NEQUI', label: 'Nequi' },
  { value: 'PSE', label: 'PSE' },
  { value: 'BREB_QR', label: 'BRE-B QR (Bancolombia)' },
  { value: 'DAVIPLATA', label: 'Daviplata' },
] as const

export function requiereDatosWompi(metodo: string): boolean {
  return metodo === 'BREB_QR' || metodo === 'DAVIPLATA'
}

export function esPagoAprobado(estado: string | null | undefined): boolean {
  return estado === 'APROBADO' || estado === 'APPROVED' || estado === 'PAID'
}
