import { ThemeProvider } from '@mui/material/styles'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import theme from '../theme/theme'
import SuscripcionPage from './SuscripcionPage'
import {
  cancelarSuscripcion,
  iniciarCheckout,
  listarApiKeys,
  listarPagos,
  listarPlanes,
  obtenerSuscripcion,
  type Plan,
  type Suscripcion,
} from '../api/client'

vi.mock('../features/auth/useEmpresaActiva', () => ({
  useEmpresaActiva: () => ({ empresaId: 1, cargando: false }),
}))

vi.mock('../api/client', () => ({
  listarPlanes: vi.fn(),
  obtenerSuscripcion: vi.fn(),
  iniciarCheckout: vi.fn(),
  cancelarSuscripcion: vi.fn(),
  listarApiKeys: vi.fn(),
  listarPagos: vi.fn(),
}))

const mockPlanes = vi.mocked(listarPlanes)
const mockSuscripcion = vi.mocked(obtenerSuscripcion)
const mockCheckout = vi.mocked(iniciarCheckout)
const mockCancelar = vi.mocked(cancelarSuscripcion)

const PLAN_FREE: Plan = {
  id: 1, codigo: 'FREE', nombre: 'Gratuito', descripcion: null, precioMensual: 0,
  moneda: 'COP', limiteFacturasMes: 10, limiteUsuarios: 1, orden: 1,
}

const PLAN_PRO: Plan = {
  id: 2, codigo: 'PRO', nombre: 'Profesional', descripcion: 'Más capacidad', precioMensual: 99000,
  moneda: 'COP', limiteFacturasMes: 500, limiteUsuarios: 5, orden: 2,
}

const SUSCRIPCION: Suscripcion = {
  id: 1, empresaId: 1, plan: PLAN_FREE, estado: 'ACTIVA', proveedor: 'SIMULADO',
  referenciaExterna: null, periodoInicio: '2026-01-01', periodoFin: null,
  uso: {
    facturasMes: 2, facturasRestantes: 8, facturasExcedidas: false,
    usuarios: 1, usuariosRestantes: 0, usuariosExcedidos: false,
  },
}

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <SuscripcionPage />
    </ThemeProvider>,
  )
}

describe('SuscripcionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockPlanes.mockResolvedValue([PLAN_FREE, PLAN_PRO])
    mockSuscripcion.mockResolvedValue(SUSCRIPCION)
    vi.mocked(listarApiKeys).mockResolvedValue([])
    vi.mocked(listarPagos).mockResolvedValue([])
  })

  it('muestra el plan actual y los planes disponibles', async () => {
    renderPage()
    expect(await screen.findByText('Plan Gratuito')).toBeInTheDocument()
    expect(screen.getByText('Actual')).toBeInTheDocument()
    expect(screen.getByText('Profesional')).toBeInTheDocument()
    expect(screen.getByText('Cambiar a este plan')).toBeInTheDocument()
  })

  it('cambia de plan con checkout inmediato y confirma', async () => {
    mockCheckout.mockResolvedValue({
      id: 9, referencia: 'SIMULADO-9', proveedor: 'SIMULADO', planCodigo: 'PRO',
      monto: 99000, moneda: 'COP', estado: 'APROBADO', url: null,
    })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cambiar a este plan' }))

    await waitFor(() => expect(mockCheckout).toHaveBeenCalledWith(1, 'PRO', 'SIMULADO'))
    expect(await screen.findByText('Pago aprobado. Plan PRO activado.')).toBeInTheDocument()
  })

  it('cancela la suscripción activa', async () => {
    mockCancelar.mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cancelar suscripción' }))

    await waitFor(() => expect(mockCancelar).toHaveBeenCalledWith(1))
    expect(await screen.findByText('Suscripción cancelada.')).toBeInTheDocument()
  })
})
