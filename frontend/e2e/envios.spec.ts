import { expect, test } from '@playwright/test'
import { loginAdmin } from './helpers'

test.describe('Reintentos de transmisión', () => {
  test('el administrador accede al panel de envíos', async ({ page }) => {
    await loginAdmin(page)

    await page.getByRole('button', { name: 'Administración' }).click()
    await page.getByRole('tab', { name: 'Reintentos' }).click()

    await expect(page.getByText('Reintentos de transmisión')).toBeVisible()
    await expect(page.getByLabel('Estado')).toBeVisible()
  })
})
