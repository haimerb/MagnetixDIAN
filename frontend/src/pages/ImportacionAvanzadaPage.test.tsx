import { ThemeProvider } from '@mui/material/styles'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { importarMultiformato } from '../api/client'
import theme from '../theme/theme'
import ImportacionAvanzadaPage from './ImportacionAvanzadaPage'

vi.mock('../features/auth/useEmpresaActiva', () => ({
  useEmpresaActiva: () => ({ empresaId: 1, cargando: false }),
}))

vi.mock('../api/client', () => ({
  importarMultiformato: vi.fn(),
}))

const mockImportar = vi.mocked(importarMultiformato)

const RESULTADO = {
  anioGravable: 2025,
  formatos: [
    {
      codigo: '1001',
      hoja: '1001',
      estado: 'ACTIVO',
      activo: true,
      filas: 227,
      encabezados: ['NIT', 'Valor'],
      datos: [{ NIT: '900123456', Valor: '1000' }],
      medioMagneticoId: 42,
    },
    {
      codigo: '1003',
      hoja: '1003',
      estado: 'PENDIENTE_XSD',
      activo: false,
      filas: 15,
      encabezados: ['NIT'],
      datos: [{ NIT: '900123456' }],
      medioMagneticoId: null,
    },
  ],
  hojasOmitidas: ['Resumen'],
  formatosPersistidos: 1,
}

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <ImportacionAvanzadaPage />
    </ThemeProvider>,
  )
}

describe('ImportacionAvanzadaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockImportar.mockResolvedValue(RESULTADO)
  })

  it('analiza el workbook y muestra el resumen por formato', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.upload(
      screen.getByLabelText('Archivo Excel multiformato'),
      new File(['xlsx'], 'exogena.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      }),
    )
    await user.click(screen.getByRole('button', { name: 'Analizar workbook' }))

    await waitFor(() =>
      expect(mockImportar).toHaveBeenCalledWith(1, expect.any(Number), expect.any(File)),
    )
    expect(await screen.findByText('Formatos detectados')).toBeInTheDocument()
    expect(screen.getAllByText('1001').length).toBeGreaterThan(0)
    expect(screen.getAllByText('1003').length).toBeGreaterThan(0)
    expect(screen.getByText('Pendiente de XSD')).toBeInTheDocument()
    expect(screen.getByText(/Hojas sin código reconocido: Resumen/)).toBeInTheDocument()
  })

  it('muestra un error cuando falla el análisis', async () => {
    mockImportar.mockRejectedValue(new Error('fallo'))
    const user = userEvent.setup()
    renderPage()

    await user.upload(
      screen.getByLabelText('Archivo Excel multiformato'),
      new File(['xlsx'], 'exogena.xlsx'),
    )
    await user.click(screen.getByRole('button', { name: 'Analizar workbook' }))

    expect(
      await screen.findByText('No se pudo analizar el workbook. Verifique que sea un Excel (.xlsx) válido.'),
    ).toBeInTheDocument()
  })
})
