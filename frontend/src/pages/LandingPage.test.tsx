import { ThemeProvider } from '@mui/material/styles'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import theme from '../theme/theme'
import { listarPlanes, type Plan } from '../api/client'
import LandingPage from './LandingPage'

vi.mock('../api/client', () => ({
  listarPlanes: vi.fn(),
}))

const mockPlanes = vi.mocked(listarPlanes)

const PLAN_FREE: Plan = {
  id: 1,
  codigo: 'FREE',
  nombre: 'Gratis',
  descripcion: 'Hasta 50 documentos al mes y 1 usuario.',
  precioMensual: 0,
  moneda: 'COP',
  limiteFacturasMes: 50,
  limiteUsuarios: 1,
  orden: 1,
}

const PLAN_PRO: Plan = {
  id: 2,
  codigo: 'PRO',
  nombre: 'Profesional',
  descripcion: 'Hasta 1.000 documentos al mes y 5 usuarios.',
  precioMensual: 99000,
  moneda: 'COP',
  limiteFacturasMes: 1000,
  limiteUsuarios: 5,
  orden: 2,
}

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <LandingPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('LandingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockPlanes.mockResolvedValue([PLAN_FREE, PLAN_PRO])
  })

  it('muestra la propuesta de valor y el enlace de registro', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: 'De la venta a la factura, sin fricción' })).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Crear cuenta gratis' }).length).toBeGreaterThan(0)
    expect(await screen.findByText('Profesional')).toBeInTheDocument()
    expect(screen.getAllByText('Gratis').length).toBeGreaterThan(0)
  })

  it('muestra detalles de términos y privacidad', async () => {
    renderPage()

    await screen.findAllByText('Gratis')
    expect(screen.getByRole('link', { name: 'Términos' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Privacidad' })).toBeInTheDocument()
  })
})
