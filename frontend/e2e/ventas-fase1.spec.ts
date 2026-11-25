import { type Page, type Route } from '@playwright/test'
import { expect, test } from './fixtures'

type PedidoMock = {
  id: number
  empresaId: number
  ordenVentaId: number
  consecutivo: string
  clienteId: string | null
  cliente: string
  estado: string
  subtotal: number
  impuestos: number
  total: number
  fecha: string
  facturaDianId: string | null
  observaciones: string | null
}

function token(): string {
  const cabecera = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64')
  const carga = Buffer.from(JSON.stringify({
    sub: 'admin',
    roles: ['ROLE_ADMIN'],
    empresaId: 1,
  })).toString('base64')
  return `${cabecera}.${carga}.firma-e2e`
}

function responder(route: Route, cuerpo: unknown, estado = 200) {
  return route.fulfill({
    status: estado,
    contentType: 'application/json',
    body: JSON.stringify(cuerpo),
  })
}

async function preparar(page: Page) {
  const pedidos: PedidoMock[] = []

  await page.addInitScript(({ valor }) => {
    localStorage.setItem('magnetixdian_token', valor)
    localStorage.setItem('magnetixdian_refresh', 'refresh-e2e')
  }, { valor: token() })

  await page.route('**/api/**', async (route) => {
    const ruta = new URL(route.request().url()).pathname.replace(/^\/api/, '')
    const metodo = route.request().method()

    if (ruta === '/auth/me' && metodo === 'GET') {
      return responder(route, {
        id: 1,
        username: 'admin',
        nombre: 'Admin',
        email: 'admin@ejemplo.com',
        roles: ['ROLE_ADMIN'],
        empresaId: 1,
        empresaNit: '900123456',
        empresaRazonSocial: 'MagnetixDIAN SAS',
      })
    }
    if (ruta === '/empresas/1/erp/ventas/ordenes' && metodo === 'POST') {
      const cuerpo = route.request().postDataJSON() as Record<string, unknown>
      return responder(route, {
        id: 7,
        empresaId: 1,
        numero: cuerpo.numero,
        clienteId: cuerpo.clienteId,
        clienteNombre: cuerpo.clienteNombre,
        fecha: cuerpo.fecha,
        estado: 'BORRADOR',
        subtotal: cuerpo.subtotal,
        impuestos: cuerpo.impuestos,
        total: cuerpo.total,
        moneda: cuerpo.moneda,
        observaciones: cuerpo.observaciones,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      })
    }
    if (ruta === '/empresas/1/erp/ventas/pedidos' && metodo === 'GET') {
      return responder(route, pedidos)
    }
    if (ruta === '/empresas/1/erp/ventas/pedidos' && metodo === 'POST') {
      const cuerpo = route.request().postDataJSON() as Record<string, unknown>
      pedidos.push({
        id: 12,
        empresaId: 1,
        ordenVentaId: Number(cuerpo.ordenVentaId),
        consecutivo: String(cuerpo.numero),
        clienteId: cuerpo.clienteId as string | null,
        cliente: String(cuerpo.clienteNombre),
        estado: 'BORRADOR',
        subtotal: Number(cuerpo.subtotal),
        impuestos: Number(cuerpo.impuestos),
        total: Number(cuerpo.total),
        fecha: String(cuerpo.fecha),
        facturaDianId: null,
        observaciones: null,
      })
      return responder(route, pedidos[0], 201)
    }
    const aprobar = ruta.match(/^\/empresas\/1\/erp\/ventas\/pedidos\/(\d+)\/aprobar$/)
    if (aprobar && metodo === 'POST') {
      const pedido = pedidos.find((p) => p.id === Number(aprobar[1]))
      if (!pedido) return responder(route, { message: 'Pedido no encontrado' }, 404)
      pedido.estado = 'APROBADO'
      return responder(route, pedido)
    }
    if (ruta === '/facturacion/1/emitir' && metodo === 'POST') {
      const cuerpo = route.request().postDataJSON() as { pedidoId?: number }
      expect(cuerpo.pedidoId).toBe(12)
      const pedido = pedidos.find((p) => p.id === cuerpo.pedidoId)
      if (pedido) pedido.facturaDianId = 'FV 0002'
      return responder(route, {
        exitoso: true,
        id: 88,
        numeroCompleto: 'FV 0002',
        cufe: 'cufe-e2e-fase1',
        estado: 'EMITIDO',
        qrUrl: null,
        mensaje: 'Aceptado',
        errores: [],
      })
    }

    return responder(route, { mensaje: 'Ruta no implementada en el mock E2E' }, 404)
  })
}

test('venta ERP genera pedido, factura DIAN y deja trazabilidad', async ({ page }) => {
  await preparar(page)
  await page.goto('/app/erp/ventas')

  await expect(page.getByRole('heading', { name: 'Ventas' })).toBeVisible()
  await page.getByLabel('Número').fill('PED-2026-001')
  await page.getByLabel('Documento cliente').fill('800123456')
  await page.getByLabel('Cliente', { exact: true }).fill('Cliente ERP')
  await page.getByRole('button', { name: 'Crear pedido' }).click()

  await expect(page.getByText('PED-2026-001', { exact: true })).toBeVisible()
  await expect(page.getByText('Borrador', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Aprobar pedido' }).click()
  await expect(page.getByText('Aprobado', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Emitir factura' }).click()
  await expect(page.getByText('FV 0002', { exact: true })).toBeVisible()
  await expect(page.getByText('Factura FV 0002 vinculada al pedido PED-2026-001.')).toBeVisible()
})
