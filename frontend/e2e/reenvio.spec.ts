import { expect, test } from './fixtures'
import { loginAdmin } from './helpers'

const FACTURA = {
  id: 999,
  tipoDocumento: '01',
  prefijo: 'FE',
  numero: 999,
  numeroCompleto: 'FE 999',
  cufe: null,
  cude: null,
  ambiente: 2,
  estado: 'EMITIDA',
  fechaEmision: '2026-11-19',
  horaEmision: '10:00:00',
  fechaVencimiento: null,
  moneda: 'COP',
  formaPago: ' CONTADO',
  medioPago: 'EFECTIVO',
  adquirente: {
    tipoDocumento: '13',
    numeroDocumento: '900123456',
    digitoVerificacion: null,
    nombre: 'Cliente E2E',
    email: 'cliente@example.com',
    direccion: null,
    municipio: 'Bogotá',
    regimen: 'COMUN',
    responsabilidadFiscal: 'IVA',
  },
  lineas: [],
  subtotal: 100000,
  totalImpuestos: 19000,
  totalRetenciones: 0,
  total: 119000,
  totalPagar: 119000,
  observaciones: null,
  qrUrl: null,
  respuestaDian: null,
  fechaValidacion: null,
  createdAt: '2026-11-19T10:00:00',
  eventos: [],
}

test.describe('Reenvío de facturas', () => {
  test('reenvía la factura por correo con la pasarela simulada', async ({ page }) => {
    let reenvioRecibido = false

    await loginAdmin(page)

    await page.route(/\/api\/facturacion\/\d+\/999(?:\/|$)/, async (route) => {
      if (route.request().method() === 'POST') {
        reenvioRecibido = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ enviado: true, destinatario: 'cliente@example.com' }),
        })
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(FACTURA),
      })
    })

    await page.goto('/app/facturacion/999')
    await expect(page.getByRole('heading', { name: 'FE 999' })).toBeVisible()

    await page.getByRole('button', { name: 'Reenviar' }).click()
    await page.getByLabel(/Correo electrónico/).fill('cliente@example.com')
    await page.getByRole('button', { name: 'Enviar' }).click()

    await expect(page.getByRole('alert')).toContainText('Factura reenviada a cliente@example.com.')
    expect(reenvioRecibido).toBe(true)
  })
})
