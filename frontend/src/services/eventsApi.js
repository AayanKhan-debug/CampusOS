import apiClient from './apiClient'

export const eventsApi = {
  getEvents: async (params = {}) => {
    const response = await apiClient.get('/api/events', { params })
    return response.data
  },

  getEventById: async (id) => {
    const response = await apiClient.get(`/api/events/${id}`)
    return response.data
  },

  registerForEvent: async (id) => {
    const response = await apiClient.post(`/api/events/${id}/register`)
    return response.data
  },

  cancelRegistration: async (id) => {
    const response = await apiClient.delete(`/api/events/${id}/register`)
    return response.data
  },

  getMyEvents: async (params = {}) => {
    const response = await apiClient.get('/api/users/me/events', { params })
    return response.data
  },
}
