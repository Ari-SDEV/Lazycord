import { create } from 'zustand'
import { Client } from '@stomp/stompjs'
import type { Channel, ChatMessage } from '../types'
import { useAuthStore } from './authStore'

interface ChatState {
  channels: Channel[]
  currentChannel: Channel | null
  messages: ChatMessage[]
  stompClient: Client | null
  connected: boolean
  connect: () => void
  disconnect: () => void
  sendMessage: (content: string) => void
  joinChannel: (channelId: string) => void
  leaveChannel: (channelId: string) => void
  loadChannels: () => Promise<void>
  loadMessages: (channelId: string) => Promise<void>
  setCurrentChannel: (channel: Channel | null) => void
}

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/chat'

export const useChatStore = create<ChatState>()((set, get) => ({
  channels: [],
  currentChannel: null,
  messages: [],
  stompClient: null,
  connected: false,

  connect: () => {
    const { accessToken } = useAuthStore.getState()
    if (!accessToken) return

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      debug: (str) => console.log('STOMP:', str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    client.onConnect = () => {
      console.log('Connected to WebSocket')
      set({ connected: true })

      const { currentChannel } = get()
      if (currentChannel) {
        client.subscribe(`/topic/channel/${currentChannel.id}`, (message) => {
          const chatMessage: ChatMessage = JSON.parse(message.body)
          set((state) => ({
            messages: [...state.messages, chatMessage],
          }))
        })
      }
    }

    client.onDisconnect = () => {
      console.log('Disconnected from WebSocket')
      set({ connected: false })
    }

    client.activate()
    set({ stompClient: client })
  },

  disconnect: () => {
    const { stompClient } = get()
    if (stompClient) {
      stompClient.deactivate()
      set({ stompClient: null, connected: false })
    }
  },

  sendMessage: (content: string) => {
    const { stompClient, currentChannel } = get()
    if (!stompClient || !currentChannel) return

    stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        content,
        channelId: currentChannel.id,
        type: 'TEXT',
      }),
    })
  },

  joinChannel: (channelId: string) => {
    const { stompClient } = get()
    if (!stompClient) return

    stompClient.publish({
      destination: '/app/chat.join',
      body: channelId,
    })
  },

  leaveChannel: (channelId: string) => {
    const { stompClient } = get()
    if (!stompClient) return

    stompClient.publish({
      destination: '/app/chat.leave',
      body: channelId,
    })
  },

  loadChannels: async () => {
    try {
      const { accessToken } = useAuthStore.getState()
      const response = await fetch(`${API_URL}/api/channels`, {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      })

      if (response.ok) {
        const channels: Channel[] = await response.json()
        set({ channels })
      }
    } catch (error) {
      console.error('Failed to load channels:', error)
    }
  },

  loadMessages: async (channelId: string) => {
    try {
      const { accessToken } = useAuthStore.getState()
      const response = await fetch(`${API_URL}/api/messages/channel/${channelId}`, {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      })

      if (response.ok) {
        const messages: ChatMessage[] = await response.json()
        set({ messages })
      }
    } catch (error) {
      console.error('Failed to load messages:', error)
    }
  },

  setCurrentChannel: (channel: Channel | null) => {
    set({ currentChannel: channel, messages: [] })
    if (channel) {
      get().loadMessages(channel.id)
    }
  },
}))
