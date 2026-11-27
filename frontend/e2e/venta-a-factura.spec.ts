import { type Page, type Route } from '@playwright/test'
import { expect, test } from './fixtures'

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
  await page.addInitScript(({ valor }) => {
    localStorage.setItem('magnetixdian_token', valor)
    localStorage.setItem('magnetixdian_refresh', 'refresh-e2e')
  }, { valor: token() })

  await page.route('**/api/**', async (route) => {
    const url = new URL(route.request().url())
    const ruta = url.pathname.replace(/^\/api/, '')
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
    if (ruta === '/empresas/1/dian/habilitacion' && metodo === 'GET') {
      return responder(route, {
        empresaId: 1,
        modo: 'ADQUIRIDO_CLIENTE',
        estado: 'PRODUCCION',
        resolucionNumero: 'RES-2026-001',
        resolucionFecha: '2026-01-15',
        prefijo: 'FV',
        rangoDesde: 1,
        rangoHasta: 1000,
        testSetId: 'TS-E2E-001',
        softwareId: 'SW-E2E',
        notificarAdquirente: true,
        certificadoNombre: 'certificado.p12',
        switchoverEn: new Date().toISOString(),
        aprobadoEn: new Date().toISOString(),
        auditoria: [],
      })
    }
    if (ruta === '/empresas/1/integraciones' && metodo === 'GET') {
      return responder(route, [])
    }
    if (ruta === '/empresas/1/erp/ventas/pedidos' && metodo === 'GET') {
      return responder(route, [{
        id: 12,
        consecutivo: 'PED-2026-001',
        cliente: 'Cliente ERP',
        estado: 'CONFIRMADO',
        total: 1250000,
        fecha: '2026-11-20',
      }])
    }
    if (ruta === '/facturacion/1/validar' && metodo === 'POST') {
      return responder(route, [])
    }
    if (ruta === '/facturacion/1/emitir' && metodo === 'POST') {
      return responder(route, {
        exitoso: true,
        id: 88,
        numeroCompleto: 'FV 0002',
        cufe: 'cufe-e2e-0002',
        estado: 'EMITIDO',
        qrUrl: null,
        mensaje: 'Aceptado',
        errores: [],
      })
    }
    if (ruta === '/empresas/1/contabilidad/asientos/origen' && metodo === 'GET') {
      if (url.searchParams.get('origen') === 'FACTURA' && url.searchParams.get('origenId') === '88') {
        return responder(route, {
          id: 66,
          numero: 'AS-88',
          fecha: new Date().toISOString().slice(0, 10),
          tipoEvento: 'VENTA',
          origen: 'FACTURA',
          origenId: 88,
          descripcion: 'Asiento de la factura FV 0002',
          totalDebito: 1250000,
          totalCredito: 1250000,
          lineas: [],
        })
      }
      return responder(route, null, 404)
    }

    return responder(route, { mensaje: 'Ruta no implementada en el mock E2E' }, 404)
  })
}

test('venta del ERP se convierte en factura y asiento', async ({ page }) => {
  await preparar(page)
  await page.goto('/app/erp/integraciones')

  await expect(page.getByRole('heading', { name: 'Integraciones' })).toBeVisible()
  await expect(page.getByText('Pedidos ERP')).toBeVisible()
  await expect(page.getByText('PED-2026-001')).toBeVisible()

  await page.getByRole('link', { name: 'Nueva factura desde pedido' }).click()
  await expect(page.getByRole('heading', { name: 'Nueva factura electrónica' })).toBeVisible()
  await expect(page.getByText(/Habilitación en producción activa/)).toBeVisible()

  await page.getByLabel('Número de documento').fill('800123456')
  await page.getByLabel('Nombre / razón social').fill('Cliente ERP')
  await page.getByLabel('Descripción').fill('Venta desde pedido')
  await page.getByLabel('Valor unitario').fill('1250000')

  await page.getByRole('button', { name: 'Emitir factura' }).click()
  await expect(page.getByText('Estado: EMITIDO')).toBeVisible()
  await expect(page.getByText('FV 0002')).toBeVisible()

  await expect(page.getByText(/Asiento AS-88 generado automáticamente/)).toBeVisible()
})
