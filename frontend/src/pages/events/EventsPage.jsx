import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/events'
import { favoritesApi } from '../../api/favorites'
import { EmptyState, ErrorState } from '../../components/StateViews'
import { EventCard } from '../../components/EventCard'
import { Pagination } from '../../components/Pagination'
import { EventGridSkeleton } from '../../components/Skeletons'
import { useAuthStore } from '../../store/authStore'

const filters = [
  { label: 'All', value: '' },
  { label: 'Popular', value: 'popular' },
  { label: 'Upcoming', value: 'upcoming' },
  { label: 'Nearby', value: 'nearby' },
]

export function EventsPage() {
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const [type, setType] = useState('')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [coords, setCoords] = useState(null)
  const [locationStatus, setLocationStatus] = useState('idle')

  useEffect(() => {
    if (!navigator.geolocation) {
      setLocationStatus('unsupported')
      return
    }
    setLocationStatus('loading')
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCoords({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        })
        setLocationStatus('granted')
      },
      () => setLocationStatus('denied'),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 },
    )
  }, [])

  const query = useQuery({
    queryKey: ['events', type, page, coords?.latitude, coords?.longitude],
    queryFn: () =>
      eventsApi.list({
        type: type || undefined,
        latitude: type === 'nearby' ? coords?.latitude : undefined,
        longitude: type === 'nearby' ? coords?.longitude : undefined,
        radius: type === 'nearby' ? 50 : undefined,
        page,
        size: 9,
      }),
    enabled: type !== 'nearby' || Boolean(coords),
    staleTime: 60000,
  })
  const nearbyPreviewQuery = useQuery({
    queryKey: ['events', 'nearby-preview', coords?.latitude, coords?.longitude],
    queryFn: () => eventsApi.nearbyPreview(coords),
    enabled: Boolean(coords),
    staleTime: 60000,
  })
  const favoritesQuery = useQuery({
    queryKey: ['favorites', 'ids'],
    queryFn: () => favoritesApi.list({ page: 0, size: 100 }),
    enabled: Boolean(user),
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
      <section className="page-stack compact-stack">
        <div className="section-header">
          <div>
            <p className="eyebrow">Nearby</p>
            <h2>Events Near You</h2>
          </div>
        </div>
        {locationStatus === 'loading' && <EventGridSkeleton count={4} />}
        {(locationStatus === 'denied' || locationStatus === 'unsupported') && (
          <div className="state-box">Enable location to see nearby events</div>
        )}
        {coords && nearbyPreviewQuery.isError && <ErrorState error={nearbyPreviewQuery.error} title="Could not load nearby events" />}
        {coords && nearbyPreviewQuery.isLoading && <EventGridSkeleton count={4} />}
        {coords && !nearbyPreviewQuery.isLoading && !nearbyPreviewQuery.isError && (nearbyPreviewQuery.data || []).length > 0 && (
          <div className="event-grid compact-grid">
            {nearbyPreviewQuery.data.map((event) => (
              <EventCard key={`nearby-${event.id}`} event={event} />
            ))}
          </div>
        )}
      </section>
      {query.isLoading && <EventGridSkeleton count={9} />}
      {query.isError && <ErrorState error={query.error} title="Could not load events" />}
      {type === 'nearby' && !coords && <EmptyState title="Location required" text="Enable location to see nearby events." />}
      {!query.isLoading && !query.isError && filteredEvents.length === 0 && type !== 'nearby' && <EmptyState title="No events found" />}
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
                  user
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
