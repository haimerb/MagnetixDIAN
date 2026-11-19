import { expect, test } from '@playwright/test'

test.describe('Reintentos de transmisión', () => {
  test('el administrador accede al panel de envíos', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario').fill('admin')
    await page.getByLabel('Contraseña').fill('admin123')
    await page.getByRole('button', { name: 'Ingresar' }).click()

    await page.getByRole('link', { name: 'Administración' }).click()
    await page.getByRole('tab', { name: 'Reintentos' }).click()

    await expect(page.getByText('Reintentos de transmisión')).toBeVisible()
    await expect(page.getByLabel('Estado')).toBeVisible()
  })
})
