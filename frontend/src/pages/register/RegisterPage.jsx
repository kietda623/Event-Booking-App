import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '../../api/auth'
import { FieldError, FormError } from '../../components/FieldErrors'

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [apiError, setApiError] = useState(null)

  const mutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: () => navigate('/login'),
    onError: setApiError,
  })

  const onSubmit = (event) => {
    event.preventDefault()
    setApiError(null)
    mutation.mutate(form)
  }

  return (
    <section className="auth-panel">
      <div>
        <p className="eyebrow">Create account</p>
        <h1>Register</h1>
      </div>
      <form className="form-card" onSubmit={onSubmit}>
        <FormError error={apiError} />
        <label>
          Full name
          <input
            value={form.fullName}
            onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))}
            required
          />
          <FieldError error={apiError} field="fullName" />
        </label>
        <label>
          Email
          <input
            type="email"
            value={form.email}
            onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
            required
          />
          <FieldError error={apiError} field="email" />
        </label>
        <label>
          Password
          <input
            type="password"
            value={form.password}
            onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
            required
          />
          <FieldError error={apiError} field="password" />
        </label>
        <button className="button primary" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating...' : 'Register'}
        </button>
        <p>
          Already registered? <Link to="/login">Login</Link>
        </p>
      </form>
    </section>
  )
}
