import { expect, test, type Page } from '@playwright/test'
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

  test('lista asientos y muestra la conciliación', async ({ page }) => {
    let conciliado = false
    await page.route('**/api/empresas/1/contabilidad/asientos', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([ASIENTO]),
      }),
    )
    await page.route('**/api/empresas/1/contabilidad/conciliacion', (route) => {
      conciliado = true
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          coincidencias: 1,
          diferencias: 0,
          totalDocumentos: 1,
          documentosSinAsiento: 0,
        }),
      })
    })

    await loginAdmin(page)
    await page.goto('/app/contabilidad')

    await expect(page.getByRole('heading', { name: 'Asientos contables' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'AS-12' })).toBeVisible()
    await expect(page.getByRole('cell', { name: '1105' })).toBeVisible()

    await page.getByRole('button', { name: 'Conciliar' }).click()

    await expect(page.getByText('Resultado de la conciliación')).toBeVisible()
    await expect(page.getByText('Conciliación contable completada.')).toBeVisible()
    expect(conciliado).toBe(true)
  })
})
