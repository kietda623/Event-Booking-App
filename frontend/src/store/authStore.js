import { create } from 'zustand'

const AUTH_KEY = 'eventBookingAuth'

function loadAuth() {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    return raw ? JSON.parse(raw) : { user: null }
  } catch {
    return { user: null }
  }
}

const initialAuth = loadAuth()

export const useAuthStore = create((set) => ({
  user: initialAuth.user ?? null,
  login: (user) => {
    localStorage.setItem(AUTH_KEY, JSON.stringify({ user }))
    set({ user })
  },
  logout: () => {
    localStorage.removeItem(AUTH_KEY)
    set({ user: null })
  },
}))
