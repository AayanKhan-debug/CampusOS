import apiClient from './apiClient'

export const announcementsApi = {
  getAnnouncements: async (params = {}) => {
    const response = await apiClient.get('/api/announcements', { params })
    return response.data
  },

  getAnnouncementById: async (id) => {
    const response = await apiClient.get(`/api/announcements/${id}`)
    return response.data
  },

  markAsRead: async (id) => {
    const response = await apiClient.post(`/api/announcements/${id}/read`)
    return response.data
  },

  getUnreadAnnouncements: async (params = {}) => {
    const response = await apiClient.get('/api/users/me/announcements/unread', { params })
    return response.data
  },
}
