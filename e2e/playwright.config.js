const { defineConfig, devices } = require('@playwright/test')
const dotenv = require('dotenv')

dotenv.config({ quiet: true })

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  retries: 1,
  timeout: 30000,
  reporter: 'html',
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
