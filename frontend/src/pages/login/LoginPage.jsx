import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '../../api/auth'
import { FieldError, FormError } from '../../components/FieldErrors'
import { useAuthStore } from '../../store/authStore'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const login = useAuthStore((state) => state.login)
  const [form, setForm] = useState({ email: '', password: '' })
  const [apiError, setApiError] = useState(null)
  const from = location.state?.from?.pathname || '/'

  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      login(data.user)
      navigate(from, { replace: true })
    },
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
        <p className="eyebrow">Welcome back</p>
        <h1>Login</h1>
      </div>
      <form className="form-card" onSubmit={onSubmit}>
        {location.state?.successMessage && <div className="toast">{location.state.successMessage}</div>}
        <FormError error={apiError} />
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
          {mutation.isPending ? 'Logging in...' : 'Login'}
        </button>
        <p>
          No account yet? <Link to="/register">Register</Link>
        </p>
      </form>
    </section>
  )
}
