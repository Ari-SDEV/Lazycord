import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './stores/authStore'
import Login from './components/Login'
import Chat from './components/Chat'
import Missions from './components/Missions'
import Shop from './components/Shop'
import './App.css'

function App() {
  const { isAuthenticated, checkAuth } = useAuthStore()

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  return (
    <div className="app">
      <Routes>
        <Route 
          path="/" 
          element={isAuthenticated ? <Chat /> : <Navigate to="/login" />} 
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
