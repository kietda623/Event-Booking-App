import client from './client'

export const paymentsApi = {
  get: (id) => client.get(`/payments/${id}`),
  pay: (payload) => client.post('/payments', payload),
}
