import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { bookingsApi } from '../../api/bookings'
import { CancelBookingButton } from '../../components/CancelBookingButton'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { StatusBadge } from '../../components/StatusBadge'
import { formatCurrency, formatDateTime } from '../../utils/format'

export function BookingDetailPage() {
  const { id } = useParams()
  const [latestBooking, setLatestBooking] = useState(null)
  const query = useQuery({
    queryKey: ['bookings', 'detail', id],
    queryFn: () => bookingsApi.mine({ page: 0, size: 100 }),
  })
  const booking = useMemo(
    () => latestBooking || query.data?.content?.find((item) => String(item.bookingId) === String(id)),
    [latestBooking, query.data, id],
  )

  if (query.isLoading) return <LoadingState label="Loading booking..." />
  if (query.isError) return <ErrorState error={query.error} title="Could not load booking" />
  if (!booking) return <EmptyState title="Booking not found" text="Open your bookings list and try again." />

  return (
    <section className="narrow-page">
      <div className="page-header">
        <div>
          <p className="eyebrow">Booking detail</p>
          <h1>{booking.eventTitle}</h1>
        </div>
        <StatusBadge status={booking.status} />
      </div>
      <div className="form-card">
        {booking.status === 'CANCELLED' && booking.refundStatus && (
          <div className="toast">Refund status: {booking.refundStatus}</div>
        )}
        <dl className="info-list">
          <div>
            <dt>Booked at</dt>
            <dd>{formatDateTime(booking.bookingTime)}</dd>
          </div>
          <div>
            <dt>Quantity</dt>
            <dd>{booking.quantity}</dd>
          </div>
          <div>
            <dt>Total</dt>
            <dd>{formatCurrency(booking.totalPrice)}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>
              <StatusBadge status={booking.status} />
            </dd>
          </div>
        </dl>
        <div className="row-actions">
          <Link className="button ghost" to="/bookings">
            Back
          </Link>
          {booking.status === 'PENDING' && (
            <Link className="button primary" to={`/bookings/${booking.bookingId}/pay`}>
              Pay booking
            </Link>
          )}
          <CancelBookingButton booking={booking} onCancelled={setLatestBooking} />
        </div>
      </div>
    </section>
  )
}
