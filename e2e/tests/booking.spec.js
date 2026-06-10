const { test, expect } = require('@playwright/test')
const { createPendingBooking, login, payCurrentBooking } = require('../helpers/flows')
const { testUserEmail, testUserPassword } = require('../helpers/fixtures')

test.describe('booking flow', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, testUserEmail, testUserPassword)
  })

  test('creates and pays a booking with MOCK payment', async ({ page }) => {
    const { bookingId, eventTitle } = await createPendingBooking(page)

    await payCurrentBooking(page)
    const ticketCard = page.getByTestId('ticket-card').filter({ hasText: eventTitle }).first()
    await expect(ticketCard).toBeVisible()
    await expect(ticketCard.getByTestId('ticket-code')).toBeVisible()

    await page.goto('/bookings')
    const row = page.locator(`[data-booking-id="${bookingId}"]`)
    await expect(row).toBeVisible()
    await expect(row.getByTestId('booking-status')).toContainText('PAID')

    await page.goto('/tickets')
    await expect(page.getByTestId('ticket-card').filter({ hasText: eventTitle }).first()).toBeVisible()
  })
})
