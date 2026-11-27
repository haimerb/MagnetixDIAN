import { describe, expect, it, vi } from 'vitest'
import { api } from './client'
import {
  crearCompraErp,
  crearCuentaBancariaErp,
  listarCobrosClienteErp,
  listarComprasErp,
  listarCuentasBancariasErp,
  listarExistenciasErp,
  listarInventarioErp,
  listarPagosProveedorErp,
  listarRecepcionesErp,
  listarTesoreriaErp,
  recibirCompraErp,
  registrarCobroClienteErp,
  registrarMovimientoInventarioErp,
  registrarPagoProveedorErp,
} from './erp/erp'

describe('rutas del ERP', () => {
  it('usa los endpoints publicados por el backend para lectura', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: [] } as never)

    await listarComprasErp(1)
    await listarRecepcionesErp(1)
    await listarInventarioErp(1)
    await listarExistenciasErp(1)
    await listarTesoreriaErp(1)
    await listarCuentasBancariasErp(1)
    await listarPagosProveedorErp(1)
    await listarCobrosClienteErp(1)

    expect(get.mock.calls.map(([url]) => url)).toEqual([
      '/empresas/1/erp/compras/ordenes',
      '/empresas/1/erp/compras/recepciones',
      '/empresas/1/erp/inventario/movimientos',
      '/empresas/1/erp/inventario/existencias',
      '/empresas/1/erp/tesoreria/movimientos',
      '/empresas/1/erp/tesoreria/cuentas-bancarias',
      '/empresas/1/erp/tesoreria/pagos-proveedor',
      '/empresas/1/erp/tesoreria/cobros-cliente',
    ])
  })

  it('usa los endpoints transaccionales de Fase 2', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: {} } as never)

    await crearCompraErp(1, {} as never)
    await recibirCompraErp(1, 7, {} as never)
    await registrarMovimientoInventarioErp(1, {} as never)
    await crearCuentaBancariaErp(1, {} as never)
    await registrarPagoProveedorErp(1, {} as never)
    await registrarCobroClienteErp(1, {} as never)

    expect(post.mock.calls.map(([url]) => url)).toEqual([
      '/empresas/1/erp/compras/ordenes',
      '/empresas/1/erp/compras/ordenes/7/recepciones',
      '/empresas/1/erp/inventario/movimientos',
      '/empresas/1/erp/tesoreria/cuentas-bancarias',
      '/empresas/1/erp/tesoreria/pagos-proveedor',
      '/empresas/1/erp/tesoreria/cobros-cliente',
    ])
  })
})
