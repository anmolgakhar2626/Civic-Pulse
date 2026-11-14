import React, { useState } from 'react';
import { complaintsApi } from '../api/complaintsApi';
import { FiCheckCircle, FiXCircle, FiCopy, FiHelpCircle, FiX, FiShield, FiAlertCircle } from 'react-icons/fi';

export default function ComplaintVerificationModal({ complaint, onClose, onVerified }) {
  const [status, setStatus] = useState(complaint?.complaintVerificationStatus || 'GENUINE');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');

    try {
      const res = await complaintsApi.verifyComplaint(complaint.id, {
        verificationStatus: status,
        notes: notes.trim() || undefined,
      });
      if (onVerified) {
        onVerified(res.data || res);
      }
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to submit complaint verification.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1050,
        padding: '1.25rem',
      }}
    >
      <div
        style={{
          backgroundColor: '#0f172a',
          color: '#f8fafc',
          borderRadius: '12px',
          border: '1px solid #334155',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
          width: '100%',
          maxWidth: '560px',
          overflow: 'hidden',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderBottom: '1px solid #1e293b',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            backgroundColor: '#1e293b',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: 'rgba(16, 185, 129, 0.2)',
                color: '#34d399',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                border: '1px solid rgba(16, 185, 129, 0.4)',
              }}
            >
              <FiShield size={18} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.05rem', fontWeight: 700, margin: 0, color: '#f8fafc' }}>
                Officer Complaint Verification
              </h2>
              <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                Complaint #{complaint?.complaintNumber || complaint?.id?.substring(0, 8)}
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '0.4rem',
              borderRadius: '6px',
            }}
          >
            <FiX size={20} />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} style={{ padding: '1.5rem' }}>
          {error && (
            <div
              style={{
                backgroundColor: 'rgba(239, 68, 68, 0.15)',
                color: '#fca5a5',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                padding: '0.75rem 0.9rem',
                borderRadius: '8px',
                fontSize: '0.8rem',
                marginBottom: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              <FiAlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          <div style={{ marginBottom: '1.25rem', backgroundColor: '#1e293b', padding: '0.85rem', borderRadius: '8px', border: '1px solid #334155' }}>
            <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '0.2rem' }}>
              Citizen Reporter: <strong style={{ color: '#f8fafc' }}>{complaint?.citizenName || 'Resident'}</strong>
            </div>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: '#f8fafc', marginBottom: '0.25rem' }}>
              {complaint?.title || complaint?.category}
            </div>
            <p style={{ fontSize: '0.8rem', color: '#cbd5e1', margin: 0 }}>
              {complaint?.description}
            </p>
          </div>

          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '0.5rem' }}>
              Official Verification Assessment *
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.65rem' }}>
              <button
                type="button"
                onClick={() => setStatus('GENUINE')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: status === 'GENUINE' ? '2px solid #10b981' : '1px solid #334155',
                  backgroundColor: status === 'GENUINE' ? 'rgba(16, 185, 129, 0.15)' : '#1e293b',
                  color: status === 'GENUINE' ? '#34d399' : '#cbd5e1',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <FiCheckCircle size={16} color="#10b981" />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.825rem' }}>Genuine Issue</div>
                  <div style={{ fontSize: '0.7rem', color: '#94a3b8' }}>Valid civic issue (awards reputation)</div>
                </div>
              </button>

              <button
                type="button"
                onClick={() => setStatus('REJECTED_SPAM')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: status === 'REJECTED_SPAM' ? '2px solid #ef4444' : '1px solid #334155',
                  backgroundColor: status === 'REJECTED_SPAM' ? 'rgba(239, 68, 68, 0.15)' : '#1e293b',
                  color: status === 'REJECTED_SPAM' ? '#f87171' : '#cbd5e1',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <FiXCircle size={16} color="#ef4444" />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.825rem' }}>Rejected / Spam</div>
                  <div style={{ fontSize: '0.7rem', color: '#94a3b8' }}>Fabricated or abusive filing</div>
                </div>
              </button>

              <button
                type="button"
                onClick={() => setStatus('DUPLICATE')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: status === 'DUPLICATE' ? '2px solid #f59e0b' : '1px solid #334155',
                  backgroundColor: status === 'DUPLICATE' ? 'rgba(245, 158, 11, 0.15)' : '#1e293b',
                  color: status === 'DUPLICATE' ? '#fbbf24' : '#cbd5e1',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <FiCopy size={16} color="#f59e0b" />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.825rem' }}>Duplicate Report</div>
                  <div style={{ fontSize: '0.7rem', color: '#94a3b8' }}>Already reported by another citizen</div>
                </div>
              </button>

              <button
                type="button"
                onClick={() => setStatus('UNVERIFIED')}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: status === 'UNVERIFIED' ? '2px solid #94a3b8' : '1px solid #334155',
                  backgroundColor: status === 'UNVERIFIED' ? 'rgba(148, 163, 184, 0.15)' : '#1e293b',
                  color: status === 'UNVERIFIED' ? '#e2e8f0' : '#cbd5e1',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <FiHelpCircle size={16} color="#94a3b8" />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.825rem' }}>Under Review</div>
                  <div style={{ fontSize: '0.7rem', color: '#94a3b8' }}>Awaiting on-site field check</div>
                </div>
              </button>
            </div>
          </div>

          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '0.35rem' }}>
              Verification Notes & Inspection Reasoning
            </label>
            <textarea
              rows={3}
              placeholder="e.g. Field inspection conducted by Sector 17 engineering team; confirmed open manhole hazard..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              style={{
                width: '100%',
                backgroundColor: '#1e293b',
                color: '#f8fafc',
                border: '1px solid #334155',
                borderRadius: '6px',
                padding: '0.65rem 0.85rem',
                fontSize: '0.825rem',
                resize: 'vertical',
              }}
            />
          </div>

          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '0.75rem',
              borderTop: '1px solid #1e293b',
              paddingTop: '1rem',
            }}
          >
            <button
              type="button"
              onClick={onClose}
              style={{
                backgroundColor: '#334155',
                color: '#cbd5e1',
                border: 'none',
                borderRadius: '6px',
                padding: '0.5rem 1rem',
                fontSize: '0.8rem',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              style={{
                backgroundColor: '#0284c7',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                padding: '0.5rem 1.25rem',
                fontSize: '0.8rem',
                fontWeight: 600,
                cursor: submitting ? 'not-allowed' : 'pointer',
              }}
            >
              {submitting ? 'Updating...' : 'Submit Verification Decision'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

