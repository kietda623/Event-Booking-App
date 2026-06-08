import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { bookingsApi } from '../../api/bookings'
import { paymentsApi } from '../../api/payments'
import { FormError } from '../../components/FieldErrors'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { StatusBadge } from '../../components/StatusBadge'
import { formatCurrency } from '../../utils/format'

const methods = ['MOCK_CARD', 'CREDIT_CARD', 'BANK_TRANSFER', 'E_WALLET']

export function BookingPayPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [method, setMethod] = useState('MOCK_CARD')
  const [apiError, setApiError] = useState(null)
  const bookingsQuery = useQuery({
    queryKey: ['bookings', 'payment-summary'],
    queryFn: () => bookingsApi.mine({ page: 0, size: 100 }),
  })
  const booking = useMemo(
    () => bookingsQuery.data?.content?.find((item) => String(item.bookingId) === String(id)),
    [bookingsQuery.data, id],
  )
  const mutation = useMutation({
    mutationFn: paymentsApi.pay,
    onSuccess: () => navigate('/tickets'),
    onError: setApiError,
  })

  if (bookingsQuery.isLoading) return <LoadingState label="Loading payment summary..." />
  if (bookingsQuery.isError) return <ErrorState error={bookingsQuery.error} title="Could not load booking" />
  if (!booking) return <EmptyState title="Booking not found" text="Open your bookings list and try again." />

  const onSubmit = (event) => {
    event.preventDefault()
    setApiError(null)
    mutation.mutate({ bookingId: Number(id), method })
  }

  return (
    <section className="narrow-page">
      <div className="page-header">
        <div>
          <p className="eyebrow">Payment</p>
          <h1>{booking.eventTitle}</h1>
        </div>
        <StatusBadge status={booking.status} />
      </div>
      <form className="form-card" onSubmit={onSubmit}>
        <FormError error={apiError} />
        <div className="summary-box">
          <span>Quantity: {booking.quantity}</span>
          <strong>Total: {formatCurrency(booking.totalPrice)}</strong>
        </div>
        <label>
          Payment method
          <select value={method} onChange={(event) => setMethod(event.target.value)}>
            {methods.map((item) => (
              <option key={item} value={item}>
                {item.replace('_', ' ')}
              </option>
            ))}
          </select>
        </label>
        <button className="button primary" type="submit" disabled={mutation.isPending || booking.status !== 'PENDING'}>
          {mutation.isPending ? 'Paying...' : 'Pay booking'}
        </button>
      </form>
    </section>
  )
}
