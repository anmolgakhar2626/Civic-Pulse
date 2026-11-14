import React, { useState } from 'react';
import { complaintsApi } from '../api/complaintsApi';
import { useAuth } from '../context/AuthContext';
import { FiSend, FiFileText, FiCheckCircle, FiAlertCircle, FiX } from 'react-icons/fi';

export default function CitizenComplaintModal({ onClose, onCreated }) {
  const { user } = useAuth();

  const [formData, setFormData] = useState({
    title: '',
    category: 'ROADS_AND_TRAFFIC',
    description: '',
    address: 'Sector 22 Market, Main Road',
    latitude: 30.7300,
    longitude: 76.7720,
    evidenceUrl: ''
  });

  const [loading, setLoading] = useState(false);
  const [submittedComplaint, setSubmittedComplaint] = useState(null);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const payload = {
        title: formData.title.trim() || formData.category.replace(/_/g, ' ') + ' Report',
        category: formData.category,
        description: formData.description.trim(),
        address: formData.address.trim(),
        latitude: parseFloat(formData.latitude),
        longitude: parseFloat(formData.longitude),
        evidenceUrl: formData.evidenceUrl ? formData.evidenceUrl.trim() : undefined,
      };

      const res = await complaintsApi.createComplaint(payload);
      setSubmittedComplaint(res.data || res);
      if (onCreated) onCreated();
    } catch (err) {
      const msg = err.response?.data?.message || err.message;
      setError(msg || 'Failed to submit complaint.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.6)',
        backdropFilter: 'blur(3px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '1rem',
      }}
    >
      <div
        className="panel"
        style={{
          backgroundColor: '#fff',
          borderRadius: '10px',
          maxWidth: '560px',
          width: '100%',
          boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)',
          overflow: 'hidden',
        }}
      >
        <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem 1.25rem', borderBottom: '1px solid var(--border-light)' }}>
          <div className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, fontSize: '1rem' }}>
            <FiFileText size={18} color="#0f766e" />
            <span>Citizen Civic Complaint Portal</span>
          </div>
          {onClose && (
            <button
              className="btn btn-outline btn-sm"
              onClick={onClose}
              style={{ padding: '0.25rem 0.5rem', display: 'inline-flex', alignItems: 'center' }}
            >
              <FiX size={16} />
            </button>
          )}
        </div>

        <div className="panel-body" style={{ padding: '1.25rem' }}>
          {submittedComplaint ? (
            <div style={{ textAlign: 'center', padding: '1.5rem 0' }}>
              <FiCheckCircle size={44} color="#15803d" style={{ marginBottom: '0.75rem' }} />
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                Complaint Registered Successfully
              </h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                Your complaint has been submitted and queued for Department Officer verification.
              </p>
              <div style={{
                display: 'inline-block',
                background: 'var(--bg-subtle)',
                border: '1px solid var(--border-subtle)',
                padding: '0.75rem 1.5rem',
                borderRadius: '8px',
                fontFamily: 'monospace',
                fontWeight: 700,
                fontSize: '1rem',
                color: '#0284c7',
                marginBottom: '1.25rem'
              }}>
                Tracking Number: {submittedComplaint.complaintNumber}
              </div>
              <div>
                <button className="btn btn-outline" onClick={() => setSubmittedComplaint(null)}>
                  Submit Another Complaint
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              {error && (
                <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', padding: '0.65rem 0.85rem', borderRadius: '6px', fontSize: '0.8rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <FiAlertCircle size={16} className="flex-shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div className="form-group" style={{ marginBottom: '0.85rem' }}>
                <label className="form-label" style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem' }}>
                  Subject / Complaint Title *
                </label>
                <input
                  type="text"
                  required
                  className="cc-input"
                  style={{ width: '100%' }}
                  placeholder="e.g. Open manhole hazard on sidewalk"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.85rem', marginBottom: '0.85rem' }}>
                <div className="form-group">
                  <label className="form-label" style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem' }}>
                    Civic Category *
                  </label>
                  <select
                    className="cc-select"
                    style={{ width: '100%' }}
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  >
                    <option value="ROADS_AND_TRAFFIC">Road Damage / Pothole</option>
                    <option value="SOLID_WASTE_MANAGEMENT">Illegal Garbage Dumping</option>
                    <option value="STREET_LIGHTING">Streetlight Outage</option>
                    <option value="WATER_SUPPLY">Drinking Water Leakage</option>
                    <option value="SEWAGE_AND_DRAINAGE">Open Manhole / Sewage</option>
                    <option value="POLLUTION_AND_ENVIRONMENT">Environmental Pollution</option>
                    <option value="PUBLIC_HEALTH">Public Health Hazard</option>
                    <option value="GENERAL">General Civic Issue</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label" style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem' }}>
                    Street Address / Sector *
                  </label>
                  <input
                    type="text"
                    required
                    className="cc-input"
                    style={{ width: '100%' }}
                    placeholder="Sector / Market / Crossing"
                    value={formData.address}
                    onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                <label className="form-label" style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem' }}>
                  Detailed Description *
                </label>
                <textarea
                  required
                  rows={3}
                  className="cc-input"
                  style={{ width: '100%', resize: 'vertical' }}
                  placeholder="Describe the hazard, location markers, and impact on pedestrians or vehicles..."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                {onClose && (
                  <button type="button" className="btn btn-outline" onClick={onClose}>Cancel</button>
                )}
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  <FiSend size={13} style={{ marginRight: '4px' }} />
                  {loading ? 'Submitting...' : 'Register Complaint'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
