import api from './client';

export const notificationsApi = {
  getNotifications: async (params = {}) => {
    return await api.get('/notifications', { params });
  },

  getNotificationById: async (id) => {
    return await api.get(`/notifications/${id}`);
  },

  getUnreadCount: async () => {
    return await api.get('/notifications/unread-count');
  },

  markAsRead: async (id) => {
    return await api.patch(`/notifications/${id}/read`);
  },

  markAsUnread: async (id) => {
    return await api.patch(`/notifications/${id}/unread`);
  },

  markAllAsRead: async () => {
    return await api.patch('/notifications/read-all');
  },

  deleteNotification: async (id) => {
    return await api.delete(`/notifications/${id}`);
  }
};

export default notificationsApi;
