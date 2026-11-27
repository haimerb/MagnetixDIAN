import { type Page } from '@playwright/test'
import { expect, test } from './fixtures'
import { loginAdmin } from './helpers'

const COMPRAS = [
  {
    id: 11,
    empresaId: 1,
    numero: 'OC-2026-001',
    proveedorId: '900123456',
    proveedorNombre: 'Proveedor Andino',
    estado: 'CONTABILIZADA',
    subtotal: 1000000,
    impuestos: 190000,
    total: 1190000,
    observaciones: null,
    fecha: '2026-11-20',
    createdAt: '2026-11-20T00:00:00Z',
    updatedAt: '2026-11-20T00:00:00Z',
  },
]

const RECEPCIONES = [
  {
    id: 12,
    empresaId: 1,
    compraId: 11,
    numero: 'RC-2026-001',
    fecha: '2026-11-21',
    bodega: 'Bodega principal',
    estado: 'CONTABILIZADA',
    total: 1000000,
    observaciones: null,
    createdAt: '2026-11-21T00:00:00Z',
    updatedAt: '2026-11-21T00:00:00Z',
    lineas: [],
  },
]

const INVENTARIO = [
  {
    id: 21,
    empresaId: 1,
    tipo: 'ENTRADA',
    bodega: 'Bodega principal',
    productoId: 'PROD-1',
    descripcion: 'Laptop X1',
    cantidad: 12,
    costoUnitario: 1000000,
    costoPromedio: 1000000,
    existenciaAnterior: 18,
    existenciaNueva: 30,
    fecha: '2026-11-21',
    referenciaTipo: null,
    referenciaId: null,
    estado: 'ACTIVO',
    observaciones: null,
    createdAt: '2026-11-21T00:00:00Z',
  },
]

const EXISTENCIAS = [
  {
    empresaId: 1,
    bodega: 'Bodega principal',
    productoId: 'PROD-1',
    existencia: 30,
    costoPromedio: 1000000,
    updatedAt: '2026-11-21T00:00:00Z',
  },
]

const TESORERIA = [
  {
    id: 31,
    empresaId: 1,
    cuentaBancariaId: 5,
    tipo: 'EGRESO',
    fecha: '2026-11-22',
    valor: 450000,
    saldoAnterior: 3200000,
    saldoNuevo: 2750000,
    referenciaTipo: 'PAGO',
    referenciaId: '3',
    estado: 'ACTIVO',
    descripcion: 'Pago proveedor',
    createdAt: '2026-11-22T00:00:00Z',
  },
]

const CUENTAS = [
  {
    id: 5,
    empresaId: 1,
    banco: 'Banco de Pruebas',
    numeroCuenta: '123456789',
    tipoCuenta: 'CORRIENTE',
    moneda: 'COP',
    saldo: 2750000,
    activa: true,
    estado: 'ACTIVA',
    createdAt: '2026-11-01T00:00:00Z',
    updatedAt: '2026-11-22T00:00:00Z',
  },
]

const PAGOS = [
  {
    id: 3,
    empresaId: 1,
    compraId: 11,
    cuentaBancariaId: 5,
    proveedorId: '900123456',
    proveedorNombre: 'Proveedor Andino',
    fecha: '2026-11-22',
    valor: 450000,
    medioPago: 'TRANSFERENCIA',
    referencia: 'TR-001',
    estado: 'ACTIVO',
    observaciones: null,
    createdAt: '2026-11-22T00:00:00Z',
  },
]

const COBROS = [
  {
    id: 4,
    empresaId: 1,
    pedidoId: 2,
    cuentaBancariaId: 5,
    clienteId: '800123456',
    clienteNombre: 'Cliente Andino',
    fecha: '2026-11-23',
    valor: 250000,
    medioPago: 'TRANSFERENCIA',
    referencia: 'TR-002',
    estado: 'ACTIVO',
    observaciones: null,
    createdAt: '2026-11-23T00:00:00Z',
  },
]

