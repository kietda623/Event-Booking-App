import client from './client'

export const authApi = {
  register: (payload) => client.post('/auth/register', payload),
  login: (payload) => client.post('/auth/login', payload),
  refresh: () => client.post('/auth/refresh'),
  logout: () => client.post('/auth/logout'),
}
