import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'
import './CommunitySwitcher.css'

interface Community {
  id: string
  embedId: string
  name: string
  slug: string
  avatarUrl?: string
  isPublic: boolean
}

export default function CommunitySwitcher() {
  const { accessToken } = useAuthStore()
  const [communities, setCommunities] = useState<Community[]>([])
  const [isOpen, setIsOpen] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchCommunities()
  }, [])

  const fetchCommunities = async () => {
    try {
      const response = await fetch('/api/communities/my', {
        headers: { Authorization: `Bearer ${accessToken}` }
      })
      if (response.ok) {
        const data = await response.json()
        setCommunities(data)
      }
    } catch (error) {
      console.error('Failed to fetch communities:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) return <div className="community-switcher-loading">...</div>

  return (
    <div className="community-switcher">
      <button 
        className="community-switcher-btn"
        onClick={() => setIsOpen(!isOpen)}
      >
        🏠 Communities
      </button>

      {isOpen && (
        <div className="community-dropdown">
          <div className="community-list">
            {communities.map(community => (
              <Link
                key={community.id}
                to={`/c/${community.slug}`}
                className="community-item"
                onClick={() => setIsOpen(false)}
              >
                <div className="community-avatar">
                  {community.avatarUrl ? (
                    <img src={community.avatarUrl} alt={community.name} />
                  ) : (
                    <span>{community.name[0].toUpperCase()}</span>
                  )}
                </div>
                <span className="community-name">{community.name}</span>
              </Link>
            ))}
          </div>
          
          <Link to="/communities/discover" className="discover-link">
            🔍 Discover Communities
          </Link>
        </div>
      )}
    </div>
  )
}
