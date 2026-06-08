import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/events'
import { favoritesApi } from '../../api/favorites'
import { FavoriteButton } from '../../components/FavoriteButton'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { useAuthStore } from '../../store/authStore'
import { formatCurrency, formatDateTime } from '../../utils/format'

const FALLBACK_IMAGE =
  'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1400&q=80'

export function EventDetailPage() {
  const { id } = useParams()
  const queryClient = useQueryClient()
  const token = useAuthStore((state) => state.token)
  const query = useQuery({
    queryKey: ['event', id],
    queryFn: () => eventsApi.get(id),
  })
  const favoritesQuery = useQuery({
    queryKey: ['favorites', 'ids'],
    queryFn: () => favoritesApi.list({ page: 0, size: 100 }),
    enabled: Boolean(token),
  })
  const favoriteMutation = useMutation({
    mutationFn: ({ eventId, favorited }) => (favorited ? favoritesApi.remove(eventId) : favoritesApi.add(eventId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] })
      queryClient.invalidateQueries({ queryKey: ['event', id] })
    },
  })

  if (query.isLoading) return <LoadingState label="Loading event..." />
  if (query.isError) return <ErrorState error={query.error} title="Could not load event" />
  if (!query.data) return <EmptyState title="Event not found" />

  const event = query.data
  const isFavorited = (favoritesQuery.data?.content || []).some((item) => String(item.id) === String(event.id))

  return (
    <section className="detail-layout">
      <img className="detail-image" src={event.imageUrl || FALLBACK_IMAGE} alt={event.title} />
      <div className="detail-panel">
        <p className="eyebrow">{formatDateTime(event.eventDate)}</p>
        <div className="title-row">
          <h1>{event.title}</h1>
          {token && (
            <FavoriteButton
              active={isFavorited}
              disabled={favoriteMutation.isPending}
              label={`${isFavorited ? 'Remove' : 'Add'} ${event.title} favorite`}
              onClick={() => favoriteMutation.mutate({ eventId: event.id, favorited: isFavorited })}
            />
          )}
        </div>
        <p>{event.description || 'No description provided.'}</p>
        <dl className="info-list">
          <div>
            <dt>Location</dt>
            <dd>{event.location || 'TBA'}</dd>
          </div>
          <div>
            <dt>Price</dt>
            <dd>{formatCurrency(event.price)}</dd>
          </div>
          <div>
            <dt>Available tickets</dt>
            <dd>{event.availableTickets ?? 0}</dd>
          </div>
        </dl>
        {token && (
          <Link className="button primary" to={`/events/${event.id}/book`}>
            Book Now
          </Link>
        )}
        {!token && (
          <Link className="button primary" to="/login">
            Login to book
          </Link>
        )}
      </div>
    </section>
  )
}
