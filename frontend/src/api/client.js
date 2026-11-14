import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 12000,
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('civicpulse_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Centralized Response interceptor to handle standard API responses, 401 and 403
api.interceptors.response.use(
  (response) => {
    // Return standard response data
    return response.data;
  },
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      // Clear token if expired/unauthorized
      const hadToken = !!localStorage.getItem('civicpulse_token');
      localStorage.removeItem('civicpulse_token');
      localStorage.removeItem('civicpulse_user');
      const currentPath = window.location.pathname;
      if (currentPath !== '/login' && hadToken) {
        window.dispatchEvent(new Event('civicpulse_auth_logout'));
      }
    }

    let message = error.response?.data?.message || error.message || 'Request failed';
    if (status === 403 && !error.response?.data?.message) {
      message = "You don't have permission to access this resource";
    }

    const enhancedError = new Error(message);
    enhancedError.status = status;
    enhancedError.response = error.response;
    return Promise.reject(enhancedError);
  }
);

export default api;
