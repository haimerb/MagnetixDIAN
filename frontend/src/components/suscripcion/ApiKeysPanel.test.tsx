import { ThemeProvider } from '@mui/material/styles'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import theme from '../../theme/theme'
import ApiKeysPanel from './ApiKeysPanel'
import { crearApiKey, listarApiKeys, revocarApiKey } from '../../api/client'

vi.mock('../../api/client', () => ({
  listarApiKeys: vi.fn(),
  crearApiKey: vi.fn(),
  revocarApiKey: vi.fn(),
}))

const mockListar = vi.mocked(listarApiKeys)
const mockCrear = vi.mocked(crearApiKey)
const mockRevocar = vi.mocked(revocarApiKey)

function renderPanel() {
  return render(
    <ThemeProvider theme={theme}>
      <ApiKeysPanel empresaId={1} />
    </ThemeProvider>,
  )
}

describe('ApiKeysPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra el estado vacío cuando no hay claves', async () => {
    mockListar.mockResolvedValue([])
    renderPanel()
    expect(await screen.findByText('Sin claves de API')).toBeInTheDocument()
  })

  it('lista las claves con su prefijo y estado', async () => {
    mockListar.mockResolvedValue([
      { id: 5, nombre: 'produccion', prefijo: 'abcd1234', activo: true, ultimoUso: null, createdAt: '2026-01-01T00:00:00Z' },
      { id: 6, nombre: 'legacy', prefijo: 'deadbeef', activo: false, ultimoUso: null, createdAt: '2026-01-01T00:00:00Z' },
    ])
    renderPanel()
    expect(await screen.findByText('produccion')).toBeInTheDocument()
    expect(screen.getByText('Activa')).toBeInTheDocument()
    expect(screen.getByText('Revocada')).toBeInTheDocument()
    expect(screen.getByText(/mx_abcd1234/)).toBeInTheDocument()
  })

  it('crea una clave y muestra el secreto una sola vez', async () => {
    mockListar.mockResolvedValue([])
    mockCrear.mockResolvedValue({ id: 1, nombre: 'prod', prefijo: 'abcd1234', clave: 'mx_abcd1234secreto' })
    const user = userEvent.setup()
    renderPanel()

    await user.click(await screen.findByRole('button', { name: 'Crear clave' }))
    await user.type(screen.getByLabelText('Nombre'), 'prod')
    await user.click(screen.getByRole('button', { name: 'Crear' }))

    await waitFor(() => expect(mockCrear).toHaveBeenCalledWith(1, 'prod'))
    expect(await screen.findByText('mx_abcd1234secreto')).toBeInTheDocument()
  })

  it('revoca una clave activa', async () => {
    mockListar.mockResolvedValue([
      { id: 5, nombre: 'produccion', prefijo: 'abcd1234', activo: true, ultimoUso: null, createdAt: '2026-01-01T00:00:00Z' },
    ])
    mockRevocar.mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderPanel()

    await user.click(await screen.findByRole('button', { name: 'Revocar clave produccion' }))
    await waitFor(() => expect(mockRevocar).toHaveBeenCalledWith(1, 5))
  })
})
