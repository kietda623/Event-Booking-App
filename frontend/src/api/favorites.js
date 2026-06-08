import client from './client'

export const favoritesApi = {
  list: (params = {}) => client.get('/users/favorites', { params }),
  add: (eventId) => client.post(`/events/${eventId}/favorite`),
  remove: (eventId) => client.delete(`/events/${eventId}/favorite`),
}
