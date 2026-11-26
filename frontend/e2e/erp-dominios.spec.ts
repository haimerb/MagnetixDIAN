import { type Page } from '@playwright/test'
import { expect, test } from './fixtures'
import { loginAdmin } from './helpers'

const COMPRAS = [
  {
    id: 11,
    consecutivo: 'OC-2026-001',
    proveedor: 'Proveedor Andino',
    estado: 'CONTABILIZADA',
    total: 1190000,
    fecha: '2026-11-20',
  },
]

const INVENTARIO = [
  {
    id: 21,
    producto: 'Laptop X1',
    bodega: 'Bodega principal',
    tipo: 'Entrada',
    cantidad: 12,
    saldo: 30,
    fecha: '2026-11-21',
  },
]

const TESORERIA = [
  {
    id: 31,
    cuenta: 'Cuenta corriente',
    descripcion: 'Pago proveedor',
    tipo: 'EGRESO',
    valor: 450000,
    saldo: 2750000,
    fecha: '2026-11-22',
  },
]

async function responderDominios(page: Page) {
  await page.route('**/api/empresas/1/erp/compras/ordenes', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(COMPRAS),
    }),
  )
  await page.route('**/api/empresas/1/erp/inventario/movimientos', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(INVENTARIO),
    }),
  )
  await page.route('**/api/empresas/1/erp/tesoreria/movimientos', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(TESORERIA),
    }),
  )
}

async function abrirSubmenu(page: Page) {
  await page
    .getByRole('navigation', { name: 'Navegación principal' })
    .getByRole('button', { name: 'ERP', exact: true })
    .click()
}

test.describe('ERP · dominios', () => {
  test.beforeEach(async ({ page }) => {
    await responderDominios(page)
    await loginAdmin(page)
  })

  test('el submenú ERP expone los seis dominios registrados', async ({ page }) => {
    await abrirSubmenu(page)

    for (const etiqueta of [
      'Ventas',
      'Compras',
      'Inventario',
      'Tesorería',
      'Exógena',
      'Integraciones',
    ]) {
      await expect(page.getByRole('menuitem', { name: etiqueta, exact: true })).toBeVisible()
    }
  })

  test('Compras abre desde el menú y muestra el estado contable', async ({ page }) => {
    await abrirSubmenu(page)
    await page.getByRole('menuitem', { name: 'Compras', exact: true }).click()

    await expect(page).toHaveURL(/\/app\/erp\/compras$/)
    await expect(page.getByRole('menu')).toHaveCount(0)
    await expect(page.getByRole('heading', { name: 'Compras', exact: true, level: 1 })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'OC-2026-001' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Proveedor Andino' })).toBeVisible()
    await expect(page.getByText('Contabilizada', { exact: true })).toBeVisible()
  })

  test('Inventario abre desde el menú y muestra movimientos de bodega', async ({ page }) => {
    await abrirSubmenu(page)
    await page.getByRole('menuitem', { name: 'Inventario', exact: true }).click()

    await expect(page).toHaveURL(/\/app\/erp\/inventario$/)
    await expect(page.getByRole('heading', { name: 'Inventario', exact: true, level: 1 })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Laptop X1' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Bodega principal' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Entrada' })).toBeVisible()
  })

  test('Tesorería abre desde el menú y muestra ingresos y egresos', async ({ page }) => {
    await abrirSubmenu(page)
    await page.getByRole('menuitem', { name: 'Tesorería', exact: true }).click()

    await expect(page).toHaveURL(/\/app\/erp\/tesoreria$/)
    await expect(page.getByRole('heading', { name: 'Tesorería', exact: true, level: 1 })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Cuenta corriente' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Pago proveedor' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'EGRESO' })).toBeVisible()
  })

  test('el submenú ERP también funciona en el drawer móvil', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await page.getByRole('button', { name: 'Abrir menú de navegación' }).click()
    await page
      .getByRole('list', { name: 'Navegación principal' })
      .getByRole('button', { name: 'ERP', exact: true })
      .click()
    await page
      .getByRole('list', { name: 'Submenú ERP' })
      .getByRole('button', { name: 'Tesorería', exact: true })
      .click()

    await expect(page).toHaveURL(/\/app\/erp\/tesoreria$/)
    await expect(page.getByRole('list', { name: 'Submenú ERP' })).toHaveCount(0)
    await expect(page.getByRole('heading', { name: 'Tesorería', exact: true, level: 1 })).toBeVisible()
  })
})
