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
    await page.goto('/app/integraciones')

    await expect(page.getByRole('heading', { name: 'Integraciones', level: 1 })).toBeVisible()

    for (const proveedor of PROVEEDORES) {
      await expect(page.getByRole('heading', { name: proveedor, exact: true })).toBeVisible()
    }

    await expect(page.getByRole('button', { name: 'Bre-B QR' })).toBeVisible()
  })
})
