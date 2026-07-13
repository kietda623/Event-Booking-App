import client from './client'

export const eventsApi = {
  list: (params = {}) => client.get('/events', { params }),
  nearbyPreview: (params = {}) => client.get('/events/nearby-preview', { params }),
  get: (id) => client.get(`/events/${id}`),
  create: (payload) => client.post('/events', payload),
  update: (id, payload) => client.put(`/events/${id}`, payload),
  remove: (id) => client.delete(`/events/${id}`),
  createTier: (eventId, payload) => client.post(`/events/${eventId}/tiers`, payload),
  updateTier: (eventId, tierId, payload) => client.put(`/events/${eventId}/tiers/${tierId}`, payload),
  removeTier: (eventId, tierId) => client.delete(`/events/${eventId}/tiers/${tierId}`),
  seats: (eventId) => client.get(`/events/${eventId}/seats`),
  holdSeats: (eventId, payload) => client.post(`/events/${eventId}/seats/hold`, payload),
  releaseSeats: (eventId) => client.delete(`/events/${eventId}/seats/hold`),
}
