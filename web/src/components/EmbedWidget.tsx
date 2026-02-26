import { useEffect, useState } from 'react'
import './EmbedWidget.css'

interface EmbedWidgetProps {
  embedId: string
  theme?: 'dark' | 'light'
}

interface CommunityData {
  embedId: string
  name: string
  description: string
  avatarUrl?: string
  memberCount: number
  joinUrl: string
}

export default function EmbedWidget({ embedId, theme = 'dark' }: EmbedWidgetProps) {
  const [community, setCommunity] = useState<CommunityData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchCommunityData()
  }, [embedId])

  const fetchCommunityData = async () => {
    try {
      // Domain wird automatisch vom Browser gesendet (Origin header)
      const response = await fetch(`/api/embed/${embedId}`)
      
      if (!response.ok) {
        throw new Error('Failed to load community')
      }
      
      const data = await response.json()
      setCommunity(data)
    } catch (err) {
      setError('Community not found or access denied')
    } finally {
      setLoading(false)
    }
  }

  const handleJoin = () => {
    if (!community) return

    // Try to open Tauri client
    const tauriUrl = `lazycord://join/${community.embedId}`
    
    // Check if Tauri is available
    const iframe = document.createElement('iframe')
    iframe.style.display = 'none'
    document.body.appendChild(iframe)
    
    try {
      const contentWindow = iframe.contentWindow
      if (contentWindow) {
        contentWindow.location.href = tauriUrl
      }
    } catch {
      // Tauri not available, redirect to web
      window.open(community.joinUrl, '_blank')
    }
    
    setTimeout(() => {
      document.body.removeChild(iframe)
    }, 100)
  }

  if (loading) return <div className={`embed-widget ${theme}`}>Loading...</div>
  if (error) return <div className={`embed-widget ${theme}`}>{error}</div>
  if (!community) return null

  return (
    <div className={`embed-widget ${theme}`}>
      <div className="widget-header">
        <div className="widget-avatar">
          {community.avatarUrl ? (
            <img src={community.avatarUrl} alt={community.name} />
          ) : (
            <span>{community.name[0].toUpperCase()}</span>
          )}
        </div>
        <div className="widget-info">
          <h3>{community.name}</h3>
          <span className="member-count">{community.memberCount} members</span>
        </div>
      </div>
      
      <p className="widget-description">{community.description}</p>
      
      <button className="widget-join-btn" onClick={handleJoin}>
        Join Community
      </button>
    </div>
  )
}
