import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { pushApi } from '../../api/push'
import { usersApi } from '../../api/users'
import { FormError } from '../../components/FieldErrors'
import { ErrorState, LoadingState } from '../../components/StateViews'

export function ProfilePage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ fullName: '', avatarUrl: '', reminderEnabled: false })
  const [apiError, setApiError] = useState(null)
  const [pushError, setPushError] = useState(null)
  const [pushEnabled, setPushEnabled] = useState(false)
  const [pushPending, setPushPending] = useState(false)
  const [toast, setToast] = useState('')
  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: usersApi.profile,
  })

  useEffect(() => {
    if (profileQuery.data) {
      setForm((current) => ({
        ...current,
        fullName: profileQuery.data.fullName || '',
        avatarUrl: profileQuery.data.avatar || '',
      }))
    }
  }, [profileQuery.data])

  useEffect(() => {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) return
    navigator.serviceWorker.ready
      .then((registration) => registration.pushManager.getSubscription())
      .then((subscription) => setPushEnabled(Boolean(subscription)))
      .catch(() => setPushEnabled(false))
  }, [])

  const mutation = useMutation({
    mutationFn: async (payload) => {
      const profile = await usersApi.updateProfile({ fullName: payload.fullName, avatar: payload.avatarUrl })
      await usersApi.updateReminders({ eventReminder: payload.reminderEnabled })
      return profile
    },
    onSuccess: () => {
      setToast('Profile saved')
      setApiError(null)
      queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
    onError: setApiError,
  })

  if (profileQuery.isLoading) return <LoadingState label="Loading profile..." />
  if (profileQuery.isError) return <ErrorState error={profileQuery.error} title="Could not load profile" />

  const onSubmit = (event) => {
    event.preventDefault()
    setToast('')
    setApiError(null)
    mutation.mutate(form)
  }

  const onPushToggle = async (event) => {
    const enabled = event.target.checked
    setPushError(null)
    setPushPending(true)
    try {
      if (enabled) {
        await enablePush()
        setPushEnabled(true)
      } else {
        await disablePush()
        setPushEnabled(false)
      }
    } catch (error) {
      setPushError(error)
      setPushEnabled(!enabled)
    } finally {
      setPushPending(false)
    }
  }

  return (
    <section className="narrow-page">
      <div className="page-header">
        <div>
          <p className="eyebrow">Account</p>
          <h1>Profile</h1>
        </div>
      </div>
      <form className="form-card" onSubmit={onSubmit}>
        <FormError error={apiError} />
        {toast && <div className="toast">{toast}</div>}
        <label>
          Full name
          <input
            value={form.fullName}
            onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))}
          />
        </label>
        <label>
          Avatar URL
          <input
            value={form.avatarUrl}
            onChange={(event) => setForm((current) => ({ ...current, avatarUrl: event.target.value }))}
          />
        </label>
        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={form.reminderEnabled}
            onChange={(event) => setForm((current) => ({ ...current, reminderEnabled: event.target.checked }))}
          />
          Reminder enabled
        </label>
        <label className="checkbox-row">
          <input type="checkbox" checked={pushEnabled} disabled={pushPending} onChange={onPushToggle} />
          Enable push notifications
        </label>
        <FormError error={pushError} />
        <button className="button primary" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving...' : 'Save profile'}
        </button>
      </form>
    </section>
  )
}

async function enablePush() {
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

async function disablePush() {
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
