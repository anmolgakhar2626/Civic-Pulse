import api from './client';

export const authApi = {
  login: async (username, password) => {
    return await api.post('/auth/login', { username, password });
  },

  sendLoginOtp: async (mobileNumber) => {
    return await api.post('/auth/login-otp/send', { mobileNumber });
  },

  verifyLoginOtp: async (mobileNumber, otp) => {
    return await api.post('/auth/login-otp/verify', { mobileNumber, otp });
  },

  register: async (userData) => {
    return await api.post('/auth/register', userData);
  },

  sendOtp: async (mobileNumber, purpose = 'REGISTRATION') => {
    return await api.post('/auth/otp/send', { mobileNumber, purpose });
  },

  verifyOtp: async (mobileNumber, otp, purpose = 'REGISTRATION') => {
    return await api.post('/auth/otp/verify', { mobileNumber, otp, purpose });
  },

  resendOtp: async (mobileNumber, purpose = 'REGISTRATION') => {
    return await api.post('/auth/otp/resend', { mobileNumber, purpose });
  },

  logout: async () => {
    try {
      return await api.post('/auth/logout');
    } catch {
      // Best-effort logout notification
      return { message: 'Logged out' };
    }
  },

  getMe: async () => {
    return await api.get('/auth/me');
  },

  updateProfile: async (profileData) => {
    return await api.put('/auth/profile', profileData);
  },

  changePassword: async (passwordData) => {
    return await api.put('/auth/change-password', passwordData);
  }
};
