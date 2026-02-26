import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User } from '../types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: User | null
  roles: string[]
  isAuthenticated: boolean
  login: (username: string, password: string) => Promise<boolean>
  register: (username: string, email: string, password: string, firstName: string, lastName: string) => Promise<boolean>
  logout: () => void
  checkAuth: () => void
  isAdmin: () => boolean
}

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      roles: [],
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
            roles: data.roles || [],
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
            roles: data.roles || [],
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
          roles: [],
          isAuthenticated: false,
        })
      },

      checkAuth: () => {
        const { accessToken } = get()
        if (!accessToken) {
          set({ isAuthenticated: false })
        }
      },

      isAdmin: () => {
        const { roles } = get()
        return roles.includes('admin') || roles.includes('Admin')
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        roles: state.roles,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)