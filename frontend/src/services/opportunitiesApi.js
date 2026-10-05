import apiClient from './apiClient'

export const opportunitiesApi = {
  getOpportunities: async (params = {}) => {
    const response = await apiClient.get('/api/opportunities', { params })
    return response.data
  },

  getOpportunityById: async (id) => {
    const response = await apiClient.get(`/api/opportunities/${id}`)
    return response.data
  },

  createOpportunity: async (data) => {
    const response = await apiClient.post('/api/opportunities', data)
    return response.data
  },

  updateOpportunity: async (id, data) => {
    const response = await apiClient.put(`/api/opportunities/${id}`, data)
    return response.data
  },

  deleteOpportunity: async (id) => {
    const response = await apiClient.delete(`/api/opportunities/${id}`)
    return response.data
  },
}
