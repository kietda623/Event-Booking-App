import { pushApi } from '../api/push'

export async function getPushEnabled() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    return false
  }
  const registration = await navigator.serviceWorker.ready
  const subscription = await registration.pushManager.getSubscription()
  return Boolean(subscription)
}

export async function enablePushNotifications() {
  if (!('Notification' in window) || !('serviceWorker' in navigator) || !('PushManager' in window)) {
    throw { code: 'PUSH_UNSUPPORTED', message: 'Push notifications are not supported in this browser.' }
  }
  const permission = await Notification.requestPermission()
  if (permission !== 'granted') {
    throw { code: 'PUSH_DENIED', message: 'Notification permission was not granted.' }
  }
  const keyResponse = await pushApi.publicKey()
  const publicKey = keyResponse.publicKey
  if (!publicKey) {
    throw { code: 'PUSH_NOT_CONFIGURED', message: 'Push notifications are not configured yet.' }
  }
  const registration = await navigator.serviceWorker.ready
  const existing = await registration.pushManager.getSubscription()
  const subscription =
    existing ||
    (await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(publicKey),
    }))
  await pushApi.subscribe(subscription.toJSON())
}

export async function disablePushNotifications() {
  if (!('serviceWorker' in navigator)) return
  const registration = await navigator.serviceWorker.ready
  const subscription = await registration.pushManager.getSubscription()
  if (!subscription) return
  await pushApi.unsubscribe({ endpoint: subscription.endpoint, keys: { p256dh: 'unused', auth: 'unused' } })
  await subscription.unsubscribe()
}

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  return Uint8Array.from([...rawData].map((char) => char.charCodeAt(0)))
}
