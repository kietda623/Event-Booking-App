import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export function AppLayout() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const token = useAuthStore((state) => state.token)
  const logout = useAuthStore((state) => state.logout)

  const onLogout = () => {
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
          {token && <NavLink to="/favorites">Favorites</NavLink>}
          {token && <NavLink to="/bookings">Bookings</NavLink>}
          {token && <NavLink to="/tickets">Tickets</NavLink>}
          {token && <NavLink to="/profile">Profile</NavLink>}
          {user?.role === 'ADMIN' && <NavLink to="/admin/events">Admin Events</NavLink>}
          {user?.role === 'ADMIN' && <NavLink to="/admin/checkin">Check-in</NavLink>}
          {user?.role === 'ADMIN' && <NavLink to="/admin/analytics">Analytics</NavLink>}
        </nav>
        <div className="topbar-actions">
          {token ? (
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
