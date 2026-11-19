import { ThemeProvider } from '@mui/material/styles'
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import theme from '../../theme/theme'
import PagosPanel from './PagosPanel'
import { listarPagos } from '../../api/client'

vi.mock('../../api/client', () => ({
  listarPagos: vi.fn(),
}))

const mockListar = vi.mocked(listarPagos)

function renderPanel() {
  return render(
    <ThemeProvider theme={theme}>
      <PagosPanel empresaId={1} />
    </ThemeProvider>,
  )
}

describe('PagosPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra el estado vacío cuando no hay pagos', async () => {
    mockListar.mockResolvedValue([])
    renderPanel()
    expect(await screen.findByText('Sin pagos')).toBeInTheDocument()
  })

  it('lista los pagos con plan, proveedor y estado', async () => {
    mockListar.mockResolvedValue([
      { id: 1, referencia: 'WOMPI-123', proveedor: 'WOMPI', planCodigo: 'PRO', monto: 99000, moneda: 'COP', estado: 'APROBADO', url: null },
    ])
    renderPanel()
    expect(await screen.findByText('Plan PRO')).toBeInTheDocument()
    expect(screen.getByText('Aprobado')).toBeInTheDocument()
    expect(screen.getByText('WOMPI · WOMPI-123')).toBeInTheDocument()
  })

  it('muestra un error cuando falla la carga', async () => {
    mockListar.mockRejectedValue(new Error('boom'))
    renderPanel()
    expect(await screen.findByText('No se pudo cargar el historial de pagos.')).toBeInTheDocument()
  })
})
