import { describe, expect, it, vi } from 'vitest'
import { api } from './client'
import { listarComprasErp, listarInventarioErp, listarTesoreriaErp } from './erp/erp'

describe('rutas de lectura del ERP', () => {
  it('usa los endpoints publicados por el backend', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: [] } as never)

    await listarComprasErp(1)
    await listarInventarioErp(1)
    await listarTesoreriaErp(1)

    expect(get.mock.calls.map(([url]) => url)).toEqual([
      '/empresas/1/erp/compras/ordenes',
      '/empresas/1/erp/inventario/movimientos',
      '/empresas/1/erp/tesoreria/movimientos',
    ])
  })
})
