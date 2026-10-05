import apiClient from './apiClient'

export const helpDeskApi = {
  createIssue: async (data) => {
    const response = await apiClient.post('/api/help-desk', data)
    return response.data
  },

  getMyIssues: async (params = {}) => {
    const response = await apiClient.get('/api/users/me/issues', { params })
    return response.data
  },

  getIssueById: async (id) => {
    const response = await apiClient.get(`/api/help-desk/${id}`)
    return response.data
  },

  updateIssue: async (id, data) => {
    const response = await apiClient.put(`/api/help-desk/${id}`, data)
    return response.data
  },

  deleteIssue: async (id) => {
    const response = await apiClient.delete(`/api/help-desk/${id}`)
    return response.data
  },

  closeMyIssue: async (id) => {
    const response = await apiClient.patch(`/api/help-desk/${id}/status`, { status: 'CLOSED' })
    return response.data
  },
}
