import { type Page, type Route } from '@playwright/test'
import { expect, test } from './fixtures'

type Opciones = {
  empresaId?: number
  firmado?: boolean
  duplicado?: boolean
  estadoHabilitacion?: string
  fallarHabilitacion?: boolean
}

function token(empresaId: number): string {
  const cabecera = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64')
  const carga = Buffer.from(JSON.stringify({
    sub: 'admin',
    roles: ['ROLE_ADMIN'],
    empresaId,
  })).toString('base64')
  return `${cabecera}.${carga}.firma-e2e`
}

async function entrar(page: Page, empresaId = 1) {
  await page.addInitScript(({ valor }) => {
    localStorage.setItem('magnetixdian_token', valor)
    localStorage.setItem('magnetixdian_refresh', 'refresh-e2e')
  }, { valor: token(empresaId) })
}

function responder(route: Route, cuerpo: unknown, estado = 200) {
  return route.fulfill({
    status: estado,
    contentType: 'application/json',
    body: JSON.stringify(cuerpo),
  })
}

async function mockBase(page: Page, opciones: Opciones = {}) {
  const empresaId = opciones.empresaId ?? 1
  const peticiones: string[] = []

  await page.route('**/api/**', async (route) => {
    const ruta = new URL(route.request().url()).pathname.replace(/^\/api/, '')
    const metodo = route.request().method()
    peticiones.push(`${metodo} ${ruta}`)

    if (ruta === '/auth/me' && metodo === 'GET') {
      return responder(route, {
        id: 1,
        username: 'admin',
        nombre: 'Admin',
        email: 'admin@ejemplo.com',
        roles: ['ROLE_ADMIN'],
        empresaId,
        empresaNit: '900123456',
        empresaRazonSocial: 'MagnetixDIAN SAS',
      })
    }
    if (ruta === '/planes' && metodo === 'GET') {
      return responder(route, [{
        id: 1,
        codigo: 'BASICO',
        nombre: 'Básico',
        descripcion: 'Plan de prueba',
        precioMensual: 99000,
        moneda: 'COP',
        limiteFacturasMes: 100,
        limiteUsuarios: 2,
        orden: 1,
      }])
    }
    if (ruta === `/empresas/${empresaId}/pagos/checkout` && metodo === 'POST') {
      return responder(route, {
        id: 1,
        referencia: 'PAY-E2E-001',
        proveedor: 'SIMULADO',
        planCodigo: 'BASICO',
        monto: 99000,
        moneda: 'COP',
        estado: 'APROBADO',
        url: null,
      })
    }
    if (/^\/empresas\/\d+\/pagos\/[^/]+\/webhook-estado$/.test(ruta) && metodo === 'GET') {
      return responder(route, {
        referencia: 'PAY-E2E-001',
        proveedor: 'SIMULADO',
        estado: opciones.duplicado ? 'RECHAZADO' : 'APROBADO',
        firmado: opciones.firmado ?? true,
        eventId: opciones.duplicado ? 'evt-duplicado' : 'evt-unico',
        procesadoEn: opciones.duplicado ? null : new Date().toISOString(),
        mensaje: opciones.duplicado ? 'Evento duplicado: no procesado' : 'Webhook verificado',
      })
    }
    if (ruta === `/empresas/${empresaId}/dian/habilitacion` && metodo === 'GET') {
      if (opciones.fallarHabilitacion) {
        return responder(route, { mensaje: 'Acceso denegado' }, 403)
      }
      return responder(route, {
        empresaId,
        modo: 'ADQUIRIDO_CLIENTE',
        estado: opciones.estadoHabilitacion ?? 'BORRADOR',
        resolucionNumero: null,
        resolucionFecha: null,
        prefijo: null,
        rangoDesde: null,
        rangoHasta: null,
        testSetId: null,
        softwareId: null,
        notificarAdquirente: false,
        certificadoNombre: null,
        switchoverEn: null,
        aprobadoEn: null,
        auditoria: [],
      })
    }

    return responder(route, { mensaje: 'Ruta no implementada en el mock E2E' }, 404)
  })

  return { peticiones }
}

async function pagar(page: Page) {
  await page.goto('/checkout?empresa=1&plan=BASICO')
  await page.getByRole('button', { name: 'Pagar con pasarela de prueba' }).click()
  await expect(page.getByText('Referencia: PAY-E2E-001')).toBeVisible()
  await page.getByRole('button', { name: 'Consultar estado del webhook' }).click()
}

test.describe('Negativos ERP', () => {
  test('webhook sin firma se marca como inválido', async ({ page }) => {
    await entrar(page)
    await mockBase(page, { firmado: false })
    await pagar(page)
    await expect(page.getByText(/firma ausente o inválida/)).toBeVisible()
  })

  test('replay del evento de pago se rechaza', async ({ page }) => {
    await entrar(page)
    await mockBase(page, { duplicado: true })
    await pagar(page)
    await expect(page.getByText('Evento duplicado: no procesado')).toBeVisible()
    await expect(page.getByText(/firma válida/)).toBeVisible()
  })

  test('sin TEST_SET_APROBADO la emisión está bloqueada', async ({ page }) => {
    await entrar(page)
    await mockBase(page, { estadoHabilitacion: 'BORRADOR' })
    await page.goto('/app/facturacion/nueva')
    await expect(page.getByText(/Emisión bloqueada/)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Emitir factura' })).toBeDisabled()
  })

  test('la empresa activa nunca consulta datos de otra empresa', async ({ page }) => {
    await entrar(page, 2)
    const { peticiones } = await mockBase(page, { empresaId: 2, fallarHabilitacion: true })
    await page.goto('/app/habilitacion-dian')
    await expect(page.getByText('No se pudo consultar la habilitación DIAN de la empresa.')).toBeVisible()
    expect(peticiones.some((peticion) => peticion.includes('/empresas/1/'))).toBe(false)
    expect(peticiones.some((peticion) => peticion.includes('/empresas/2/dian/habilitacion'))).toBe(true)
  })
})
