import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { useAuthStore } from '../store/authStore'

export function AppLayout() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)

  const onLogout = async () => {
    try {
      await authApi.logout()
    } catch {
      // Local logout still proceeds if the session already expired.
    }
    logout()
    navigate('/')
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to="/" className="brand">
          Event Booking
        </Link>
        <nav className="nav-links" aria-label="Primary navigation">
          <NavLink to="/">Events</NavLink>
          {user && <NavLink to="/favorites">Favorites</NavLink>}
          {user && <NavLink to="/bookings">Bookings</NavLink>}
          {user && <NavLink to="/tickets">Tickets</NavLink>}
          {user && <NavLink to="/profile">Profile</NavLink>}
          {user?.role === 'ADMIN' && <NavLink to="/admin/events">Admin Events</NavLink>}
          {user?.role === 'ADMIN' && <NavLink to="/admin/checkin">Check-in</NavLink>}
          {user?.role === 'ADMIN' && <NavLink to="/admin/analytics">Analytics</NavLink>}
        </nav>
        <div className="topbar-actions">
          {user ? (
            <>
              <span className="user-chip">{user?.fullName || user?.email || 'User'}</span>
              <button className="button ghost" type="button" onClick={onLogout}>
                Logout
              </button>
            </>
          ) : (
            <>
              <Link className="button ghost" to="/login">
                Login
              </Link>
              <Link className="button primary" to="/register">
                Register
              </Link>
            </>
          )}
        </div>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
