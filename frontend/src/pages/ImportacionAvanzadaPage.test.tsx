import { ThemeProvider } from '@mui/material/styles'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  confirmarImportacion,
  importarMultiformato,
  validarDatosJson,
  vistaPreviaArchivo,
} from '../api/client'
import theme from '../theme/theme'
import ImportacionAvanzadaPage from './ImportacionAvanzadaPage'
import ImportacionUniversalPage from './ImportacionUniversalPage'

vi.mock('../features/auth/useEmpresaActiva', () => ({
  useEmpresaActiva: () => ({ empresaId: 1, cargando: false }),
}))

vi.mock('../api/client', () => ({
  importarMultiformato: vi.fn(),
  validarDatosJson: vi.fn(),
  vistaPreviaArchivo: vi.fn(),
  confirmarImportacion: vi.fn(),
  listarMapeos: vi.fn(),
}))

vi.mock('../features/notificaciones/contexto', () => ({
  useNotificaciones: () => ({ notificar: vi.fn() }),
}))

const mockImportar = vi.mocked(importarMultiformato)
const mockValidar = vi.mocked(validarDatosJson)
const mockVistaPrevia = vi.mocked(vistaPreviaArchivo)
const mockConfirmar = vi.mocked(confirmarImportacion)

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
      estado: 'PENDIENTE_GENERADOR',
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

const VALIDACION = {
  id: null,
  medioMagneticoId: 42,
  estado: 'COMPLETADA',
  erroresTotal: 1,
  advertenciasTotal: 0,
  registrosValidados: 227,
  inicio: '2026-11-21T12:00:00Z',
  fin: '2026-11-21T12:00:01Z',
  errores: [
    {
      linea: 5,
      campo: 'NIT',
      valorEsperado: ' LONGITUD_9',
      valorEncontrado: '900123',
      mensaje: 'NIT con longitud inválida.',
      severidad: 'ERROR' as const,
    },
  ],
}

const VISTA_PREVIA = {
  archivoId: 'archivo-1',
          nombreArchivo: 'exogena.xlsx',
  formatoDetectado: 'CSV',
  cabeceras: ['NIT', 'Valor'],
  filasEjemplo: [['900123456', '1000']],
  sugerencias: [
    { campo: 'nit', cabeceraOrigen: 'NIT', indice: 0, confianza: 0.95, formatoDetectado: 'CSV' },
    { campo: 'total', cabeceraOrigen: 'Valor', indice: 1, confianza: 0.8, formatoDetectado: 'CSV' },
  ],
  confianza: 0.875,
  totalFilas: 120,
  contenidoBase64: 'YQ==',
}

const RESULTADO_IMPORTACION = {
  archivoId: 'archivo-1',
  estado: 'COMPLETADA',
  filasLeidas: 120,
  filasImportadas: 118,
  filasOmitidas: 2,
  errores: 0,
  mensajes: [],
}

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <ImportacionAvanzadaPage />
    </ThemeProvider>,
  )
}

function renderUniversalPage() {
  return render(
    <ThemeProvider theme={theme}>
      <ImportacionUniversalPage />
    </ThemeProvider>,
  )
}

describe('ImportacionAvanzadaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockImportar.mockResolvedValue(RESULTADO)
    mockValidar.mockResolvedValue(VALIDACION)
    mockVistaPrevia.mockResolvedValue(VISTA_PREVIA)
    mockConfirmar.mockResolvedValue(RESULTADO_IMPORTACION)
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
    expect(screen.getByText('Pendiente de generador')).toBeInTheDocument()
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

  it('valida la estructura del registro persistido', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.upload(
      screen.getByLabelText('Archivo Excel multiformato'),
      new File(['xlsx'], 'exogena.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      }),
    )
    await user.click(screen.getByRole('button', { name: 'Analizar workbook' }))
    await user.click(
      await screen.findByRole('button', { name: 'Validar estructura del registro 42' }),
    )

    await waitFor(() => expect(mockValidar).toHaveBeenCalledWith(42))
    expect(await screen.findByText(/Validación estructural · registro #42/)).toBeInTheDocument()
    expect(screen.getByText('NIT con longitud inválida.')).toBeInTheDocument()
  })

  it('usa el contrato canónico de vista previa y confirmación universal', async () => {
    const user = userEvent.setup()
    renderUniversalPage()

    await user.upload(
      screen.getByLabelText('Archivo de importación universal'),
      new File(['xlsx'], 'exogena.xlsx', {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      }),
    )
    await user.click(screen.getByRole('button', { name: 'Analizar archivo' }))

    await waitFor(() => expect(mockVistaPrevia).toHaveBeenCalledWith(1, expect.any(File)))
    expect(await screen.findByText('Mapeo de columnas')).toBeInTheDocument()
    expect(screen.getByText('Columnas')).toBeInTheDocument()
    expect(screen.getByText('95%')).toBeInTheDocument()
    expect(screen.getByText('80%')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Confirmar importación' }))

    await waitFor(() =>
      expect(mockConfirmar).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
  archivoId: 'archivo-1',
  nombreArchivo: 'exogena.xlsx',
          formatoDetectado: 'CSV',
          contenidoBase64: 'YQ==',
          columnas: VISTA_PREVIA.sugerencias,
        }),
      ),
    )
  })
})
