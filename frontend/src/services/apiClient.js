import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to attach JWT
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('campus_os_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor to handle 401 & format errors cleanly
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const currentPath = window.location.pathname
      if (currentPath !== '/login' && currentPath !== '/register') {
        localStorage.removeItem('campus_os_token')
        localStorage.removeItem('campus_os_user')
        window.dispatchEvent(new CustomEvent('campusos:unauthorized'))
      }
    }

    // Extract user-friendly error message
    let message = 'An unexpected error occurred. Please try again.'
    if (error.response) {
      const data = error.response.data
      if (data?.message) {
        message = data.message
      } else if (data?.errors && typeof data.errors === 'object') {
        const errorList = Object.values(data.errors)
        message = errorList.length > 0 ? errorList[0] : 'Validation failed'
      } else if (data?.error) {
        message = data.error
      }
    } else if (error.request) {
      message = 'Unable to connect to CampusOS. Please check your connection and try again.'
    }

    const enhancedError = new Error(message)
    enhancedError.status = error.response?.status
    enhancedError.data = error.response?.data
    enhancedError.originalError = error

    return Promise.reject(enhancedError)
  }
)

export default apiClient
