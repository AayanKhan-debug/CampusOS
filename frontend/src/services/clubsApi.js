import apiClient from './apiClient'

export const clubsApi = {
  getClubs: async (params = {}) => {
    const response = await apiClient.get('/api/clubs', { params })
    return response.data
  },

  getClubById: async (id) => {
    const response = await apiClient.get(`/api/clubs/${id}`)
    return response.data
  },

  joinClub: async (clubId) => {
    const response = await apiClient.post(`/api/clubs/${clubId}/join`)
    return response.data
  },

  getClubMembers: async (clubId, params = {}) => {
    const response = await apiClient.get(`/api/clubs/${clubId}/members`, { params })
    return response.data
  },
}
