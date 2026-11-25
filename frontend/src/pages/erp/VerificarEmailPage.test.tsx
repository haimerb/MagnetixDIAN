import { ThemeProvider } from '@mui/material/styles'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import theme from '../../theme/theme'
import { reenviarVerificacionEmail, verificarEmail } from '../../api/erp/cuenta'
import VerificarEmailPage from './VerificarEmailPage'

vi.mock('../../api/erp/cuenta', () => ({
  verificarEmail: vi.fn(),
  reenviarVerificacionEmail: vi.fn(),
  tokenDeVerificacion: (valor: string | null) => valor?.trim() || null,
}))

const mockVerificar = vi.mocked(verificarEmail)
const mockReenviar = vi.mocked(reenviarVerificacionEmail)

function renderPage(ruta: string) {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[ruta]}>
        <VerificarEmailPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('VerificarEmailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('usa el endpoint de registro y muestra la respuesta del backend', async () => {
    mockVerificar.mockResolvedValue({
      empresaId: 1,
      estadoTenant: 'ACTIVO',
      emailVerificado: true,
      mensaje: 'Correo verificado.',
    })
    renderPage('/verificar-email?token=abc')

    await waitFor(() => expect(mockVerificar).toHaveBeenCalledWith('abc'))
    expect(await screen.findByText(/Correo verificado\. · Estado del tenant: ACTIVO/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Continuar a habilitación DIAN' })).toBeInTheDocument()
  })

  it('muestra el mensaje del servidor cuando el enlace no es válido', async () => {
    mockVerificar.mockRejectedValue({
      response: { data: { message: 'Token de verificación expirado.' } },
    })
    renderPage('/verificar-email?token=abc')

    expect(await screen.findByRole('alert')).toHaveTextContent('Token de verificación expirado.')
  })

  it('muestra el mensaje devuelto al reenviar la verificación', async () => {
    mockReenviar.mockResolvedValue({
      enviado: true,
      mensaje: 'Revisa tu bandeja de entrada.',
      token: null,
    })
    renderPage('/verificar-email')

    fireEvent.change(screen.getByLabelText(/Correo electrónico/), {
      target: { value: 'cliente@ejemplo.com' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Reenviar verificación' }))

    await waitFor(() =>
      expect(mockReenviar).toHaveBeenCalledWith('cliente@ejemplo.com'))
    expect(await screen.findByText('Revisa tu bandeja de entrada.')).toBeInTheDocument()
  })
})
