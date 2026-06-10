const { test, expect } = require('@playwright/test')
const { openFirstEventDetail } = require('../helpers/flows')

test.describe('event browsing', () => {
  test('homepage loads event cards', async ({ page }) => {
    await page.goto('/')

    await expect(page.getByTestId('event-card').first()).toBeVisible()
    expect(await page.getByTestId('event-card').count()).toBeGreaterThanOrEqual(1)
  })

  test('popular filter updates URL and reloads cards', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Popular' }).click()

    await expect(page).toHaveURL(/type=popular/)
    await expect(page.getByTestId('event-card').first()).toBeVisible()
  })

  test('upcoming filter updates URL and reloads cards', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Upcoming' }).click()

    await expect(page).toHaveURL(/type=upcoming/)
    await expect(page.getByTestId('event-card').first()).toBeVisible()
  })

  test('nearby filter uses geolocation and shows distance badges', async ({ page, context }) => {
    await context.grantPermissions(['geolocation'])
    await context.setGeolocation({ latitude: 10.776, longitude: 106.7 })

    await page.goto('/')
    await page.getByRole('button', { name: 'Nearby' }).click()

    await expect(page).toHaveURL(/type=nearby/)
    await expect(page.getByTestId('distance-badge').first()).toBeVisible()
  })

  test('event detail shows title price and tier cards', async ({ page }) => {
    await openFirstEventDetail(page)

    await expect(page.getByTestId('event-detail-title')).toBeVisible()
    await expect(page.getByTestId('event-detail-price')).toBeVisible()
    await expect(page.getByTestId('tier-card').first()).toBeVisible()
  })

  test('guest Book Now redirects to login', async ({ page }) => {
    await openFirstEventDetail(page)

    await page.getByTestId('guest-book-now').click()
    await expect(page).toHaveURL(/\/login$/)
  })
})
