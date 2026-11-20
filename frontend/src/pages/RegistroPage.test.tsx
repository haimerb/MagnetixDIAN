import { ThemeProvider } from '@mui/material/styles'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import theme from '../theme/theme'
import { registrarEmpresa } from '../api/client'
import RegistroPage from './RegistroPage'

vi.mock('../api/client', () => ({
  registrarEmpresa: vi.fn(),
}))

const mockRegistrar = vi.mocked(registrarEmpresa)

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter>
        <RegistroPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

async function llenarFormulario(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/NIT/), '900123456')
  await user.type(screen.getByLabelText(/Razón social/), 'Empresa Prueba SAS')
  await user.type(screen.getByLabelText(/Nombre completo/), 'Cliente Uno')
  await user.type(screen.getByLabelText(/Correo electrónico/), 'cliente1@ejemplo.com')
  await user.type(screen.getByLabelText(/Usuario/), 'cliente1')
  await user.type(screen.getByLabelText(/Contraseña/), 'clave1234')
}

describe('RegistroPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('registra la empresa con aceptación de términos', async () => {
    mockRegistrar.mockResolvedValue({
      empresaId: 1,
      usuarioId: 2,
      username: 'cliente1',
      mensaje: 'Cuenta creada.',
    })
    const user = userEvent.setup()
    renderPage()

    await llenarFormulario(user)
    await user.click(screen.getByRole('checkbox', { name: 'Acepto los términos y condiciones' }))
    await user.click(screen.getByRole('button', { name: 'Crear cuenta gratis' }))

    await waitFor(() =>
      expect(mockRegistrar).toHaveBeenCalledWith({
        nit: '900123456',
        razonSocial: 'Empresa Prueba SAS',
        nombre: 'Cliente Uno',
        email: 'cliente1@ejemplo.com',
        username: 'cliente1',
        password: 'clave1234',
        aceptaTerminos: true,
      }),
    )
  })

  it('muestra el error devuelto por el servidor', async () => {
    mockRegistrar.mockRejectedValue({ response: { data: { message: 'El NIT ya está registrado.' } } })
    const user = userEvent.setup()
    renderPage()

    await llenarFormulario(user)
    await user.click(screen.getByRole('checkbox', { name: 'Acepto los términos y condiciones' }))
    await user.click(screen.getByRole('button', { name: 'Crear cuenta gratis' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El NIT ya está registrado.')
  })
})
