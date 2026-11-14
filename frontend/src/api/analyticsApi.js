import api from './client';

export const analyticsApi = {
  // Prompt 6 Dedicated Analytics Endpoints
  getOverview: async (params = {}) => {
    return await api.get('/analytics/overview', { params });
  },

  getIncidentTrends: async (params = {}) => {
    return await api.get('/analytics/incidents/trends', { params });
  },

  getIncidentsByCategory: async (params = {}) => {
    return await api.get('/analytics/incidents/by-category', { params });
  },

  getIncidentsBySeverity: async (params = {}) => {
    return await api.get('/analytics/incidents/by-severity', { params });
  },

  getIncidentsByStatus: async (params = {}) => {
    return await api.get('/analytics/incidents/by-status', { params });
  },

  getSlaPerformance: async (params = {}) => {
    return await api.get('/analytics/sla-performance', { params });
  },

  getInfrastructureAnalytics: async () => {
    return await api.get('/analytics/infrastructure');
  },

  getResponseTimes: async (params = {}) => {
    return await api.get('/analytics/response-times', { params });
  },

  getLocationAnalytics: async (params = {}) => {
    return await api.get('/analytics/locations', { params });
  },

  exportIncidentsCsv: async (params = {}) => {
    return await api.get('/analytics/export/csv', {
      params,
      responseType: 'blob',
    });
  },

  // Legacy Endpoints
  getIncidents: async (params = {}) => {
    return await api.get('/analytics/incidents', { params });
  },

  getSla: async (params = {}) => {
    return await api.get('/analytics/sla', { params });
  },

  getDepartments: async (params = {}) => {
    return await api.get('/analytics/departments', { params });
  },

  getSensors: async (params = {}) => {
    return await api.get('/analytics/sensors', { params });
  },
};

export default analyticsApi;
