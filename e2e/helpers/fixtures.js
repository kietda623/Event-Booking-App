const adminEmail = process.env.ADMIN_EMAIL || 'admin.e2e@example.com'
const adminPassword = process.env.ADMIN_PASSWORD || 'Admin123!'
const testUserEmail = process.env.TEST_USER_EMAIL || 'user.e2e@example.com'
const testUserPassword = process.env.TEST_USER_PASSWORD || 'User123!'

const seedEventTitle = 'E2E Seed Future Festival'
const checkinTicketCode = process.env.E2E_CHECKIN_TICKET_CODE || 'E2E-CHECKIN-0001'

function uniqueEmail(prefix = 'user') {
  return `${prefix}.${Date.now()}.${Math.random().toString(36).slice(2)}@example.com`
}

module.exports = {
  adminEmail,
  adminPassword,
  testUserEmail,
  testUserPassword,
  seedEventTitle,
  checkinTicketCode,
  uniqueEmail,
}
