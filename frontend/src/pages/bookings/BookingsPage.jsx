import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { bookingsApi } from '../../api/bookings'
import { CancelBookingButton } from '../../components/CancelBookingButton'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { formatCurrency, formatDateTime } from '../../utils/format'

export function BookingsPage() {
  const [page, setPage] = useState(0)
  const query = useQuery({
    queryKey: ['bookings', page],
    queryFn: () => bookingsApi.mine({ page, size: 10 }),
  })

  if (query.isLoading) return <LoadingState label="Loading bookings..." />
  if (query.isError) return <ErrorState error={query.error} title="Could not load bookings" />

  const bookings = query.data?.content || []

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <p className="eyebrow">My activity</p>
          <h1>Bookings</h1>
        </div>
      </div>
      {bookings.length === 0 ? (
        <EmptyState title="No bookings yet" text="Book an event to see it here." />
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Event</th>
                  <th>Date</th>
                  <th>Qty</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {bookings.map((booking) => (
                  <tr key={booking.bookingId}>
                    <td>{booking.eventTitle}</td>
                    <td>{formatDateTime(booking.bookingTime)}</td>
                    <td>{booking.quantity}</td>
                    <td>{formatCurrency(booking.totalPrice)}</td>
                    <td>
                      <StatusBadge status={booking.status} />
                    </td>
                    <td className="row-actions">
                      <Link className="button small" to={`/bookings/${booking.bookingId}`}>
                        View
                      </Link>
                      {booking.status === 'PENDING' ? (
                        <Link className="button small" to={`/bookings/${booking.bookingId}/pay`}>
                          Pay
                        </Link>
                      ) : booking.status !== 'PAID' ? (
                        <span className="muted">-</span>
                      ) : null}
                      <CancelBookingButton booking={booking} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={query.data?.page || 0} totalPages={query.data?.totalPages || 0} onPageChange={setPage} />
        </>
      )}
    </section>
  )
}
