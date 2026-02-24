import { useState, useEffect, useRef } from 'react'
import { useNotificationStore } from '../stores/notificationStore'
import { useAuthStore } from '../stores/authStore'
import './NotificationBell.css'

interface Notification {
  id: string
  type: 'MENTION' | 'MESSAGE' | 'MISSION_COMPLETE' | 'LEVEL_UP' | 'SYSTEM'
  title: string
  message: string
  read: boolean
  createdAt: string
}

export default function NotificationBell() {
  const { user } = useAuthStore()
  const {
    notifications,
    unreadCount,
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    addNotification
  } = useNotificationStore()
  
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    fetchUnreadCount()
    fetchNotifications()
  }, [])

  // WebSocket for realtime notifications
  useEffect(() => {
    if (!user) return
    
    const ws = new WebSocket(`ws://localhost:8080/ws/notifications?token=${localStorage.getItem('token')}`)
    
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      if (data.type === 'NOTIFICATION') {
        addNotification(data.notification)
      } else if (data.type === 'COUNT') {
        useNotificationStore.getState().setUnreadCount(data.count)
      }
    }

    return () => ws.close()
  }, [user])

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleNotificationClick = (notification: Notification) => {
    if (!notification.read) {
      markAsRead(notification.id)
    }
  }

  const getIcon = (type: string) => {
    switch (type) {
      case 'MENTION': return '@'
      case 'MESSAGE': return '💬'
      case 'MISSION_COMPLETE': return '🏆'
      case 'LEVEL_UP': return '⬆️'
      default: return '🔔'
    }
  }

  const formatTime = (date: string) => {
    const now = new Date()
    const notifDate = new Date(date)
    const diff = now.getTime() - notifDate.getTime()
    
    if (diff < 60000) return 'Just now'
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`
    return notifDate.toLocaleDateString()
  }

  return (
    <div className="notification-bell" ref={dropdownRef}>
      <button 
        className="bell-button" 
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Notifications"
      >
        🔔
        {unreadCount > 0 && (
          <span className="notification-badge">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="notification-dropdown">
          <div className="notification-header">
            <h3>Notifications</h3>
            {unreadCount > 0 && (
              <button className="mark-all-btn" onClick={markAllAsRead}>
                Mark all as read
              </button>
            )}
          </div>

          <div className="notification-list">
            {notifications.length === 0 ? (
              <div className="no-notifications">No notifications</div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={`notification-item ${!notification.read ? 'unread' : ''}`}
                  onClick={() => handleNotificationClick(notification)}
                >
                  <span className="notification-icon">{getIcon(notification.type)}</span>
                  <div className="notification-content">
                    <div className="notification-title">{notification.title}</div>
                    <div className="notification-message">{notification.message}</div>
                    <div className="notification-time">{formatTime(notification.createdAt)}</div>
                  </div>
                  {!notification.read && <span className="unread-dot" />}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}
