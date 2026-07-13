import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { bookingsApi } from '../../api/bookings'
import { eventsApi } from '../../api/events'
import { FormError } from '../../components/FieldErrors'
import { ErrorState, LoadingState } from '../../components/StateViews'
import { formatCurrency, formatDateTime } from '../../utils/format'

export function BookEventPage() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const selectedSeats = useMemo(
    () =>
      (searchParams.get('seats') || '')
        .split(',')
        .map((seat) => seat.trim())
        .filter(Boolean),
    [searchParams],
  )
  const [quantity, setQuantity] = useState(selectedSeats.length || 1)
  const [selectedTierId, setSelectedTierId] = useState(searchParams.get('tierId') || '')
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

  const event = eventQuery.data
  const tiers = useMemo(() => event?.tiers || [], [event?.tiers])
  const selectedTier = tiers.find((tier) => String(tier.id) === String(selectedTierId))
  const maxTickets = Math.max(selectedTier?.availableQuantity || event?.availableTickets || 1, 1)
  const hasSeatLock = selectedSeats.length > 0

  useEffect(() => {
    if (!selectedTierId && tiers.length === 1) {
      setSelectedTierId(String(tiers[0].id))
    }
  }, [selectedTierId, tiers])

  useEffect(() => {
    if (hasSeatLock) {
      setQuantity(selectedSeats.length)
    }
  }, [hasSeatLock, selectedSeats.length])

  if (eventQuery.isLoading) return <LoadingState label="Loading checkout..." />
  if (eventQuery.isError) return <ErrorState error={eventQuery.error} title="Could not load event" />

  const onSubmit = (submitEvent) => {
    submitEvent.preventDefault()
    setApiError(null)
    if (!selectedTier) {
      setApiError({ message: 'Choose a ticket tier before continuing.' })
      return
    }
    mutation.mutate({
      eventId: Number(id),
      tierId: Number(selectedTier.id),
      quantity: Number(quantity),
      seatNumbers: selectedSeats,
    })
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
          <strong>{formatCurrency(selectedTier?.price ?? event.price)} per ticket</strong>
          <span>{selectedTier?.availableQuantity ?? event.availableTickets ?? 0} tickets available</span>
          {selectedSeats.length > 0 && <span>Seats: {selectedSeats.join(', ')}</span>}
        </div>
        {tiers.length > 1 && (
          <div className="tier-grid selectable">
            {tiers.map((tier) => (
              <button
                type="button"
                key={tier.id}
                className={`tier-card selectable ${String(selectedTierId) === String(tier.id) ? 'active' : ''}`}
                data-testid="tier-option"
                onClick={() => setSelectedTierId(String(tier.id))}
                disabled={hasSeatLock && String(selectedTierId) !== String(tier.id)}
              >
                <span>{tier.name}</span>
                <strong>{formatCurrency(tier.price)}</strong>
                <small>{tier.availableQuantity ?? 0} left</small>
              </button>
            ))}
          </div>
        )}
        <label>
          Quantity
          <input
            type="number"
            min="1"
            max={maxTickets}
            value={quantity}
            onChange={(inputEvent) => setQuantity(inputEvent.target.value)}
            disabled={hasSeatLock}
            required
          />
        </label>
        <Link className="button ghost" to={`/events/${event.id}/seats`}>
          Choose seats
        </Link>
        <button className="button primary" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating booking...' : 'Continue to payment'}
        </button>
      </form>
    </section>
  )
}
