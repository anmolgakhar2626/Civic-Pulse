import api from './client';

export const infrastructureApi = {
  getInfrastructure: async (params = {}) => {
    return await api.get('/infrastructure', { params });
  },

  getInfrastructureById: async (id) => {
    return await api.get(`/infrastructure/${id}`);
  },

  createInfrastructure: async (data) => {
    return await api.post('/infrastructure', data);
  },

  updateInfrastructure: async (id, data) => {
    return await api.put(`/infrastructure/${id}`, data);
  },

  deleteInfrastructure: async (id) => {
    return await api.delete(`/infrastructure/${id}`);
  },
};

export default infrastructureApi;

