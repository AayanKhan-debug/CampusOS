import apiClient from './apiClient'

export const resourcesApi = {
  getDepartments: async (params = {}) => {
    const response = await apiClient.get('/api/departments', { params })
    return response.data
  },

  getCourses: async (params = {}) => {
    const response = await apiClient.get('/api/courses', { params })
    return response.data
  },

  getSubjects: async (params = {}) => {
    const response = await apiClient.get('/api/subjects', { params })
    return response.data
  },

  getResources: async (params = {}) => {
    const response = await apiClient.get('/api/resources', { params })
    return response.data
  },

  getResourceById: async (id) => {
    const response = await apiClient.get(`/api/resources/${id}`)
    return response.data
  },
}
