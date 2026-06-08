import { Link } from 'react-router-dom'
import { FavoriteButton } from './FavoriteButton'
import { formatCurrency, formatDateTime } from '../utils/format'

const FALLBACK_IMAGE =
  'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=900&q=80'

export function EventCard({ event, isFavorited = false, favoriteBusy = false, onFavoriteToggle }) {
  return (
    <article className="event-card">
      {onFavoriteToggle && (
        <FavoriteButton
          active={isFavorited}
          disabled={favoriteBusy}
          label={`${isFavorited ? 'Remove' : 'Add'} ${event.title} favorite`}
          onClick={() => onFavoriteToggle(event)}
        />
      )}
      <img src={event.imageUrl || FALLBACK_IMAGE} alt={event.title} />
      <div className="event-card-body">
        <div className="event-meta-row">
          <span>{formatDateTime(event.eventDate)}</span>
          <span>{formatCurrency(event.price)}</span>
        </div>
        <h3>{event.title}</h3>
        <p>{event.location || 'Location pending'}</p>
        <div className="event-card-footer">
          <span>{event.availableTickets ?? 0} tickets left</span>
          <Link to={`/events/${event.id}`} className="button small">
            View
          </Link>
        </div>
      </div>
    </article>
  )
}
