import api from './client';

export const mapApi = {
  getMapIncidents: async (params = {}) => {
    return await api.get('/map/incidents', { params });
  },

  getMapInfrastructure: async (params = {}) => {
    return await api.get('/map/infrastructure', { params });
  },

  getMapShelters: async (params = {}) => {
    return await api.get('/map/shelters', { params });
  },

  getMapSummary: async () => {
    return await api.get('/map/summary');
  },
};

export default mapApi;

