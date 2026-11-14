import api from './client';

export const weatherApi = {
  getCurrentWeather: async (latitude, longitude) => {
    const params = {};
    if (latitude != null) params.latitude = latitude;
    if (longitude != null) params.longitude = longitude;
    return await api.get('/weather/current', { params });
  },

  getCurrentAirQuality: async (latitude, longitude) => {
    const params = {};
    if (latitude != null) params.latitude = latitude;
    if (longitude != null) params.longitude = longitude;
    return await api.get('/air-quality/current', { params });
  },

  getAirQuality: async (latitude, longitude) => {
    const params = {};
    if (latitude != null) params.latitude = latitude;
    if (longitude != null) params.longitude = longitude;
    return await api.get('/air-quality/current', { params });
  }
};
