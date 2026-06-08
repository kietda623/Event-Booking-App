import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/AppLayout.jsx'
import { LoadingState } from './components/StateViews.jsx'
import { AdminRoute } from './routes/AdminRoute.jsx'
import { GuestRoute } from './routes/GuestRoute.jsx'
import { PrivateRoute } from './routes/PrivateRoute.jsx'

const AdminAnalyticsPage = lazy(() =>
  import('./pages/admin-analytics/AdminAnalyticsPage.jsx').then((module) => ({ default: module.AdminAnalyticsPage })),
)
const AdminCheckInPage = lazy(() =>
  import('./pages/admin-checkin/AdminCheckInPage.jsx').then((module) => ({ default: module.AdminCheckInPage })),
)
const AdminEventFormPage = lazy(() =>
  import('./pages/admin-event-form/AdminEventFormPage.jsx').then((module) => ({ default: module.AdminEventFormPage })),
)
const AdminEventsPage = lazy(() =>
  import('./pages/admin-events/AdminEventsPage.jsx').then((module) => ({ default: module.AdminEventsPage })),
)
const BookingDetailPage = lazy(() =>
  import('./pages/booking-detail/BookingDetailPage.jsx').then((module) => ({ default: module.BookingDetailPage })),
)
const BookingPayPage = lazy(() =>
  import('./pages/booking-pay/BookingPayPage.jsx').then((module) => ({ default: module.BookingPayPage })),
)
const BookEventPage = lazy(() =>
  import('./pages/book-event/BookEventPage.jsx').then((module) => ({ default: module.BookEventPage })),
)
const BookingsPage = lazy(() =>
  import('./pages/bookings/BookingsPage.jsx').then((module) => ({ default: module.BookingsPage })),
)
const EventDetailPage = lazy(() =>
  import('./pages/event-detail/EventDetailPage.jsx').then((module) => ({ default: module.EventDetailPage })),
)
const EventsPage = lazy(() => import('./pages/events/EventsPage.jsx').then((module) => ({ default: module.EventsPage })))
const FavoritesPage = lazy(() =>
  import('./pages/favorites/FavoritesPage.jsx').then((module) => ({ default: module.FavoritesPage })),
)
const LoginPage = lazy(() => import('./pages/login/LoginPage.jsx').then((module) => ({ default: module.LoginPage })))
const ProfilePage = lazy(() =>
  import('./pages/profile/ProfilePage.jsx').then((module) => ({ default: module.ProfilePage })),
)
const RegisterPage = lazy(() =>
  import('./pages/register/RegisterPage.jsx').then((module) => ({ default: module.RegisterPage })),
)
const TicketsPage = lazy(() =>
  import('./pages/tickets/TicketsPage.jsx').then((module) => ({ default: module.TicketsPage })),
)

function App() {
  return (
    <Suspense fallback={<LoadingState label="Loading page..." />}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<EventsPage />} />
          <Route path="events/:id" element={<EventDetailPage />} />
          <Route element={<GuestRoute />}>
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
          </Route>
          <Route element={<PrivateRoute />}>
            <Route path="events/:id/book" element={<BookEventPage />} />
            <Route path="favorites" element={<FavoritesPage />} />
            <Route path="bookings" element={<BookingsPage />} />
            <Route path="bookings/:id" element={<BookingDetailPage />} />
            <Route path="bookings/:id/pay" element={<BookingPayPage />} />
            <Route path="tickets" element={<TicketsPage />} />
            <Route path="profile" element={<ProfilePage />} />
          </Route>
          <Route element={<AdminRoute />}>
            <Route path="admin/events" element={<AdminEventsPage />} />
            <Route path="admin/events/new" element={<AdminEventFormPage />} />
            <Route path="admin/events/:id/edit" element={<AdminEventFormPage />} />
            <Route path="admin/checkin" element={<AdminCheckInPage />} />
            <Route path="admin/analytics" element={<AdminAnalyticsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </Suspense>
  )
}

export default App
