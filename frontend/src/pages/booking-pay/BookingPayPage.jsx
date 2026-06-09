import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { CardElement, Elements, useElements, useStripe } from '@stripe/react-stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import { bookingsApi } from '../../api/bookings'
import { paymentsApi } from '../../api/payments'
import { FormError } from '../../components/FieldErrors'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { StatusBadge } from '../../components/StatusBadge'
import { formatCurrency } from '../../utils/format'

const stripeKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || ''
const stripePromise = stripeKey ? loadStripe(stripeKey) : null
const canUseMock = import.meta.env.VITE_ENV === 'development'
const methods = canUseMock ? ['STRIPE', 'MOCK'] : ['STRIPE']

export function BookingPayPage() {
  const { id } = useParams()
  const bookingsQuery = useQuery({
    queryKey: ['bookings', 'payment-summary'],
    queryFn: () => bookingsApi.mine({ page: 0, size: 100 }),
  })
  const booking = useMemo(
    () => bookingsQuery.data?.content?.find((item) => String(item.bookingId) === String(id)),
    [bookingsQuery.data, id],
  )

  if (bookingsQuery.isLoading) return <LoadingState label="Loading payment summary..." />
  if (bookingsQuery.isError) return <ErrorState error={bookingsQuery.error} title="Could not load booking" />
  if (!booking) return <EmptyState title="Booking not found" text="Open your bookings list and try again." />

  return (
    <Elements stripe={stripePromise}>
      <BookingPaymentForm booking={booking} bookingId={Number(id)} />
    </Elements>
  )
}

function BookingPaymentForm({ booking, bookingId }) {
  const navigate = useNavigate()
  const stripe = useStripe()
  const elements = useElements()
  const [method, setMethod] = useState('STRIPE')
  const [apiError, setApiError] = useState(null)
  const [stripeError, setStripeError] = useState('')
  const mutation = useMutation({
    mutationFn: async () => {
      if (method === 'MOCK') {
        return paymentsApi.pay({ bookingId, method: 'MOCK' })
      }
      if (!stripePromise) {
        throw { code: 'STRIPE_NOT_CONFIGURED', message: 'Stripe publishable key is not configured.' }
      }
      if (!stripe || !elements) {
        throw { code: 'STRIPE_NOT_READY', message: 'Stripe is still loading.' }
      }
      const intent = await paymentsApi.pay({ bookingId, method: 'STRIPE' })
      const card = elements.getElement(CardElement)
      const result = await stripe.confirmCardPayment(intent.clientSecret, {
        payment_method: { card },
      })
      if (result.error) {
        setStripeError(result.error.message || 'Payment failed.')
        throw { code: 'PAYMENT_DECLINED', message: result.error.message || 'Payment failed.' }
      }
      return result.paymentIntent
    },
    onSuccess: () => navigate('/tickets'),
    onError: setApiError,
  })

  const onSubmit = (event) => {
    event.preventDefault()
    setApiError(null)
    setStripeError('')
    mutation.mutate()
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
          {booking.tierName && <span>Tier: {booking.tierName}</span>}
          {(booking.seatNumbers || []).length > 0 && <span>Seats: {booking.seatNumbers.join(', ')}</span>}
          <strong>Total: {formatCurrency(booking.totalPrice)}</strong>
        </div>
        <label>
          Payment method
          <select value={method} onChange={(event) => setMethod(event.target.value)}>
            {methods.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>
        {method === 'STRIPE' && (
          <label>
            Card
            <div className="stripe-card-box">
              <CardElement options={{ hidePostalCode: true }} />
            </div>
            {stripeError && <span className="field-error">{stripeError}</span>}
          </label>
        )}
        <button className="button primary" type="submit" disabled={mutation.isPending || booking.status !== 'PENDING'}>
          {mutation.isPending ? 'Paying...' : 'Pay booking'}
        </button>
      </form>
    </section>
  )
}
