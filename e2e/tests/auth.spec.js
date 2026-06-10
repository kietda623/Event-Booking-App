const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/flows')
const { testUserEmail, testUserPassword, uniqueEmail } = require('../helpers/fixtures')

test.describe('auth flows', () => {
  test('register redirects to login and shows success toast', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Full name').fill('Fresh E2E User')
    await page.getByLabel('Email').fill(uniqueEmail('fresh'))
    await page.getByLabel('Password').fill('Fresh123!')
    await page.getByRole('button', { name: /^Register$/ }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText(/Registration successful/i)).toBeVisible()
  })

  test('register duplicate email shows EMAIL_ALREADY_EXISTS', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Full name').fill('Duplicate E2E User')
    await page.getByLabel('Email').fill(testUserEmail)
    await page.getByLabel('Password').fill(testUserPassword)
    await page.getByRole('button', { name: /^Register$/ }).click()

    await expect(page.getByText('EMAIL_ALREADY_EXISTS')).toBeVisible()
  })

  test('login valid redirects home and shows user name in navbar', async ({ page }) => {
    await login(page, testUserEmail, testUserPassword)

    await expect(page).toHaveURL(/\/$/)
    await expect(page.locator('.user-chip')).toContainText(/E2E User|user\.e2e@example\.com/i)
  })

  test('login wrong password shows error', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Email').fill(testUserEmail)
    await page.getByLabel('Password').fill('WrongPassword123!')
    await page.getByRole('button', { name: /^Login$/ }).click()

    await expect(page.getByText(/INVALID_CREDENTIALS|Invalid/i)).toBeVisible()
  })

  test('logout redirects to login and protected routes stay protected', async ({ page }) => {
    await login(page, testUserEmail, testUserPassword)

    await page.getByRole('button', { name: /^Logout$/ }).click()
    await expect(page).toHaveURL(/\/login$/)

    await page.goto('/bookings')
    await expect(page).toHaveURL(/\/login$/)
  })
})
