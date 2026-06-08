import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { bookingsApi } from '../../api/bookings'
import { eventsApi } from '../../api/events'
import { FormError } from '../../components/FieldErrors'
import { ErrorState, LoadingState } from '../../components/StateViews'
import { formatCurrency, formatDateTime } from '../../utils/format'

export function BookEventPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [quantity, setQuantity] = useState(1)
  const [apiError, setApiError] = useState(null)
  const eventQuery = useQuery({
    queryKey: ['event', id],
    queryFn: () => eventsApi.get(id),
  })
  const mutation = useMutation({
    mutationFn: bookingsApi.create,
    onSuccess: (booking) => navigate(`/bookings/${booking.bookingId}/pay`),
    onError: setApiError,
  })

  if (eventQuery.isLoading) return <LoadingState label="Loading checkout..." />
  if (eventQuery.isError) return <ErrorState error={eventQuery.error} title="Could not load event" />

  const event = eventQuery.data
  const maxTickets = Math.max(event?.availableTickets || 1, 1)

  const onSubmit = (submitEvent) => {
    submitEvent.preventDefault()
    setApiError(null)
    mutation.mutate({ eventId: Number(id), quantity: Number(quantity) })
  }

  return (
    <section className="narrow-page">
      <div className="page-header">
        <div>
          <p className="eyebrow">Checkout</p>
          <h1>{event.title}</h1>
        </div>
      </div>
      <form className="form-card" onSubmit={onSubmit}>
        <FormError error={apiError} />
        <div className="summary-box">
          <span>{formatDateTime(event.eventDate)}</span>
          <strong>{formatCurrency(event.price)} per ticket</strong>
          <span>{event.availableTickets ?? 0} tickets available</span>
        </div>
        <label>
          Quantity
          <input
            type="number"
            min="1"
            max={maxTickets}
            value={quantity}
            onChange={(inputEvent) => setQuantity(inputEvent.target.value)}
            required
          />
        </label>
        <button className="button primary" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating booking...' : 'Continue to payment'}
        </button>
      </form>
    </section>
  )
}
