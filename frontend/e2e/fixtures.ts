import { test as base } from '@playwright/test'
import { mockApi } from './helpers'

export { expect } from '@playwright/test'

export const test = base.extend<{ apiMockAuto: void }>({
  apiMockAuto: [
    async ({ page }, use) => {
      await mockApi(page)
      await use()
    },
    { auto: true },
  ],
})
