import { type Page } from '@playwright/test'
import { expect, test } from './fixtures'
import { loginAdmin } from './helpers'

const TOKEN =
  'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiZW1wcmVzYUlkIjoxfQ==.firma'

const PERFIL = {
  id: 1,
  username: 'admin',
  nombre: 'Administrador',
  email: 'admin@example.com',
  roles: ['ROLE_ADMIN'],
  empresaId: 1,
  empresaNit: '900123456',
  empresaRazonSocial: 'MagnetixDIAN SAS',
}

const BRANDING = {
  empresaId: 1,
  publicSlug: 'magnetixdian',
  displayName: 'MagnetixDIAN',
  logoUrl: null,
  primaryColor: '#0E5C55',
  accentColor: '#D98A2B',
  fontFamily: 'Inter',
  invoiceFooter: null,
  whatsappNumber: null,
  whatsappTemplate: null,
  pwaThemeColor: '#0E5C55',
  pwaBackgroundColor: '#F1F5F4',
}

const ASIENTO = {
  id: 12,
  numero: 'AS-12',
  fecha: '2026-11-20',
  tipoEvento: 'CAUSACION_RADIAN',
  descripcion: 'Causación RADIAN',
  totalDebito: 119000,
  totalCredito: 119000,
  lineas: [
    { id: 1, codigoCuenta: '4105', nombreCuenta: 'Ventas', descripcion: 'Ingreso', debito: 0, credito: 100000 },
    { id: 2, codigoCuenta: '2408', nombreCuenta: 'IVA', descripcion: 'Impuesto', debito: 0, credito: 19000 },
    { id: 3, codigoCuenta: '1105', nombreCuenta: 'Clientes', descripcion: 'Cartera', debito: 119000, credito: 0 },
  ],
}

const CONCILIACION = {
  conciliacionId: 7,
  empresaId: 1,
  fechaDesde: null,
  fechaHasta: null,
  coincidencias: 1,
  diferencias: 0,
  totalDocumentos: 1,
  documentosSinAsiento: 0,
  valorDiferencia: 0,
  lineas: [
    {
      documentoId: 55,
      consecutivo: 'FACT-1',
      asientoId: 12,
      resultado: 'CONCILIADO',
      severidad: 'INFO',
      campo: 'valor',
      valorDocumento: 119000,
      valorAsiento: 119000,
      diferencia: 0,
      mensaje: 'Valores iguales',
    },
  ],
}

const FORMATO_1125 = {
  codigo: '1125',
  descripcion: 'Información de compras y ventas',
  estado: 'ACTIVO',
  activo: true,
  version: 'v13',
  xsd: '1125.xsd',
  validacion: 'xsd-local-2025',
  generadorXml: 'GeneradorXmlExogenaOficial',
  encabezados: ['tipoDocumento', 'numeroDocumento', 'valor'],
}

async function stubAutenticacion(page: Page) {
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        accessToken: TOKEN,
        refreshToken: 'refresh-e2e',
        username: 'admin',
        roles: ['ROLE_ADMIN'],
      }),
    }),
  )
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(PERFIL) }),
  )
  await page.route('**/api/empresas/1/branding', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(BRANDING) }),
  )
}

