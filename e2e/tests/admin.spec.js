const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/flows')
const { adminEmail, adminPassword, checkinTicketCode, seedEventTitle } = require('../helpers/fixtures')

test.describe('admin flows', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, adminEmail, adminPassword)
  })

  test('manages events, views analytics, and checks in a ticket', async ({ page }) => {
    const title = `E2E Admin Event ${Date.now()}`
    const updatedTitle = `${title} Updated`
    const futureDate = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16)

    await page.goto('/admin/events')
    await expect(page.getByTestId('admin-events-table')).toBeVisible()

    await page.getByRole('link', { name: /New event/i }).click()
    await page.getByLabel('Title').fill(title)
    await page.getByLabel('Location').fill('E2E Hall')
    await page.getByLabel('Start time').fill(futureDate)
    await page.getByLabel('Price', { exact: true }).fill('100000')
    await page.getByLabel('Total tickets', { exact: true }).fill('50')
    await page.getByLabel('Image URL').fill('https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200')
    await page.getByLabel('Latitude').fill('10.776')
    await page.getByLabel('Longitude').fill('106.700')
    await page.getByLabel('Description', { exact: true }).fill('Created by Playwright E2E')
    await page.getByLabel('Tier name').fill('GENERAL')
    await page.getByLabel('Tier price').fill('100000')
    await page.getByLabel('Total quantity').fill('50')
    await page.getByRole('button', { name: /Save event/i }).click()

    await expect(page).toHaveURL(/\/admin\/events$/)
    await expect(page.getByTestId('admin-event-row').filter({ hasText: title })).toBeVisible()

    await page.getByTestId('admin-event-row').filter({ hasText: title }).getByRole('link', { name: /Edit/i }).click()
    await page.getByLabel('Title').fill(updatedTitle)
    await page.getByRole('button', { name: /Save event/i }).click()
    await expect(page.getByTestId('admin-event-row').filter({ hasText: updatedTitle })).toBeVisible()

    page.once('dialog', async (dialog) => {
      await dialog.accept()
    })
    await page.getByTestId('admin-event-row').filter({ hasText: updatedTitle }).getByRole('button', { name: /Delete/i }).click()
    await expect(page.getByTestId('admin-event-row').filter({ hasText: updatedTitle })).toHaveCount(0)

    await page.goto('/admin/analytics')
    for (const testId of ['metric-total-events', 'metric-total-users', 'metric-total-bookings', 'metric-total-revenue']) {
      const metric = page.getByTestId(testId)
      await expect(metric).toBeVisible()
      const numericValue = Number((await metric.locator('strong').innerText()).replace(/[^\d]/g, ''))
      expect(numericValue).toBeGreaterThan(0)
    }

    await page.goto('/admin/checkin')
    await page.getByLabel('Ticket code').fill(checkinTicketCode)
    await page.getByRole('button', { name: /Check in/i }).click()
    await expect(page.getByText(seedEventTitle)).toBeVisible()
    await expect(page.getByText(/checked in|success/i)).toBeVisible()
  })
})
