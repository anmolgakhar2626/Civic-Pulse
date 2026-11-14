import api from './client';

export const gisApi = {
  getIncidentsGeoJson: async () => {
    return await api.get('/gis/incidents');
  },

  getSensorsGeoJson: async () => {
    return await api.get('/gis/sensors');
  },

  getInfrastructureGeoJson: async () => {
    return await api.get('/gis/infrastructure');
  },

  getNearbyIncidents: async (latitude, longitude, radius = 5000) => {
    return await api.get('/gis/nearby-incidents', {
      params: { latitude, longitude, radius }
    });
  },

  getNearbyResources: async (latitude, longitude, radius = 5000) => {
    return await api.get('/gis/nearby-resources', {
      params: { latitude, longitude, radius }
    });
  }
};
