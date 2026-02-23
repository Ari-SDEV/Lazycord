import { useEffect, useState } from 'react'
import { useAuthStore } from '../stores/authStore'
import './Missions.css'

interface Mission {
  id: string
  title: string
  description: string
  type: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ONE_TIME' | 'ACHIEVEMENT'
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT'
  xpReward: number
  pointsReward: number
  requiredCount: number
  currentCount: number
  completed: boolean
  rewarded: boolean
  endDate?: string
}

export default function Missions() {
  const { user } = useAuthStore()
  const [missions, setMissions] = useState<Mission[]>([])
  const [activeTab, setActiveTab] = useState<'available' | 'progress' | 'completed'>('available')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchMissions()
  }, [])

  const fetchMissions = async () => {
    try {
      const response = await fetch('/api/missions/my', {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
      })
      if (response.ok) {
        const data = await response.json()
        setMissions(data)
      }
    } catch (error) {
      console.error('Failed to fetch missions:', error)
    } finally {
      setLoading(false)
    }
  }

  const claimReward = async (missionId: string) => {
    try {
      const response = await fetch(`/api/missions/${missionId}/claim`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
      })
      if (response.ok) {
        fetchMissions()
      }
    } catch (error) {
      console.error('Failed to claim reward:', error)
    }
  }

  const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
      case 'EASY': return '#3ba55d'
      case 'MEDIUM': return '#faa61a'
      case 'HARD': return '#ed4245'
      case 'EXPERT': return '#9b59b6'
      default: return '#72767d'
    }
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'DAILY': return '📅'
      case 'WEEKLY': return '📆'
      case 'MONTHLY': return '📅'
      case 'ACHIEVEMENT': return '🏆'
      default: return '📋'
    }
  }

  const filteredMissions = missions.filter((mission) => {
    if (activeTab === 'available') return !mission.completed
    if (activeTab === 'progress') return !mission.completed && mission.currentCount > 0
    if (activeTab === 'completed') return mission.completed
    return true
  })

  const availableRewards = missions.filter(m => m.completed && !m.rewarded).length

  if (loading) return <div className="missions-loading">Loading missions...</div>

  return (
    <div className="missions-container">
      <div className="missions-header">
        <h2>Missions</h2>
        {availableRewards > 0 && (
          <div className="rewards-badge">
            🎁 {availableRewards} reward{availableRewards !== 1 ? 's' : ''} available!
          </div>
        )}
      </div>

      <div className="missions-tabs">
        <button
          className={activeTab === 'available' ? 'active' : ''}
          onClick={() => setActiveTab('available')}
        >
          Available
        </button>
        <button
          className={activeTab === 'progress' ? 'active' : ''}
          onClick={() => setActiveTab('progress')}
        >
          In Progress
        </button>
        <button
          className={activeTab === 'completed' ? 'active' : ''}
          onClick={() => setActiveTab('completed')}
        >
          Completed
        </button>
      </div>

      <div className="missions-list">
        {filteredMissions.length === 0 ? (
          <div className="no-missions">No missions found</div>
        ) : (
          filteredMissions.map((mission) => (
            <div
              key={mission.id}
              className={`mission-card ${mission.completed ? 'completed' : ''}`}
            >
              <div className="mission-header">
                <span className="mission-type">{getTypeIcon(mission.type)}</span>
                <h3>{mission.title}</h3>
                <span
                  className="mission-difficulty"
                  style={{ color: getDifficultyColor(mission.difficulty) }}
                >
                  {mission.difficulty}
                </span>
              </div>

              <p className="mission-description">{mission.description}</p>

              <div className="mission-progress">
                <div className="progress-bar">
                  <div
                    className="progress-fill"
                    style={{
                      width: `${Math.min((mission.currentCount / mission.requiredCount) * 100, 100)}%`,
                    }}
                  />
                </div>
                <span>
                  {mission.currentCount} / {mission.requiredCount}
                </span>
              </div>

              <div className="mission-rewards">
                <span>✨ {mission.xpReward} XP</span>
                <span>💰 {mission.pointsReward} Points</span>
              </div>

              {mission.completed && !mission.rewarded && (
                <button
                  className="claim-btn"
                  onClick={() => claimReward(mission.id)}
                >
                  Claim Reward 🎁
                </button>
              )}

              {mission.completed && mission.rewarded && (
                <span className="claimed-badge">✓ Claimed</span>
              )}

              {mission.endDate && (
                <div className="mission-timer">
                  Ends: {new Date(mission.endDate).toLocaleDateString()}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  )
}
