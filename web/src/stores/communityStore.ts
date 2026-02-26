import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Community } from '../types'

interface CommunityState {
  currentCommunity: Community | null
  setCurrentCommunity: (community: Community | null) => void
  clearCommunity: () => void
}

export const useCommunityStore = create<CommunityState>()(
  persist(
    (set) => ({
      currentCommunity: null,
      setCurrentCommunity: (community) => set({ currentCommunity: community }),
      clearCommunity: () => set({ currentCommunity: null }),
    }),
    {
      name: 'community-storage',
      partialize: (state) => ({ currentCommunity: state.currentCommunity }),
    }
  )
)
