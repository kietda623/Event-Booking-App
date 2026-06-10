const { test, expect } = require('@playwright/test')
const { cancelBookingRow, createPendingBooking, login, payCurrentBooking } = require('../helpers/flows')
const { testUserEmail, testUserPassword } = require('../helpers/fixtures')

test.describe('cancel booking', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, testUserEmail, testUserPassword)
  })

  test('cancels a pending booking from bookings list', async ({ page }) => {
    const { bookingId } = await createPendingBooking(page)

    await page.goto('/bookings')
    await cancelBookingRow(page, bookingId)
  })

  test('cancels a paid booking and shows refund notice', async ({ page }) => {
    const { bookingId } = await createPendingBooking(page)
    await payCurrentBooking(page)

    await page.goto(`/bookings/${bookingId}`)
    page.once('dialog', async (dialog) => {
      await dialog.accept()
    })
    await page.getByRole('button', { name: /^Cancel$/ }).click()

    await expect(page.getByTestId('booking-status')).toContainText('CANCELLED')
    await expect(page.getByText(/Refund status: PENDING/i)).toBeVisible()
  })
})
