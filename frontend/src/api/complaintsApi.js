import api from './client';

export const complaintsApi = {
  getComplaints: async (params = {}) => {
    return await api.get('/complaints', { params });
  },

  getComplaintById: async (id) => {
    return await api.get(`/complaints/${id}`);
  },

  createComplaint: async (data) => {
    return await api.post('/complaints', data);
  },

  createComplaintMultipart: async (formData) => {
    return await api.post('/complaints', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  assignComplaint: async (id, data) => {
    return await api.patch(`/complaints/${id}/assign`, data);
  },

  getEligibleAssignees: async (deptId = null) => {
    return await api.get('/complaints/assignees', {
      params: deptId ? { departmentId: deptId } : {}
    });
  },

  updateProgress: async (id, data) => {
    return await api.patch(`/complaints/${id}/progress`, data);
  },

  updateProgressMultipart: async (id, formData) => {
    return await api.patch(`/complaints/${id}/progress`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  submitResolution: async (id, data) => {
    return await api.post(`/complaints/${id}/resolution`, data);
  },

  submitResolutionMultipart: async (id, formData) => {
    return await api.post(`/complaints/${id}/resolution`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  verifyResolution: async (id, data) => {
    return await api.patch(`/complaints/${id}/verify-resolution`, data);
  },

  changePriority: async (id, data) => {
    return await api.patch(`/complaints/${id}/priority`, data);
  },

  closeOrReopen: async (id, data) => {
    return await api.patch(`/complaints/${id}/close`, data);
  },

  getReceipt: async (id) => {
    return await api.get(`/complaints/${id}/receipt`);
  },

  downloadReceiptPdf: async (id) => {
    return await api.get(`/complaints/${id}/receipt/pdf`, {
      responseType: 'blob'
    });
  },

  getAdminStats: async () => {
    return await api.get('/complaints/admin/stats');
  },

  getDepartmentStats: async (deptId = null) => {
    return await api.get('/complaints/department/stats', {
      params: deptId ? { departmentId: deptId } : {}
    });
  },

  getAllDepartmentStats: async () => {
    return await api.get('/complaints/department/stats/all');
  },

  verifyComplaint: async (id, data) => {
    return await api.patch(`/complaints/${id}/verify`, data);
  },

  getVerificationHistory: async (id) => {
    return await api.get(`/complaints/${id}/verification-history`);
  },

  getMyReputation: async () => {
    return await api.get('/complaints/reputation/me');
  },

  getCitizenReputation: async (citizenId) => {
    return await api.get(`/complaints/reputation/${citizenId}`);
  },

  forwardToGovernmentPortal: async (id, data = {}) => {
    return await api.post(`/complaints/${id}/external-submission`, data);
  },

  getExternalTracking: async (id) => {
    return await api.get(`/complaints/${id}/external-tracking`);
  },

  syncExternalStatus: async (id) => {
    return await api.post(`/complaints/${id}/external-sync`);
  },

  getAcknowledgementDocumentUrl: (documentReference) => {
    return `/api/complaints/documents/${documentReference}`;
  }
};
