const { expect } = require('@playwright/test')

async function login(page, email, password) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: /^Login$/ }).click()
  await expect(page.locator('.user-chip')).toBeVisible()
}

async function openFirstEventDetail(page) {
  await page.goto('/')
  const firstCard = page.getByTestId('event-card').first()
  await expect(firstCard).toBeVisible()
  const eventTitle = (await firstCard.getByTestId('event-card-title').innerText()).trim()
  await firstCard.getByTestId('event-card-view').click()
  await expect(page.getByTestId('event-detail-title')).toContainText(eventTitle)
  return eventTitle
}

async function createPendingBooking(page, quantity = '1') {
  const eventTitle = await openFirstEventDetail(page)
  await page.getByTestId('book-now').click()
  await expect(page).toHaveURL(/\/events\/\d+\/book/)

  const tierOption = page.getByTestId('tier-option').first()
  if (await tierOption.isVisible().catch(() => false)) {
    await tierOption.click()
  }

  await page.getByLabel('Quantity').fill(quantity)
  await page.getByRole('button', { name: /Continue to payment/i }).click()
  await expect(page).toHaveURL(/\/bookings\/\d+\/pay/)
  const match = page.url().match(/\/bookings\/(\d+)\/pay/)
  return { bookingId: match?.[1], eventTitle }
}

async function payCurrentBooking(page) {
  await page.getByTestId('payment-method').selectOption('MOCK')
  await page.getByRole('button', { name: /Pay booking/i }).click()
  await expect(page).toHaveURL(/\/tickets/)
  await expect(page.getByTestId('ticket-card').first()).toBeVisible()
}

async function cancelBookingRow(page, bookingId) {
  page.once('dialog', async (dialog) => {
    await dialog.accept()
  })
  const row = page.locator(`[data-booking-id="${bookingId}"]`)
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: /^Cancel$/ }).click()
  await expect(row.getByTestId('booking-status')).toContainText('CANCELLED')
}

module.exports = {
  login,
  openFirstEventDetail,
  createPendingBooking,
  payCurrentBooking,
  cancelBookingRow,
}
