import api from './client';

export const departmentsApi = {
  getDepartments: async (params = {}) => {
    return await api.get('/departments', { params });
  },

  getDepartmentById: async (id) => {
    return await api.get(`/departments/${id}`);
  }
};
