import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '../types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: User | null
  isAuthenticated: boolean
  login: (username: string, password: string) => Promise<boolean>
  register: (username: string, email: string, password: string, firstName: string, lastName: string) => Promise<boolean>
  logout: () => void
  checkAuth: () => void
}

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,

      login: async (username: string, password: string) => {
        try {
          const response = await fetch(`${API_URL}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
          })

          if (!response.ok) return false

          const data = await response.json()
          set({
            accessToken: data.access_token,
            refreshToken: data.refresh_token,
            user: data.user,
            isAuthenticated: true,
          })
          return true
        } catch (error) {
          console.error('Login failed:', error)
          return false
        }
      },

      register: async (username: string, email: string, password: string, firstName: string, lastName: string) => {
        try {
          const response = await fetch(`${API_URL}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password, firstName, lastName }),
          })

          if (!response.ok) return false

          const data = await response.json()
          set({
            accessToken: data.access_token,
            refreshToken: data.refresh_token,
            user: data.user,
            isAuthenticated: true,
          })
          return true
        } catch (error) {
          console.error('Register failed:', error)
          return false
        }
      },

      logout: () => {
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          isAuthenticated: false,
        })
      },

      checkAuth: () => {
        const { accessToken } = get()
        if (!accessToken) {
          set({ isAuthenticated: false })
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)