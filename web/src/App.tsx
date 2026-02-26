import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './stores/authStore'
import { useCommunityStore } from './stores/communityStore'
import Login from './components/Login'
import Chat from './components/Chat'
import Missions from './components/Missions'
import Shop from './components/Shop'
import CommunitySelection from './components/CommunitySelection'
import './App.css'

function App() {
  const { isAuthenticated, checkAuth } = useAuthStore()
  const { currentCommunity, setCurrentCommunity } = useCommunityStore()

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  // If authenticated but no community selected, show community selection
  const showCommunitySelection = isAuthenticated && !currentCommunity

  return (
    <div className="app">
      <Routes>
        <Route 
          path="/" 
          element={
            !isAuthenticated ? <Navigate to="/login" /> :
            showCommunitySelection ? <CommunitySelection onSelectCommunity={setCurrentCommunity} /> :
            <Chat />
          } 
        />
        <Route 
          path="/chat" 
          element={
            !isAuthenticated ? <Navigate to="/login" /> :
            !currentCommunity ? <Navigate to="/" /> :
            <Chat />
          } 
        />
        <Route 
          path="/missions" 
          element={isAuthenticated ? <Missions /> : <Navigate to="/login" />} 
        />
        <Route 
          path="/shop" 
          element={isAuthenticated ? <Shop /> : <Navigate to="/login" />} 
        />
        <Route 
          path="/login" 
          element={!isAuthenticated ? <Login /> : <Navigate to="/" />} 
        />
      </Routes>
    </div>
  )
}

export default App
