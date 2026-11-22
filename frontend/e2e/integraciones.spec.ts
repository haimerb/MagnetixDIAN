import { expect, test } from '@playwright/test'
import { loginAdmin } from './helpers'

const PROVEEDORES = [
  'SIIGO',
  'ALEGRA',
  'ODOO',
  'WORLD_OFFICE',
  'HELISA',
  'SHOPIFY',
  'WOOCOMMERCE',
  'SAP_BUSINESS_ONE',
  'DYNAMICS_365',
  'NETSUITE',
]

test.describe('Integraciones por empresa', () => {
  test('muestra los diez proveedores y el selector de métodos de pago con Bre-B', async ({ page }) => {
    await loginAdmin(page)
    await page.goto('/app/empresa')
    await page.getByRole('tab', { name: 'Integraciones' }).click()

    await expect(page.getByRole('heading', { name: 'Proveedores conectables' })).toBeVisible()
    await expect(page.getByText('Colombia · Vía importación').first()).toBeVisible()

    for (const proveedor of PROVEEDORES) {
      await expect(page.getByRole('heading', { name: proveedor, exact: true })).toBeVisible()
    }

    await expect(page.getByRole('button', { name: 'Bre-B QR' })).toBeVisible()
  })
})
