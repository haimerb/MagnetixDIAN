import { expect, test } from '@playwright/test'
import { loginAdmin } from './helpers'

test.describe('Pagos con pasarela simulada', () => {
  test('activa un plan y muestra el comprobante', async ({ page }) => {
    await loginAdmin(page)
    await page.goto('/app/suscripcion')

    const botones = page.getByRole('button', { name: 'Cambiar a este plan' })
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
    await expect(page.getByRole('alert')).toContainText('Pago aprobado. Plan')
    await expect(page.getByText('Proveedor: SIMULADO')).toBeVisible()
  })
})
