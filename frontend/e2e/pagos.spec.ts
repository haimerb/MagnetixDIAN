import { expect, test } from './fixtures'
import { loginAdmin } from './helpers'

test.describe('Pagos con pasarela simulada', () => {
  test('activa un plan y muestra el comprobante', async ({ page }) => {
    await loginAdmin(page)
    await page.goto('/app/suscripcion')

    await expect(page.getByText('Planes disponibles')).toBeVisible()

    const botones = page.getByRole('button', { name: 'Cambiar a este plan' })
    await botones.first().waitFor({ state: 'visible' })
    const total = await botones.count()
    let cambioRealizado = false
    for (let i = 0; i < total; i += 1) {
      const boton = botones.nth(i)
      if (await boton.isEnabled()) {
        await boton.click()
        cambioRealizado = true
        break
      }
    }

    expect(cambioRealizado).toBe(true)
    await expect(page.getByRole('alert').filter({ hasText: 'Pago aprobado. Plan' })).toContainText('Pago aprobado. Plan')
    await expect(page.getByText('Proveedor: SIMULADO')).toBeVisible()
  })
})
