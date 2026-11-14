import api from './client';

export const incidentsApi = {
  getIncidents: async (params = {}) => {
    return await api.get('/incidents', { params });
  },

  getIncidentById: async (id) => {
    return await api.get(`/incidents/${id}`);
  },

  createIncident: async (incidentData) => {
    return await api.post('/incidents', incidentData);
  },

  updateIncident: async (id, updateData) => {
    return await api.put(`/incidents/${id}`, updateData);
  },

  updateStatus: async (id, status, notes = '') => {
    return await api.patch(`/incidents/${id}/status`, { status, remarks: notes, notes });
  },

  deleteIncident: async (id) => {
    return await api.delete(`/incidents/${id}`);
  },

  evaluatePriority: async (id, payload = {}) => {
    return await api.post(`/incidents/${id}/evaluate-priority`, payload);
  },

  assignIncident: async (id, assignmentData) => {
    return await api.post(`/incidents/${id}/assign`, assignmentData);
  },

  autoAssign: async (id) => {
    return await api.post(`/incidents/${id}/auto-assign`);
  },

  generateResponsePlan: async (id) => {
    return await api.post(`/incidents/${id}/generate-response-plan`);
  },

  getResponseActions: async (id) => {
    return await api.get(`/incidents/${id}/actions`);
  },

  updateActionStatus: async (actionId, status, notes = '') => {
    return await api.patch(`/actions/${actionId}/status`, { status, notes });
  },

  getAssignmentHistory: async (id) => {
    return await api.get(`/incidents/${id}/assignment-history`);
  }
};

export default incidentsApi;
