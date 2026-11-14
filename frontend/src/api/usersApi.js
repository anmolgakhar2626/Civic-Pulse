import api from './client';

export const usersApi = {
  getUsers: async () => {
    return await api.get('/users');
  },

  getUserById: async (id) => {
    return await api.get(`/users/${id}`);
  },

  updateStatus: async (id, enabled) => {
    return await api.patch(`/users/${id}/status`, { enabled });
  },

  updateRole: async (id, role) => {
    return await api.patch(`/users/${id}/role`, { role });
  },

  getStats: async () => {
    return await api.get('/users/stats');
  },
};
