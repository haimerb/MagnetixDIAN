import { expect, test } from './fixtures'

test.describe('Registro self-service', () => {
  test('la landing lleva al registro', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Información exógena DIAN sin fricción' })).toBeVisible()
    await page.getByRole('link', { name: 'Crear cuenta gratis' }).first().click()
    await expect(page).toHaveURL(/\/registro$/)
  })

  test('crea empresa, usuario y plan gratuito', async ({ page }) => {
    const sufijo = Date.now().toString().slice(-8)
    const username = `cli${sufijo}`

    await page.goto('/registro')
    await page.getByLabel(/NIT/).fill(`9${sufijo}`)
    await page.getByLabel(/Razón social/).fill(`Empresa ${sufijo} SAS`)
    await page.getByLabel(/Nombre completo/).fill('Cliente E2E')
    await page.getByLabel(/Correo electrónico/).fill(`${username}@ejemplo.com`)
    await page.getByLabel(/Usuario/).fill(username)
    await page.getByLabel(/Contraseña/).fill('clave1234')
    await page.getByRole('checkbox', { name: 'Acepto los términos y condiciones' }).check()
    await page.getByRole('button', { name: 'Crear cuenta gratis' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('alert')).toContainText('Cuenta creada correctamente')
    await expect(page.getByLabel(/Usuario/)).toHaveValue(username)
  })
})
