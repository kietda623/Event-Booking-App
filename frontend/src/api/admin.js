import client from './client'

export const adminApi = {
  analytics: () => client.get('/admin/analytics'),
}
