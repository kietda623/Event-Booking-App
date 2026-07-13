import client from './client'

export const bookingsApi = {
  mine: (params = {}) => client.get('/bookings/my', { params }),
  create: (payload) => client.post('/bookings', payload),
  cancel: (id) => client.put(`/bookings/${id}/cancel`),
}
