import apiClient from './apiClient'

export const lostFoundApi = {
  getListings: async (params = {}) => {
    const response = await apiClient.get('/api/lost-found', { params })
    return response.data
  },

  getListingById: async (id) => {
    const response = await apiClient.get(`/api/lost-found/${id}`)
    return response.data
  },

  createListing: async (data) => {
    const response = await apiClient.post('/api/lost-found', data)
    return response.data
  },

  updateListing: async (id, data) => {
    const response = await apiClient.put(`/api/lost-found/${id}`, data)
    return response.data
  },

  deleteListing: async (id) => {
    const response = await apiClient.delete(`/api/lost-found/${id}`)
    return response.data
  },

  updateStatus: async (id, status) => {
    const response = await apiClient.patch(`/api/lost-found/${id}/status`, { status })
    return response.data
  },
}
