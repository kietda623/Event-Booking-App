import axios from 'axios'

export const TOKEN_KEY = 'eventBookingToken'

const client = axios.create({
  baseURL: 'http://localhost:8080/api',
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload?.success === false) {
      throw payload
    }
    return payload?.data ?? payload
  },
  (error) => {
    if (error.response?.data) {
      return Promise.reject(error.response.data)
    }
    return Promise.reject({
      success: false,
      code: 'NETWORK_ERROR',
      message: error.message || 'Network error',
      errors: [],
    })
  },
)

export default client
