import client from './client'

export const usersApi = {
  profile: () => client.get('/users/profile'),
  updateProfile: (payload) => client.put('/users/profile', payload),
  updateReminders: (payload) => client.put('/users/reminders', payload),
}
