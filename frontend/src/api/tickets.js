import client from './client'

export const ticketsApi = {
  mine: (params = {}) => client.get('/tickets', { params }),
  checkIn: (payload) => client.post('/tickets/checkin', payload),
}
