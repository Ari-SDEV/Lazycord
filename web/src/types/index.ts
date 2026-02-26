export interface User {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  avatarUrl?: string
  status?: 'online' | 'offline' | 'away' | 'dnd'
  points: number
  level: number
  xp?: number
  rank?: string
}

export type CommunityRole = 'OWNER' | 'ADMIN' | 'MODERATOR' | 'MEMBER' | 'NONE'

export interface Community {
  id: string
  embedId: string
  name: string
  description?: string
  isPublic: boolean
  active: boolean
  owner?: User
  createdAt: string
  userRole: CommunityRole
  isMember: boolean
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
  attachments?: FileAttachment[]
  edited: boolean
  createdAt: string
}

export interface FileAttachment {
  id: string
  originalName: string
  mimeType: string
  size: number
  url: string
  downloadUrl: string
}

export interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: User | null
  isAuthenticated: boolean
}