test.describe('Contabilidad', () => {
  test.beforeEach(async ({ page }) => {
    await stubAutenticacion(page)
  })

  test('el menú de Contabilidad lleva a Asentar facturación y desde allí a Asientos contables', async ({ page }) => {
    await page.route('**/api/empresas/1/radian/documentos*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
    )
    await page.route('**/api/empresas/1/contabilidad/asientos*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([ASIENTO]) }),
    )

    await loginAdmin(page)
    await page.getByRole('button', { name: 'Contabilidad' }).click()
    await expect(page).toHaveURL(/\/app\/contabilidad\/asentar-facturacion$/)
    await expect(page.getByRole('heading', { name: 'Asentar la facturación' })).toBeVisible()

    await page.getByRole('link', { name: 'Asientos contables' }).click()
    await expect(page).toHaveURL(/\/app\/contabilidad(?:\?.*)?$/)
    await expect(page.getByRole('heading', { name: 'Asientos contables' })).toBeVisible()
    await expect(page.getByRole('tab', { name: 'Exógena' })).toBeVisible()
  })

  test('lista asientos y muestra la conciliación detallada', async ({ page }) => {
    let conciliado = false
    await page.route('**/api/empresas/1/contabilidad/asientos', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([ASIENTO]),
      }),
    )
    await page.route('**/api/empresas/1/contabilidad/conciliacion*', (route) => {
      conciliado = true
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(CONCILIACION),
      })
    })

    await loginAdmin(page)
    await page.goto('/app/contabilidad')

    await expect(page.getByRole('heading', { name: 'Asientos contables' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'AS-12' })).toBeVisible()
    await expect(page.getByRole('cell', { name: '1105' })).toBeVisible()

    await page.getByRole('button', { name: 'Conciliar' }).first().click()

    await expect(page.getByRole('heading', { name: 'Conciliación detallada' })).toBeVisible()
    await expect(page.getByText('Resultado de la conciliación')).toBeVisible()
    await expect(page.getByRole('cell', { name: 'FACT-1' })).toBeVisible()
    await expect(page.getByText('Conciliación contable completada.')).toBeVisible()
    expect(conciliado).toBe(true)
  })

  test('muestra estados vacíos honestos en PUC y periodos', async ({ page }) => {
    await loginAdmin(page)
    await page.goto('/app/contabilidad?tab=puc')
    await expect(page.getByRole('heading', { name: 'Plan único de cuentas', exact: true, level: 1 })).toBeVisible()
    await expect(page.getByText('Catálogo sin consulta pública')).toBeVisible()

    await page.getByRole('tab', { name: 'Periodos' }).click()
    await expect(page.getByRole('heading', { name: 'Periodos contables', exact: true, level: 1 })).toBeVisible()
  })

  test('genera y valida XML exógeno del formato 1125', async ({ page }) => {
    let generado = false
    await page.route('**/api/empresas/1/exogena/formatos', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([FORMATO_1125]) }),
    )
    await page.route('**/api/empresas/1/exogena/formatos/1125/conceptos', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { formato: '1125', codigo: 'PT', descripcion: 'Pagos y transferencias', codCpt: 1 },
        ]),
      }),
    )
    await page.route('**/api/empresas/1/exogena/1125/generar', (route) => {
      generado = true
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          formato: '1125',
          version: 'v13',
          descripcion: 'Información de compras y ventas',
          registros: 1,
          xml: '<mas><Cab></Cab><item></item></mas>',
          validacion: 'xsd-local-2025',
          prevalidacionOficial: false,
        }),
      })
    })
    await page.route('**/api/empresas/1/exogena/1125/validar-xml', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          formato: '1125',
          valido: true,
          mensaje: 'XML válido contra XSD local.',
          prevalidacionOficial: false,
        }),
      }),
    )

    await loginAdmin(page)
    await page.goto('/app/contabilidad?tab=exogena')
    await expect(page.getByRole('heading', { name: 'Información exógena' })).toBeVisible()
    await expect(page.getByText('1125 · Información de compras y ventas')).toBeVisible()

    await page.getByLabel('tipoDocumento').fill('CC')
    await page.getByLabel('numeroDocumento').fill('900123456')
    await page.getByLabel('valor').fill('100000')
    await page.getByRole('button', { name: 'Generar XML' }).click()

    await expect(page.locator('pre')).toContainText('<mas>')
    expect(generado).toBe(true)

    await page.getByRole('button', { name: 'Validar XSD' }).click()
    await expect(page.getByText('XML válido contra XSD local.')).toBeVisible()
  })

  test('muestra tarifas y causaciones de impuestos', async ({ page }) => {
    await page.route('**/api/empresas/1/impuestos/tarifas*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 1,
            impuestoCodigo: 'IVA',
            nombre: 'Impuesto sobre las ventas',
            anio: new Date().getFullYear(),
            conceptoCodigo: 'TARIFA_GENERAL',
            porcentaje: 19,
            baseMinUvt: 0,
            baseMaxUvt: 0,
            activo: true,
          },
        ]),
      }),
    )
    await page.route('**/api/empresas/1/impuestos/causaciones*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
    )
    await page.route('**/api/empresas/1/impuestos/declaraciones*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
    )
    await page.route('**/api/empresas/1/impuestos/calendario*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
    )
    await page.route('**/api/empresas/1/impuestos/responsabilidades', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
    )

    await loginAdmin(page)
    await page.goto('/app/contabilidad?tab=impuestos')
    await expect(page.getByRole('heading', { name: 'Impuestos', exact: true })).toBeVisible()
    await expect(page.getByRole('table', { name: 'Tarifas de impuestos' })).toContainText('IVA')
  })
})
