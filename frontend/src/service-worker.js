self.addEventListener('push', (event) => {
  const data = event.data ? event.data.json() : {}
  const title = data.title || 'Event Booking'
  const options = {
    body: data.body || 'You have a new notification.',
    icon: '/vite.svg',
    badge: '/vite.svg',
  }

  event.waitUntil(self.registration.showNotification(title, options))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  event.waitUntil(self.clients.openWindow('/tickets'))
})
