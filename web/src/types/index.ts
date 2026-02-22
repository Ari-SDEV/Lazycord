export interface User {
  id: string
  username: string
  email: string
  avatarUrl?: string
  status?: 'online' | 'offline' | 'away' | 'dnd'
}

export interface Channel {
  id: string
  name: string
  description?: string
  type: 'PUBLIC' | 'PRIVATE' | 'DIRECT'
  createdById: string
  createdByUsername: string
  memberCount: number
  createdAt: string
}

export interface ChatMessage {
  id: string
  content: string
  type: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'
  senderId: string
  senderUsername: string
  senderAvatarUrl?: string
  channelId: string
  attachmentUrl?: string
  edited: boolean
  createdAt: string
}

export interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: User | null
  isAuthenticated: boolean
}
