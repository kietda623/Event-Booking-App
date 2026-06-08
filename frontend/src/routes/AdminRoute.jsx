import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export function AdminRoute() {
  const token = useAuthStore((state) => state.token)
  const user = useAuthStore((state) => state.user)

  if (!token) {
    return <Navigate to="/login" replace />
  }

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
