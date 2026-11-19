import { expect, test } from '@playwright/test'

test.describe('Autenticación', () => {
  test('muestra error con credenciales inválidas', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario').fill('admin')
    await page.getByLabel('Contraseña').fill('clave-incorrecta')
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await expect(page.getByRole('alert')).toContainText('Credenciales inválidas')
  })

  test('inicia sesión y llega al panel', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario').fill('admin')
    await page.getByLabel('Contraseña').fill('admin123')
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await expect(page.getByRole('heading', { name: 'Panel de reportes' })).toBeVisible()
  })

  test('permite solicitar recuperación de contraseña', async ({ page }) => {
    await page.goto('/recuperar')
    await page.getByLabel('Usuario o correo').fill('admin')
    await page.getByRole('button', { name: 'Enviar enlace' }).click()
    await expect(page.getByRole('alert')).toContainText('Si el usuario existe')
  })
})
