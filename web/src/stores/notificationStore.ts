import { create } from 'zustand'

interface Notification {
  id: string
  type: 'MENTION' | 'MESSAGE' | 'MISSION_COMPLETE' | 'LEVEL_UP' | 'SYSTEM'
  title: string
  message: string
  data?: string
  read: boolean
  createdAt: string
}

interface NotificationState {
  notifications: Notification[]
  unreadCount: number
  loading: boolean
  fetchNotifications: () => Promise<void>
  fetchUnreadCount: () => Promise<void>
  markAsRead: (id: string) => Promise<void>
  markAllAsRead: () => Promise<void>
  addNotification: (notification: Notification) => void
  setUnreadCount: (count: number) => void
}

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  unreadCount: 0,
  loading: false,

  fetchNotifications: async () => {
    set({ loading: true })
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${API_URL}/api/notifications`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (response.ok) {
        const data = await response.json()
        set({ notifications: data.content })
      }
    } catch (error) {
      console.error('Failed to fetch notifications:', error)
    } finally {
      set({ loading: false })
    }
  },

  fetchUnreadCount: async () => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${API_URL}/api/notifications/count`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (response.ok) {
        const data = await response.json()
        set({ unreadCount: data.count })
      }
    } catch (error) {
      console.error('Failed to fetch unread count:', error)
    }
  },

  markAsRead: async (id: string) => {
    try {
      const token = localStorage.getItem('token')
      await fetch(`${API_URL}/api/notifications/${id}/read`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      set((state) => ({
        notifications: state.notifications.map((n) =>
          n.id === id ? { ...n, read: true } : n
        ),
        unreadCount: Math.max(0, state.unreadCount - 1)
      }))
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  },

  markAllAsRead: async () => {
    try {
      const token = localStorage.getItem('token')
      await fetch(`${API_URL}/api/notifications/read-all`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      set((state) => ({
        notifications: state.notifications.map((n) => ({ ...n, read: true })),
        unreadCount: 0
      }))
    } catch (error) {
      console.error('Failed to mark all as read:', error)
    }
  },

  addNotification: (notification: Notification) => {
    set((state) => ({
      notifications: [notification, ...state.notifications],
      unreadCount: notification.read ? state.unreadCount : state.unreadCount + 1
    }))
  },

  setUnreadCount: (count: number) => {
    set({ unreadCount: count })
  }
}))
