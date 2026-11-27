import { ThemeProvider } from '@mui/material/styles'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  crearActivoErp,
  darBajaActivoErp,
  depreciarActivoErp,
  listarActivosErp,
  listarDepreciacionesErp,
} from '../../api/client'
import theme from '../../theme/theme'
import ActivosTab from './ActivosTab'

vi.mock('../../api/client', () => ({
  crearActivoErp: vi.fn(),
  darBajaActivoErp: vi.fn(),
  depreciarActivoErp: vi.fn(),
  listarActivosErp: vi.fn(),
  listarDepreciacionesErp: vi.fn(),
}))

vi.mock('../notificaciones/contexto', () => ({
  useNotificaciones: () => ({ notificar: vi.fn() }),
}))

const activo = {
  id: 5,
  empresaId: 1,
  codigo: 'ACT-0001',
  nombre: 'Computador oficina',
  categoria: 'Equipo de oficina',
  fechaAlta: '2026-03-01',
  valorAdquisicion: 1000000,
  valorResidual: 100000,
  vidaUtilMeses: 60,
  depreciacionAcumulada: 15000,
  valorLibros: 985000,
  estado: 'ACTIVO',
  fechaBaja: null,
  observaciones: null,
  createdAt: '2026-03-01T08:00:00.000Z',
  updatedAt: '2026-03-01T08:00:00.000Z',
}

const depreciacion = {
  id: 9,
  empresaId: 1,
  activoId: 5,
  codigoActivo: 'ACT-0001',
  periodo: '2026-03',
  fecha: '2026-03-31',
  valor: 15000,
  depreciacionAcumulada: 15000,
  valorLibros: 985000,
  estado: 'REGISTRADO',
  createdAt: '2026-03-31T08:00:00.000Z',
}

function renderTab() {
  return render(
    <ThemeProvider theme={theme}>
      <ActivosTab empresaId={1} cargandoEmpresa={false} />
    </ThemeProvider>,
  )
}

describe('ActivosTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(listarActivosErp).mockResolvedValue([activo])
    vi.mocked(listarDepreciacionesErp).mockResolvedValue([depreciacion])
  })

  it('lista activos y depreciaciones', async () => {
    renderTab()

    expect(await screen.findAllByText('ACT-0001')).toHaveLength(2)
    expect(screen.getAllByText('2026-03')).toHaveLength(1)
  })

  it('calcula la depreciación del activo seleccionado', async () => {
    vi.mocked(depreciarActivoErp).mockResolvedValue(depreciacion)
    renderTab()
    expect(await screen.findAllByText('ACT-0001')).toHaveLength(2)

    await userEvent.clear(screen.getByLabelText('Periodo'))
    await userEvent.type(screen.getByLabelText('Periodo'), '2026-03')
    await userEvent.click(screen.getByRole('button', { name: 'Calcular depreciación' }))

    await waitFor(() => {
      expect(crearActivoErp).not.toHaveBeenCalled()
      expect(darBajaActivoErp).not.toHaveBeenCalled()
      expect(depreciarActivoErp).toHaveBeenCalledWith(1, 5, {
        periodo: '2026-03',
        fecha: expect.any(String),
      })
    })
  })
})
