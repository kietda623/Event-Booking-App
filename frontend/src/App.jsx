import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/AppLayout.jsx'
import { AdminRoute } from './routes/AdminRoute.jsx'
import { GuestRoute } from './routes/GuestRoute.jsx'
import { PrivateRoute } from './routes/PrivateRoute.jsx'
import { AdminAnalyticsPage } from './pages/admin-analytics/AdminAnalyticsPage.jsx'
import { AdminCheckInPage } from './pages/admin-checkin/AdminCheckInPage.jsx'
import { AdminEventFormPage } from './pages/admin-event-form/AdminEventFormPage.jsx'
import { AdminEventsPage } from './pages/admin-events/AdminEventsPage.jsx'
import { BookingDetailPage } from './pages/booking-detail/BookingDetailPage.jsx'
import { BookingPayPage } from './pages/booking-pay/BookingPayPage.jsx'
import { BookEventPage } from './pages/book-event/BookEventPage.jsx'
import { BookingsPage } from './pages/bookings/BookingsPage.jsx'
import { EventDetailPage } from './pages/event-detail/EventDetailPage.jsx'
import { EventsPage } from './pages/events/EventsPage.jsx'
import { FavoritesPage } from './pages/favorites/FavoritesPage.jsx'
import { LoginPage } from './pages/login/LoginPage.jsx'
import { ProfilePage } from './pages/profile/ProfilePage.jsx'
import { RegisterPage } from './pages/register/RegisterPage.jsx'
import { TicketsPage } from './pages/tickets/TicketsPage.jsx'

function App() {
  return (
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
  )
}

export default App
