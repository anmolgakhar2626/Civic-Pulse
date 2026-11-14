import api from './client';

export const slaApi = {
  getOverview: async () => {
    return await api.get('/sla/overview');
  },

  getActiveSlas: async (params = {}) => {
    return await api.get('/sla/active', { params });
  },

  getAtRiskSlas: async (params = {}) => {
    return await api.get('/sla/at-risk', { params });
  },

  getBreachedSlas: async (params = {}) => {
    return await api.get('/sla/breached', { params });
  },

  getSlaByIncidentId: async (incidentId) => {
    return await api.get(`/sla/incidents/${incidentId}`);
  },

  getPolicies: async () => {
    return await api.get('/sla/policies');
  },

  createPolicy: async (policyData) => {
    return await api.post('/sla/policies', policyData);
  },

  updatePolicy: async (id, policyData) => {
    return await api.put(`/sla/policies/${id}`, policyData);
  },

  getEscalations: async (limit = 50) => {
    return await api.get('/sla/escalations', { params: { limit } });
  }
};

export default slaApi;
