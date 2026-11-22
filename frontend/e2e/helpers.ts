import { expect, type Page } from '@playwright/test'

export async function loginAdmin(page: Page) {
  const ip = `198.51.100.${Math.floor(Math.random() * 254) + 1}`
  await page.setExtraHTTPHeaders({ 'X-Forwarded-For': ip })
  await page.goto('/login')
  await page.getByLabel('Usuario').fill('admin')
  await page.getByLabel('Contraseña').fill('admin123')
  await page.getByRole('button', { name: 'Ingresar' }).click()
  await expect(page.getByRole('heading', { name: 'Panel de reportes' })).toBeVisible()
}
