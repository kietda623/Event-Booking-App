import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { favoritesApi } from '../../api/favorites'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { EventCard } from '../../components/EventCard'
import { Pagination } from '../../components/Pagination'

export function FavoritesPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const query = useQuery({
    queryKey: ['favorites', page],
    queryFn: () => favoritesApi.list({ page, size: 9 }),
  })
  const removeMutation = useMutation({
    mutationFn: favoritesApi.remove,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] })
      queryClient.invalidateQueries({ queryKey: ['event'] })
    },
  })

  if (query.isLoading) return <LoadingState label="Loading favorites..." />
  if (query.isError) return <ErrorState error={query.error} title="Could not load favorites" />

  const events = query.data?.content || []

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <p className="eyebrow">Saved</p>
          <h1>Favorites</h1>
        </div>
      </div>
      {removeMutation.isError && <ErrorState error={removeMutation.error} title="Could not update favorite" />}
      {events.length === 0 ? (
        <EmptyState title="No favorites yet" text="Save events you want to revisit later." />
      ) : (
        <>
          <div className="event-grid">
            {events.map((event) => (
              <EventCard
                key={event.id}
                event={event}
                isFavorited
                favoriteBusy={removeMutation.isPending}
                onFavoriteToggle={() => removeMutation.mutate(event.id)}
              />
            ))}
          </div>
          <Pagination page={query.data?.page || 0} totalPages={query.data?.totalPages || 0} onPageChange={setPage} />
        </>
      )}
    </section>
  )
}
