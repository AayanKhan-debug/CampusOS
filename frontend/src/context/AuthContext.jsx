import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { authApi } from '../services/authApi'
import { toast } from 'sonner'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('campus_os_token'))
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('campus_os_user')
    try {
      return savedUser ? JSON.parse(savedUser) : null
    } catch {
      return null
    }
  })
  const [isLoading, setIsLoading] = useState(true)

  // Fetch fresh profile on startup if token exists
  useEffect(() => {
    let isMounted = true

    async function loadUser() {
      if (!token) {
        setIsLoading(false)
        return
      }

      try {
        const freshUser = await authApi.getProfile()
        if (isMounted) {
          setUser(freshUser)
          localStorage.setItem('campus_os_user', JSON.stringify(freshUser))
        }
      } catch (err) {
        // If profile fetch fails with 401, token is invalid
        if (err.status === 401 && isMounted) {
          setToken(null)
          setUser(null)
          localStorage.removeItem('campus_os_token')
          localStorage.removeItem('campus_os_user')
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    loadUser()

    // Listen for global 401 events
    const handleUnauthorized = () => {
      setToken(null)
      setUser(null)
      toast.error('Session expired. Please sign in again.')
    }

    window.addEventListener('campusos:unauthorized', handleUnauthorized)
    return () => {
      isMounted = false
      window.removeEventListener('campusos:unauthorized', handleUnauthorized)
    }
  }, [token])

  const login = useCallback(async (credentials) => {
    const res = await authApi.login(credentials)
    const authToken = res.token
    const authUser = res.user

    localStorage.setItem('campus_os_token', authToken)
    localStorage.setItem('campus_os_user', JSON.stringify(authUser))

    setToken(authToken)
    setUser(authUser)
    toast.success(`Welcome back, ${authUser.name}!`)
    return res
  }, [])

  const register = useCallback(async (userData) => {
    const res = await authApi.register(userData)
    const authToken = res.token
    const authUser = res.user

    localStorage.setItem('campus_os_token', authToken)
    localStorage.setItem('campus_os_user', JSON.stringify(authUser))

    setToken(authToken)
    setUser(authUser)
    toast.success('Account created successfully! Welcome to CampusOS.')
    return res
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('campus_os_token')
    localStorage.removeItem('campus_os_user')
    setToken(null)
    setUser(null)
    toast.info('Signed out successfully')
  }, [])

  const updateProfile = useCallback(async (profileData) => {
    const updatedUser = await authApi.updateProfile(profileData)
    setUser(updatedUser)
    localStorage.setItem('campus_os_user', JSON.stringify(updatedUser))
    toast.success('Profile updated successfully')
    return updatedUser
  }, [])

  const value = {
    user,
    token,
    isLoading,
    isAuthenticated: !!token && !!user,
    role: user?.role || 'STUDENT',
    login,
    register,
    logout,
    updateProfile,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
