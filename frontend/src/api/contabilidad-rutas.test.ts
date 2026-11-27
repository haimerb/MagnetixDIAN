import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  api,
  buscarAsientoPorOrigen,
  cerrarPeriodoContable,
  crearPeriodoContable,
} from './client'

describe('rutas de contabilidad', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('usa los endpoints reales de apertura y cierre de periodos', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: {} } as never)

    await crearPeriodoContable(1, {
      anio: 2026,
      mes: 3,
      fechaDesde: '2026-03-01',
      fechaHasta: '2026-03-31',
    })
    await cerrarPeriodoContable(1, 7)

    expect(post.mock.calls.map(([url]) => url)).toEqual([
      '/empresas/1/contabilidad/periodos',
      '/empresas/1/contabilidad/periodos/7/cerrar',
    ])
  })

  it('busca el asiento por origen sin mostrar error global', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: { id: 12 } } as never)

    const resultado = await buscarAsientoPorOrigen(1, 'FACTURA', 44, 'VENTA')

    expect(resultado).toEqual({ id: 12 })
    expect(get).toHaveBeenCalledWith(
      '/empresas/1/contabilidad/asientos/origen',
      {
        params: { origen: 'FACTURA', origenId: 44, tipoEvento: 'VENTA' },
        skipGlobalError: true,
      },
    )
  })

  it('devuelve null cuando el asiento por origen no existe todavía', async () => {
    const error404 = Object.assign(new Error('No encontrado'), {
      isAxiosError: true,
      response: { status: 404 },
      config: {},
    })
    vi.spyOn(api, 'get').mockRejectedValueOnce(error404 as never)

    const resultado = await buscarAsientoPorOrigen(1, 'FACTURA', 44, 'VENTA')

    expect(resultado).toBeNull()
  })
})
