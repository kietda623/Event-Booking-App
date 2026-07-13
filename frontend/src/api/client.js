import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
})

client.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload?.success === false) {
      throw payload
    }
    return payload?.data ?? payload
  },
  async (error) => {
    const status = error.response?.status
    const originalRequest = error.config || {}
    if (status >= 400) {
      console.error('API error', {
        endpoint: originalRequest.url,
        status,
      })
    }

    if (
      status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh') &&
      !originalRequest.url?.includes('/auth/logout')
    ) {
      originalRequest._retry = true
      try {
        await client.post('/auth/refresh')
        return client(originalRequest)
      } catch (refreshError) {
        localStorage.removeItem('eventBookingAuth')
        if (window.location.pathname !== '/login') {
          window.location.assign('/login')
        }
        return Promise.reject(refreshError.response?.data || refreshError)
      }
    }

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
