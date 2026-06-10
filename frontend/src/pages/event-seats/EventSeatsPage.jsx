import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { eventsApi } from '../../api/events'
import { FormError } from '../../components/FieldErrors'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { formatCurrency } from '../../utils/format'

export function EventSeatsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [selectedTierId, setSelectedTierId] = useState('')
  const [selectedSeats, setSelectedSeats] = useState([])
  const [holdExpiresAt, setHoldExpiresAt] = useState(null)
  const [remainingSeconds, setRemainingSeconds] = useState(600)
  const [apiError, setApiError] = useState(null)

  const eventQuery = useQuery({
    queryKey: ['event', id],
    queryFn: () => eventsApi.get(id),
  })
  const seatsQuery = useQuery({
    queryKey: ['event', id, 'seats'],
    queryFn: () => eventsApi.seats(id),
  })
  const holdMutation = useMutation({
    mutationFn: () => eventsApi.holdSeats(id, { seatNumbers: selectedSeats }),
    onSuccess: () => {
      navigate(`/events/${id}/book?tierId=${selectedTierId}&seats=${selectedSeats.join(',')}`)
    },
    onError: setApiError,
  })

  const event = eventQuery.data
  const tiers = event?.tiers || []
  const selectedTier = tiers.find((tier) => String(tier.id) === String(selectedTierId))
  const seats = seatsQuery.data || []
  const selectedSeatSet = useMemo(() => new Set(selectedSeats), [selectedSeats])
  const selectableLimit = Math.max(selectedTier?.availableQuantity || 0, 0)

  useEffect(() => {
    if (!selectedTierId && tiers.length > 0) {
      setSelectedTierId(String(tiers[0].id))
    }
  }, [selectedTierId, tiers])

  useEffect(() => {
    setSelectedSeats([])
    setHoldExpiresAt(null)
  }, [selectedTierId])

  useEffect(() => {
    if (selectedSeats.length === 0) {
      setHoldExpiresAt(null)
      setRemainingSeconds(600)
      return
    }
    if (!holdExpiresAt) {
      setHoldExpiresAt(Date.now() + 600000)
    }
  }, [holdExpiresAt, selectedSeats.length])

  useEffect(() => {
    if (!holdExpiresAt) return undefined
    const interval = window.setInterval(() => {
      setRemainingSeconds(Math.max(0, Math.ceil((holdExpiresAt - Date.now()) / 1000)))
    }, 1000)
    return () => window.clearInterval(interval)
  }, [holdExpiresAt])

  if (eventQuery.isLoading || seatsQuery.isLoading) return <LoadingState label="Loading seats..." />
  if (eventQuery.isError) return <ErrorState error={eventQuery.error} title="Could not load event" />
  if (seatsQuery.isError) return <ErrorState error={seatsQuery.error} title="Could not load seats" />
  if (!event) return <EmptyState title="Event not found" />

  const toggleSeat = (seat) => {
    if (seat.status !== 'AVAILABLE' || String(seat.tierId) !== String(selectedTierId)) return
    setApiError(null)
    setSelectedSeats((current) => {
      if (current.includes(seat.seatNumber)) {
        return current.filter((item) => item !== seat.seatNumber)
      }
      if (current.length >= selectableLimit) {
        return current
      }
      return [...current, seat.seatNumber]
    })
  }

  const onConfirm = () => {
    setApiError(null)
    holdMutation.mutate()
  }

  const minutes = String(Math.floor(remainingSeconds / 60)).padStart(2, '0')
  const seconds = String(remainingSeconds % 60).padStart(2, '0')

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <p className="eyebrow">Seat map</p>
          <h1>{event.title}</h1>
        </div>
        <Link className="button ghost" to={`/events/${id}`}>
          Back
        </Link>
      </div>
      <FormError error={apiError} />
      <div className="tier-grid selectable">
        {tiers.map((tier) => (
          <button
            type="button"
            key={tier.id}
            className={`tier-card selectable ${String(selectedTierId) === String(tier.id) ? 'active' : ''}`}
            onClick={() => setSelectedTierId(String(tier.id))}
          >
            <span>{tier.name}</span>
            <strong>{formatCurrency(tier.price)}</strong>
            <small>{tier.availableQuantity ?? 0} left</small>
          </button>
        ))}
      </div>
      <div className="seat-toolbar">
        <span>{selectedSeats.length} selected</span>
        <span>{selectedSeats.length > 0 ? `${minutes}:${seconds}` : '10:00'}</span>
      </div>
      <div className="seat-map" aria-label="Seat map">
        {seats.map((seat) => {
          const selected = selectedSeatSet.has(seat.seatNumber)
          const tierMismatch = selectedTierId && String(seat.tierId) !== String(selectedTierId)
          const statusClass = selected ? 'selected' : seat.status?.toLowerCase()
          return (
            <button
              type="button"
              key={seat.id}
              className={`seat-cell ${statusClass} ${tierMismatch ? 'muted-seat' : ''}`}
              onClick={() => toggleSeat(seat)}
              disabled={seat.status !== 'AVAILABLE' || tierMismatch}
              title={`${seat.seatNumber} ${seat.status}`}
            >
              {seat.seatNumber}
            </button>
          )
        })}
      </div>
      <div className="seat-legend">
        <span><i className="legend-swatch available" />Available</span>
        <span><i className="legend-swatch held" />Held</span>
        <span><i className="legend-swatch booked" />Booked</span>
        <span><i className="legend-swatch selected" />Selected</span>
      </div>
      <div className="row-actions">
        <button className="button primary" type="button" onClick={onConfirm} disabled={selectedSeats.length === 0 || holdMutation.isPending}>
          {holdMutation.isPending ? 'Holding seats...' : 'Confirm Selection'}
        </button>
      </div>
    </section>
  )
}
