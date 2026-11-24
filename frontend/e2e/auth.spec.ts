import { expect, test } from './fixtures'
import { loginAdmin } from './helpers'

test.describe('Autenticación', () => {
  test('muestra la landing pública con acceso al registro', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'De la venta a la factura, sin fricción' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Crear cuenta gratis' }).first()).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Planes simples' })).toBeVisible()
  })

  test('muestra error con credenciales inválidas', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario').fill('admin')
    await page.getByLabel('Contraseña').fill('clave-incorrecta')
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await expect(page.getByRole('alert')).toContainText('Credenciales inválidas')
  })

  test('inicia sesión y llega al panel', async ({ page }) => {
    await loginAdmin(page)
  })

  test('permite solicitar recuperación de contraseña', async ({ page }) => {
    await page.goto('/recuperar')
    await page.getByLabel('Usuario o correo').fill('admin')
    await page.getByRole('button', { name: 'Enviar enlace' }).click()
    await expect(page.getByText('Si el usuario existe')).toBeVisible()
  })
})
