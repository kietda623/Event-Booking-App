import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/events'
import { favoritesApi } from '../../api/favorites'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { EventCard } from '../../components/EventCard'
import { Pagination } from '../../components/Pagination'
import { useAuthStore } from '../../store/authStore'

const filters = [
  { label: 'All', value: '' },
  { label: 'Popular', value: 'popular' },
  { label: 'Upcoming', value: 'upcoming' },
]

export function EventsPage() {
  const queryClient = useQueryClient()
  const token = useAuthStore((state) => state.token)
  const [type, setType] = useState('')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const query = useQuery({
    queryKey: ['events', type, page],
    queryFn: () => eventsApi.list({ type: type || undefined, page, size: 9 }),
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
      queryClient.invalidateQueries({ queryKey: ['event'] })
    },
  })

  const events = query.data?.content || []
  const filteredEvents = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return events
    return events.filter((event) => event.title?.toLowerCase().includes(term))
  }, [events, search])
  const favoriteIds = useMemo(
    () => new Set((favoritesQuery.data?.content || []).map((event) => event.id)),
    [favoritesQuery.data],
  )

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <p className="eyebrow">Browse</p>
          <h1>Events</h1>
        </div>
        <input
          className="search-input"
          placeholder="Search by title"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>
      <div className="toolbar">
        {filters.map((filter) => (
          <button
            key={filter.value || 'all'}
            type="button"
            className={`segmented-button ${type === filter.value ? 'active' : ''}`}
            onClick={() => {
              setType(filter.value)
              setPage(0)
            }}
          >
            {filter.label}
          </button>
        ))}
      </div>
      {query.isLoading && <LoadingState label="Loading events..." />}
      {query.isError && <ErrorState error={query.error} title="Could not load events" />}
      {!query.isLoading && !query.isError && filteredEvents.length === 0 && <EmptyState title="No events found" />}
      {!query.isLoading && !query.isError && filteredEvents.length > 0 && (
        <>
          <div className="event-grid">
            {filteredEvents.map((event) => (
              <EventCard
                key={event.id}
                event={event}
                isFavorited={favoriteIds.has(event.id)}
                favoriteBusy={favoriteMutation.isPending}
                onFavoriteToggle={
                  token
                    ? (selected) =>
                        favoriteMutation.mutate({
                          eventId: selected.id,
                          favorited: favoriteIds.has(selected.id),
                        })
                    : null
                }
              />
            ))}
          </div>
          <Pagination page={query.data?.page || 0} totalPages={query.data?.totalPages || 0} onPageChange={setPage} />
        </>
      )}
    </section>
  )
}
