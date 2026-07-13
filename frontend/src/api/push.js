import client from './client'

export const pushApi = {
  publicKey: () => client.get('/push/vapid-public-key'),
  subscribe: (payload) => client.post('/push/subscribe', payload),
  unsubscribe: (payload) => client.delete('/push/subscribe', { data: payload }),
}