async function responderDominios(page: Page) {
  const responder = (ruta: string, get: unknown, post: unknown) =>
    page.route(`**/api/empresas/1/erp/${ruta}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(route.request().method() === 'POST' ? post : get),
      }),
    )

  await responder('compras/ordenes', COMPRAS, COMPRAS[0])
  await responder('compras/recepciones', RECEPCIONES, RECEPCIONES[0])
  await responder('inventario/movimientos', INVENTARIO, INVENTARIO[0])
  await responder('inventario/existencias', EXISTENCIAS, EXISTENCIAS[0])
  await responder('tesoreria/movimientos', TESORERIA, TESORERIA[0])
  await responder('tesoreria/cuentas-bancarias', CUENTAS, CUENTAS[0])
  await responder('tesoreria/pagos-proveedor', PAGOS, PAGOS[0])
  await responder('tesoreria/cobros-cliente', COBROS, COBROS[0])
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
    expect(await page.evaluate(() => document.body.style.overflow)).not.toBe('hidden')
    await expect(page.getByRole('heading', { name: 'Compras', exact: true, level: 1 })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'OC-2026-001' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Proveedor Andino' })).toBeVisible()
    await expect(page.getByText('Contabilizada', { exact: true }).first()).toBeVisible()
  })

  test('Inventario abre desde el menú y muestra movimientos de bodega', async ({ page }) => {
    await abrirSubmenu(page)
    await page.getByRole('menuitem', { name: 'Inventario', exact: true }).click()

    await expect(page).toHaveURL(/\/app\/erp\/inventario$/)
    await expect(page.getByRole('menu')).toHaveCount(0)
    expect(await page.evaluate(() => document.body.style.overflow)).not.toBe('hidden')
    await expect(page.getByRole('heading', { name: 'Inventario', exact: true, level: 1 })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Laptop X1' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Bodega principal' }).first()).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Entrada' })).toBeVisible()
  })

  test('Tesorería abre desde el menú y muestra ingresos y egresos', async ({ page }) => {
    await abrirSubmenu(page)
    await page.getByRole('menuitem', { name: 'Tesorería', exact: true }).click()

    await expect(page).toHaveURL(/\/app\/erp\/tesoreria$/)
    await expect(page.getByRole('menu')).toHaveCount(0)
    expect(await page.evaluate(() => document.body.style.overflow)).not.toBe('hidden')
    await expect(page.getByRole('heading', { name: 'Tesorería', exact: true, level: 1 })).toBeVisible()
    await expect(page.getByRole('cell', { name: '#5' }).first()).toBeVisible()
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

  test('Fase 2 · Compras crea una orden de compra', async ({ page }) => {
    await page.goto('/app/erp/compras')
    await page.getByLabel('Número', { exact: true }).fill('OC-2026-002')
    await page.getByLabel('Proveedor', { exact: true }).fill('Proveedor Fase 2')
    await page.getByRole('button', { name: 'Crear orden' }).click()

    await expect(page.getByText('Orden de compra OC-2026-001 creada.')).toBeVisible()
  })

  test('Fase 2 · Inventario registra una entrada', async ({ page }) => {
    await page.goto('/app/erp/inventario')
    await page.getByLabel('Producto', { exact: true }).fill('PROD-2')
    await page.getByRole('button', { name: 'Registrar movimiento' }).click()

    await expect(page.getByText('Movimiento ENTRADA registrado. Saldo: 30.')).toBeVisible()
  })

  test('Fase 2 · Tesorería crea cuenta, paga y cobra', async ({ page }) => {
    await page.goto('/app/erp/tesoreria')
    await page.getByLabel('Banco', { exact: true }).fill('Banco Fase 2')
    await page.getByLabel('Número de cuenta', { exact: true }).fill('987654321')
    await page.getByRole('button', { name: 'Crear cuenta' }).click()
    await expect(page.getByText('Cuenta 123456789 creada.')).toBeVisible()

    await expect(page.getByRole('button', { name: 'Registrar pago' })).toBeEnabled()
    await page.getByRole('button', { name: 'Registrar pago' }).click()
    await expect(page.getByText(/Pago a proveedor registrado/)).toBeVisible()

    await expect(page.getByRole('button', { name: 'Registrar cobro' })).toBeEnabled()
    await page.getByRole('button', { name: 'Registrar cobro' }).click()
    await expect(page.getByText(/Cobro a cliente registrado/)).toBeVisible()
  })
})
