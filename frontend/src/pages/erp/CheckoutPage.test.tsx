import { ThemeProvider } from '@mui/material/styles'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import theme from '../../theme/theme'
import { listarPlanes, type Plan } from '../../api/client'
import { crearCheckoutBilling, estadoWebhookPago } from '../../api/erp/pagos'
import CheckoutPage from './CheckoutPage'

vi.mock('../../api/client', () => ({
  listarPlanes: vi.fn(),
}))

vi.mock('../../api/erp/pagos', () => ({
  crearCheckoutBilling: vi.fn(),
  estadoWebhookPago: vi.fn(),
  PROVEEDORES_CHECKOUT: [
    { value: 'WOMPI', label: 'Wompi' },
    { value: 'MERCADOPAGO', label: 'Mercado Pago' },
  ],
}))

const mockListarPlanes = vi.mocked(listarPlanes)
const mockCrearCheckout = vi.mocked(crearCheckoutBilling)
const mockEstadoWebhook = vi.mocked(estadoWebhookPago)

const PLAN_PRO: Plan = {
  id: 2,
  codigo: 'PRO',
  nombre: 'Profesional',
  descripcion: 'Hasta 1.000 documentos al mes.',
  precioMensual: 99000,
  moneda: 'COP',
  limiteFacturasMes: 1000,
  limiteUsuarios: 5,
  orden: 2,
}

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={['/checkout?plan=PRO&email=cliente@ejemplo.com']}>
        <CheckoutPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('CheckoutPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockListarPlanes.mockResolvedValue([PLAN_PRO])
  })

  it('abre checkout con pasarela real y consulta el webhook firmado', async () => {
    mockCrearCheckout.mockResolvedValue({
      checkoutUrl: 'https://checkout.test/p/abc',
      referencia: 'WOMPI-REF',
      monto: 99000,
      moneda: 'COP',
      estado: 'PENDIENTE',
    })
    mockEstadoWebhook.mockResolvedValue({
      referencia: 'WOMPI-REF',
      proveedor: 'WOMPI',
      estado: 'APROBADO',
      firmado: true,
      eventId: 'evt-1',
      procesadoEn: '2026-01-01T00:00:00Z',
      comprobanteNumero: 'CP-000001',
      mensaje: 'Pago aprobado.',
    })
    const user = userEvent.setup()
    renderPage()

    await waitFor(() => expect(mockListarPlanes).toHaveBeenCalled())
    await user.click(screen.getByRole('button', { name: 'Continuar al pago' }))

    await waitFor(() =>
      expect(mockCrearCheckout).toHaveBeenCalledWith({
        planCodigo: 'PRO',
        proveedor: 'WOMPI',
        correo: 'cliente@ejemplo.com',
      }),
    )
    expect(await screen.findByText('Referencia: WOMPI-REF')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir al checkout de WOMPI' }))
      .toHaveAttribute('href', 'https://checkout.test/p/abc')

    await user.click(screen.getByRole('button', { name: 'Consultar estado del webhook' }))

    await waitFor(() => expect(mockEstadoWebhook).toHaveBeenCalledWith('WOMPI-REF'))
    expect(await screen.findByText(/firma válida/)).toBeInTheDocument()
    expect(screen.getByText(/comprobante CP-000001/)).toBeInTheDocument()
  })

  it('muestra el error explícito del backend cuando la pasarela no está configurada', async () => {
    mockCrearCheckout.mockRejectedValue({
      response: { data: { message: 'La pasarela WOMPI no está configurada.' } },
    })
    const user = userEvent.setup()
    renderPage()

    await waitFor(() => expect(mockListarPlanes).toHaveBeenCalled())
    await user.click(screen.getByRole('button', { name: 'Continuar al pago' }))

    expect(await screen.findByText('La pasarela WOMPI no está configurada.')).toBeInTheDocument()
  })
})
