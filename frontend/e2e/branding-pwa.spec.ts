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
  await page.route('**/api/empresas/1/branding', (route) => {
    if (route.request().method() === 'PUT') {
      const solicitud = route.request().postDataJSON() as Record<string, unknown>
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ ...BRANDING, ...solicitud }),
      })
    }
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(BRANDING) })
  })
}

test.describe('Identidad visual y PWA', () => {
  test.beforeEach(async ({ page }) => {
    await stubAutenticacion(page)
  })

  test('guarda la marca y refleja el manifiesto PWA', async ({ page }) => {
    await loginAdmin(page)
    await page.goto('/app/empresa')
    await page.getByRole('tab', { name: 'Identidad visual' }).click()

    await expect(page.getByRole('heading', { name: 'Identidad visual' })).toBeVisible()
    await page.getByLabel('Nombre visible').fill('Marca E2E')
    await page.getByRole('button', { name: 'Guardar' }).click()

    await expect(page.getByText('Marca actualizada.')).toBeVisible()
    await expect(page.getByText('Marca E2E').first()).toBeVisible()

    await expect(page.locator('link[rel="manifest"]')).toHaveAttribute('href', '/manifest.webmanifest')
    await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute('content', '#0E5C55')
  })
})
