import api from './client';

export const sensorsApi = {
  getSensors: async () => {
    return await api.get('/iot/sensors');
  },

  getSensorById: async (sensorId) => {
    return await api.get(`/iot/sensors/${sensorId}`);
  },

  getSensorReadings: async (sensorId, limit = 50) => {
    return await api.get(`/iot/sensors/${sensorId}/readings`, { params: { limit } });
  },

  getSimulatorStatus: async () => {
    return await api.get('/iot/sensors');
  },

  triggerSpike: async (sensorId, value = 345) => {
    return await api.post(`/iot/simulator/trigger/pm25-spike?sensorId=${encodeURIComponent(sensorId)}&value=${value}`);
  }
};
