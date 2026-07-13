import { useMemo } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { favoritesApi } from '../../api/favorites'
import { useAuthStore } from '../../store/authStore'

export function useFavoriteEvents() {
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
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
  const favoriteIds = useMemo(
    () => new Set((favoritesQuery.data?.content || []).map((event) => event.id)),
    [favoritesQuery.data],
  )

  return { user, favoriteIds, favoriteMutation }
}
