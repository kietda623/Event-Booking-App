import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { usersApi } from '../../api/users'
import { FormError } from '../../components/FieldErrors'
import { ErrorState, LoadingState } from '../../components/StateViews'

export function ProfilePage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ fullName: '', avatarUrl: '', reminderEnabled: false })
  const [apiError, setApiError] = useState(null)
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
        <button className="button primary" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving...' : 'Save profile'}
        </button>
      </form>
    </section>
  )
}
