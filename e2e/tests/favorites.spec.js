const { test, expect } = require('@playwright/test')
const { login, openFirstEventDetail } = require('../helpers/flows')
const { testUserEmail, testUserPassword } = require('../helpers/fixtures')

test.describe('favorites flow', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, testUserEmail, testUserPassword)
  })

  test('favorites and unfavorites an event', async ({ page }) => {
    const eventTitle = await openFirstEventDetail(page)
    const heart = page.getByTestId('favorite-toggle')

    if ((await heart.getAttribute('aria-pressed')) === 'true') {
      await heart.click()
      await expect(heart).toHaveAttribute('aria-pressed', 'false')
    }

    await heart.click()
    await expect(heart).toHaveAttribute('aria-pressed', 'true')

    await page.goto('/favorites')
    await expect(page.getByTestId('event-card').filter({ hasText: eventTitle }).first()).toBeVisible()

    await page.getByTestId('event-card').filter({ hasText: eventTitle }).first().getByTestId('favorite-toggle').click()
    await expect(page.getByTestId('event-card').filter({ hasText: eventTitle })).toHaveCount(0)
  })
})
