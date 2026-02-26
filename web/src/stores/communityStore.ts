import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Community, CommunityRole } from '../types'

interface CommunityState {
  currentCommunity: Community | null
  userRole: CommunityRole
  setCurrentCommunity: (community: Community | null) => void
  clearCommunity: () => void
  
  // Role checks
  isCommunityOwner: () => boolean
  isCommunityAdmin: () => boolean
  isCommunityModerator: () => boolean
  hasCommunityRole: (minRole: CommunityRole) => boolean
}

export const useCommunityStore = create<CommunityState>()(
  persist(
    (set, get) => ({
      currentCommunity: null,
      userRole: 'NONE',
      
      setCurrentCommunity: (community) => {
        set({ 
          currentCommunity: community,
          userRole: community?.userRole || 'NONE'
        })
      },
      
      clearCommunity: () => set({ currentCommunity: null, userRole: 'NONE' }),
      
      isCommunityOwner: () => {
        return get().userRole === 'OWNER'
      },
      
      isCommunityAdmin: () => {
        const role = get().userRole
        return role === 'OWNER' || role === 'ADMIN'
      },
      
      isCommunityModerator: () => {
        const role = get().userRole
        return role === 'OWNER' || role === 'ADMIN' || role === 'MODERATOR'
      },
      
      hasCommunityRole: (minRole: CommunityRole) => {
        const roleLevels: Record<CommunityRole, number> = {
          'NONE': 0,
          'MEMBER': 1,
          'MODERATOR': 2,
          'ADMIN': 3,
          'OWNER': 4
        }
        
        const currentLevel = roleLevels[get().userRole] || 0
        const requiredLevel = roleLevels[minRole] || 0
        
        return currentLevel >= requiredLevel
      }
    }),
    {
      name: 'community-storage',
      partialize: (state) => ({ 
        currentCommunity: state.currentCommunity,
        userRole: state.userRole
      }),
    }
  )
)
