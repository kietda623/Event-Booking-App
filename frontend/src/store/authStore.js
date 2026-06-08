import { create } from 'zustand'
import { TOKEN_KEY } from '../api/client'

const AUTH_KEY = 'eventBookingAuth'

function loadAuth() {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    return raw ? JSON.parse(raw) : { user: null, token: localStorage.getItem(TOKEN_KEY) }
  } catch {
    return { user: null, token: localStorage.getItem(TOKEN_KEY) }
  }
}

const initialAuth = loadAuth()

export const useAuthStore = create((set) => ({
  user: initialAuth.user ?? null,
  token: initialAuth.token ?? null,
  login: (user, token) => {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(AUTH_KEY, JSON.stringify({ user, token }))
    set({ user, token })
  },
  logout: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(AUTH_KEY)
    set({ user: null, token: null })
  },
}))
