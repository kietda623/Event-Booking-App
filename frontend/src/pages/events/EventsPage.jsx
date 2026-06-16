import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '../../api/events'
import { EmptyState, ErrorState } from '../../components/StateViews'
import { EventCard } from '../../components/EventCard'
import { Pagination } from '../../components/Pagination'
import { EventGridSkeleton } from '../../components/Skeletons'
import { buildEventTypeParams, eventFilters, getEventType } from './eventFilters'
import { useBrowserGeolocation } from './useBrowserGeolocation'
import { useFavoriteEvents } from './useFavoriteEvents'

export function EventsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const { coords, locationStatus } = useBrowserGeolocation()
  const { user, favoriteIds, favoriteMutation } = useFavoriteEvents()
  const typeParam = searchParams.get('type') || ''
  const type = getEventType(typeParam)

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
  const filteredEvents = useMemo(() => {
    const events = query.data?.content || []
    const term = search.trim().toLowerCase()
    if (!term) return events
    return events.filter((event) => event.title?.toLowerCase().includes(term))
  }, [query.data?.content, search])

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
        {eventFilters.map((filter) => (
          <button
            key={filter.value || 'all'}
            type="button"
            className={`segmented-button ${type === filter.value ? 'active' : ''}`}
            onClick={() => {
              setPage(0)
              setSearchParams(buildEventTypeParams(searchParams, filter.value))
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
