import client from './client'

export const eventsApi = {
  list: (params = {}) => client.get('/events', { params }),
  get: (id) => client.get(`/events/${id}`),
  create: (payload) => client.post('/events', payload),
  update: (id, payload) => client.put(`/events/${id}`, payload),
  remove: (id) => client.delete(`/events/${id}`),
}
