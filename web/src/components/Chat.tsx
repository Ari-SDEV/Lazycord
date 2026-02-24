import { useEffect, useState, useRef } from 'react'
import { useAuthStore } from '../stores/authStore'
import { useChatStore } from '../stores/chatStore'
import './Chat.css'

export default function Chat() {
  const { user, logout } = useAuthStore()
  const {
    channels,
    currentChannel,
    messages,
    connected,
    connect,
    disconnect,
    sendMessage,
    loadChannels,
    setCurrentChannel,
  } = useChatStore()

  const [newMessage, setNewMessage] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    connect()
    loadChannels()

    return () => {
      disconnect()
    }
  }, [connect, disconnect, loadChannels])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault()
    if (newMessage.trim()) {
      sendMessage(newMessage)
      setNewMessage('')
    }
  }

  return (
    <div className="chat-container">
      <!-- Sidebar -->
      <div className="sidebar">
        <div className="sidebar-header">
          <h3>Channels</h3>
        </div>

        <div className="channels-list">
          {channels.map((channel) => (
            <button
              key={channel.id}
              className={`channel-btn ${currentChannel?.id === channel.id ? 'active' : ''}`}
              onClick={() => setCurrentChannel(channel)}
            >
              <span className="channel-icon">
                {channel.type === 'DIRECT' ? '@' : '#'}
              </span>
              {channel.name}
            </button>
          ))}
        </div>

        <div className="user-section">
          <div className="user-info">
            <div className="avatar">{user?.username[0].toUpperCase()}</div>
            <span className="username">{user?.username}</span>
            <span className={`status ${connected ? 'online' : 'offline'}`} />
          </div>
          <button className="logout-btn" onClick={logout}>Logout</button>
        </div>
      </div>

      <!-- Main Chat -->
      <div className="main-chat">
        {currentChannel ? (
          <>
            <div className="chat-header">
              <h2>
                {currentChannel.type === 'DIRECT' ? '@' : '#'} {currentChannel.name}
              </h2>
            </div>

            <div className="messages-container">
              {messages.map((msg) => (
                <div key={msg.id} className="message">
                  <div className="message-avatar">
                    {msg.senderUsername[0].toUpperCase()}
                  </div>
                  <div className="message-content">
                    <div className="message-header">
                      <span className="message-author">{msg.senderUsername}</span>
                      <span className="message-time">
                        {new Date(msg.createdAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <p className="message-text">{msg.content}</p>
                    
                    {/* Attachments */}
                    {msg.attachments && msg.attachments.length > 0 && (
                      <div className="message-attachments">
                        {msg.attachments.map((attachment) => (
                          <div key={attachment.id} className="attachment">
                            {attachment.mimeType.startsWith('image/') ? (
                              <img 
                                src={attachment.url} 
                                alt={attachment.originalName}
                                className="attachment-image"
                                onClick={() => window.open(attachment.url, '_blank')}
                              />
                            ) : (
                              <a 
                                href={attachment.downloadUrl}
                                className="attachment-file"
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                <span className="file-icon">
                                  {attachment.mimeType === 'application/pdf' ? '📄' : '📎'}
                                </span>
                                <span className="file-name">{attachment.originalName}</span>
                                <span className="file-size">
                                  {(attachment.size / 1024).toFixed(1)} KB
                                </span>
                              </a>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            <form className="message-input" onSubmit={handleSend}>
              <FileUpload 
                channelId={currentChannel.id} 
                onUploadComplete={(file) => {
                  // Send message with attachment
                  sendMessage(`📎 ${file.originalName}`)
                }}
              />
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                placeholder={`Message ${currentChannel.type === 'DIRECT' ? '@' : '#'}${currentChannel.name}`}
              />
              <button type="submit">Send</button>
            </form>
          </>
        ) : (
          <div className="no-channel">
            <h2>Select a channel to start chatting</h2>
          </div>
        )}
      </div>
    </div>
  )
}
