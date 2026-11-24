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

const DOCUMENTO = {
  id: 77,
  empresaId: 1,
  direccion: 'RECIBIDO',
  tipoDocumento: '01',
  cufeCude: 'CUFE-E2E-77',
  consecutivo: 'RC 1001',
  nitContraparte: '900123456',
  razonSocialContraparte: 'Proveedor E2E',
  fecha: '2026-11-20',
  subtotal: 100000,
  impuestos: 19000,
  retenciones: 0,
  total: 119000,
  estadoRadian: 'RECIBIDO',
  estadoCausacion: 'PENDIENTE',
  estadoContabilizacion: 'PENDIENTE',
  asientoContableId: null,
  createdAt: '2026-11-20T10:00:00Z',
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

async function stubListado(page: Page) {
  await page.route('**/api/empresas/1/radian/documentos', (route) => {
    if (route.request().method() !== 'GET') return route.continue()
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([DOCUMENTO]),
    })
  })
}

test.describe('Asentar facturación', () => {
  test.beforeEach(async ({ page }) => {
    await stubAutenticacion(page)
    await stubListado(page)
  })

  test('trae documentos de la DIAN con token', async ({ page }) => {
    await page.route('**/api/empresas/1/dian/documentos/token', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          origen: 'TOKEN',
          archivo: '',
          documentos: 1,
          mensaje: '1 documento traído de la DIAN.',
          registros: [DOCUMENTO],
        }),
      }),
    )

    await loginAdmin(page)
    await page.goto('/app/contabilidad/asentar-facturacion')

    await expect(page.getByRole('heading', { name: 'Asentar la facturación' })).toBeVisible()
    await page.getByRole('button', { name: 'Traer de la DIAN' }).click()
    await page.getByLabel('Token o enlace de la DIAN').fill('token-e2e')
    await page.getByRole('button', { name: 'Traer documentos' }).click()

    await expect(page.getByText('1 documento(s) traídos de la DIAN.')).toBeVisible()
    await expect(page.getByRole('cell', { name: 'RC 1001' })).toBeVisible()
  })

  test('causa un documento desde la bandeja', async ({ page }) => {
    let causado = false
    await page.route('**/api/empresas/1/radian/documentos/77/causar', (route) => {
      causado = true
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          documentoId: 77,
          causacionId: 88,
          estado: 'CAUSADA',
          fechaCausacion: '2026-11-21',
          total: 119000,
          mensaje: 'Documento RC 1001 causado.',
        }),
      })
    })

    await loginAdmin(page)
    await page.goto('/app/contabilidad/asentar-facturacion')

    await expect(page.getByRole('heading', { name: 'Asentar la facturación' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'RC 1001' })).toBeVisible()
    await page.getByRole('button', { name: 'Causar' }).click()
    await page.getByRole('button', { name: 'Confirmar' }).click()

    await expect(page.getByText('Documento RC 1001 causado.')).toBeVisible()
    expect(causado).toBe(true)
  })
})
