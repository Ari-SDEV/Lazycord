import { useState, useEffect } from 'react'
import { useAuthStore } from '../stores/authStore'
import { useCommunityStore } from '../stores/communityStore'
import type { Community } from '../types'
import './CommunitySelection.css'

interface CommunitySelectionProps {
  onSelectCommunity: (community: Community) => void
}

export default function CommunitySelection({ onSelectCommunity }: CommunitySelectionProps) {
  const { accessToken, user, logout, isAdmin } = useAuthStore()
  const { setCurrentCommunity } = useCommunityStore()
  const [communities, setCommunities] = useState<Community[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newCommunityName, setNewCommunityName] = useState('')
  const [newCommunityDesc, setNewCommunityDesc] = useState('')
  const [isPublic, setIsPublic] = useState(true)
  const [error, setError] = useState('')

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

  useEffect(() => {
    fetchCommunities()
  }, [])

  const fetchCommunities = async () => {
    try {
      const response = await fetch(`${API_URL}/api/communities/my`, {
        headers: { 
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        }
      })
      if (response.ok) {
        const data = await response.json()
        setCommunities(data)
      } else {
        setError('Failed to load communities')
      }
    } catch (error) {
      console.error('Failed to fetch communities:', error)
      setError('Failed to load communities')
    } finally {
      setLoading(false)
    }
  }

  const handleCreateCommunity = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    try {
      const response = await fetch(`${API_URL}/api/communities`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          name: newCommunityName,
          description: newCommunityDesc,
          isPublic: isPublic
        })
      })

      if (response.ok) {
        const newCommunity = await response.json()
        setCommunities([...communities, newCommunity])
        setShowCreateModal(false)
        setNewCommunityName('')
        setNewCommunityDesc('')
      } else {
        const err = await response.json()
        setError(err.error || 'Failed to create community')
      }
    } catch (error) {
      console.error('Create community failed:', error)
      setError('Failed to create community')
    }
  }

  if (loading) {
    return (
      <div className="community-selection">
        <div className="loading">Loading communities...</div>
      </div>
    )
  }

  return (
    <div className="community-selection">
      <div className="community-selection-header">
        <h1>Welcome, {user?.firstName || user?.username}!</h1>
        <p>Select a community to join or create a new one</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="communities-grid">
        {communities.map(community => (
          <div 
            key={community.id} 
            className="community-card"
            onClick={() => {
              setCurrentCommunity(community)
              onSelectCommunity(community)
            }}
          >
            <div className="community-card-avatar">
              {community.name[0].toUpperCase()}
            </div>
            <div className="community-card-info">
              <h3>{community.name}</h3>
              <p>{community.description || 'No description'}</p>
              <span className={`badge ${community.isPublic ? 'public' : 'private'}`}>
                {community.isPublic ? '🌐 Public' : '🔒 Private'}
              </span>
            </div>
          </div>
        ))}

        {isAdmin() && (
          <div 
            className="community-card create-card"
            onClick={() => setShowCreateModal(true)}
          >
            <div className="community-card-avatar plus">+</div>
            <div className="community-card-info">
              <h3>Create New Community</h3>
              <p>Start your own community</p>
            </div>
          </div>
        )}
      </div>

      {communities.length === 0 && !isAdmin() && (
        <div className="no-communities">
          <p>You are not a member of any community yet.</p>
          <p>Contact an administrator to be invited to a community.</p>
        </div>
      )}

      <button className="logout-btn" onClick={logout}>
        Logout
      </button>

      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>Create New Community</h2>
            <form onSubmit={handleCreateCommunity}>
              <div className="form-group">
                <label>Name</label>
                <input
                  type="text"
                  value={newCommunityName}
                  onChange={(e) => setNewCommunityName(e.target.value)}
                  placeholder="Community name"
                  required
                  maxLength={50}
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={newCommunityDesc}
                  onChange={(e) => setNewCommunityDesc(e.target.value)}
                  placeholder="What's this community about?"
                  maxLength={500}
                  rows={3}
                />
              </div>
              <div className="form-group checkbox">
                <label>
                  <input
                    type="checkbox"
                    checked={isPublic}
                    onChange={(e) => setIsPublic(e.target.checked)}
                  />
                  Public Community
                </label>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
