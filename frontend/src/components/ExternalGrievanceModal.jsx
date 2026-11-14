import React, { useState, useEffect } from 'react';
import { complaintsApi } from '../api/complaintsApi';
import LoadingSpinner from './LoadingSpinner';
import {
  FiExternalLink,
  FiCheckCircle,
  FiClock,
  FiRefreshCw,
  FiDownload,
  FiX,
  FiShield,
  FiUserCheck,
  FiAlertCircle
} from 'react-icons/fi';

export default function ExternalGrievanceModal({ complaintId, onClose, onSynced }) {
  const [tracking, setTracking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState('');
  const [syncMsg, setSyncMsg] = useState('');

  const loadTracking = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await complaintsApi.getExternalTracking(complaintId);
      setTracking(res.data || res);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to retrieve government tracking information.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (complaintId) {
      loadTracking();
    }
  }, [complaintId]);

  const handleSync = async () => {
    setSyncing(true);
    setSyncMsg('');
    setError('');
    try {
      const res = await complaintsApi.syncExternalStatus(complaintId);
      setTracking(res.data || res);
      setSyncMsg('Progress updated successfully from government portal!');
      if (onSynced) onSynced();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to sync status.');
    } finally {
      setSyncing(false);
    }
  };

  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'ACKNOWLEDGED':
        return { bg: 'rgba(56, 189, 248, 0.15)', text: '#38bdf8', border: 'rgba(56, 189, 248, 0.4)' };
      case 'UNDER_PROCESS':
        return { bg: 'rgba(234, 179, 8, 0.15)', text: '#facc15', border: 'rgba(234, 179, 8, 0.4)' };
      case 'ACTION_TAKEN':
        return { bg: 'rgba(168, 85, 247, 0.15)', text: '#c084fc', border: 'rgba(168, 85, 247, 0.4)' };
      case 'RESOLVED':
        return { bg: 'rgba(34, 197, 94, 0.15)', text: '#4ade80', border: 'rgba(34, 197, 94, 0.4)' };
      default:
        return { bg: 'rgba(148, 163, 184, 0.15)', text: '#94a3b8', border: 'rgba(148, 163, 184, 0.4)' };
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
          maxWidth: '680px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
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
                backgroundColor: 'rgba(2, 132, 199, 0.2)',
                color: '#38bdf8',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                border: '1px solid rgba(2, 132, 199, 0.4)',
              }}
            >
              <FiShield size={18} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.05rem', fontWeight: 700, margin: 0, color: '#f8fafc' }}>
                Government Grievance Portal Tracking
              </h2>
              <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                Official Public Grievance Redressal Integration
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

        {/* Content Body */}
        <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1 }}>
          {loading ? (
            <div style={{ padding: '2rem', textAlign: 'center' }}>
              <LoadingSpinner text="Retrieving government grievance records..." />
            </div>
          ) : error ? (
            <div
              style={{
                backgroundColor: 'rgba(239, 68, 68, 0.15)',
                color: '#fca5a5',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                padding: '1rem',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                fontSize: '0.85rem',
              }}
            >
              <FiAlertCircle size={18} />
              <span>{error}</span>
            </div>
          ) : tracking ? (
            <div>
              {syncMsg && (
                <div
                  style={{
                    backgroundColor: 'rgba(34, 197, 94, 0.15)',
                    color: '#86efac',
                    border: '1px solid rgba(34, 197, 94, 0.3)',
                    padding: '0.65rem 0.85rem',
                    borderRadius: '6px',
                    fontSize: '0.8rem',
                    marginBottom: '1rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                  }}
                >
                  <FiCheckCircle size={14} />
                  <span>{syncMsg}</span>
                </div>
              )}

              {/* Portal & Status Card */}
              <div
                style={{
                  backgroundColor: '#1e293b',
                  borderRadius: '8px',
                  padding: '1.25rem',
                  border: '1px solid #334155',
                  marginBottom: '1.25rem',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                  <div>
                    <span style={{ fontSize: '0.7rem', textTransform: 'uppercase', color: '#94a3b8', letterSpacing: '0.05em', fontWeight: 600 }}>
                      Integrated Portal
                    </span>
                    <h3 style={{ fontSize: '1rem', fontWeight: 700, margin: '0.2rem 0', color: '#f8fafc' }}>
                      {tracking.portalName}
                    </h3>
                    {tracking.sandbox && (
                      <span
                        style={{
                          fontSize: '0.65rem',
                          backgroundColor: 'rgba(245, 158, 11, 0.15)',
                          color: '#fcd34d',
                          border: '1px solid rgba(245, 158, 11, 0.3)',
                          padding: '0.15rem 0.5rem',
                          borderRadius: '4px',
                          fontWeight: 700,
                        }}
                      >
                        DEVELOPMENT SANDBOX ADAPTER
                      </span>
                    )}
                  </div>

                  {(() => {
                    const st = getStatusColor(tracking.externalStatus);
                    return (
                      <span
                        style={{
                          backgroundColor: st.bg,
                          color: st.text,
                          border: `1px solid ${st.border}`,
                          padding: '0.35rem 0.75rem',
                          borderRadius: '9999px',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          letterSpacing: '0.025em',
                        }}
                      >
                        {tracking.externalStatus}
                      </span>
                    );
                  })()}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', fontSize: '0.8rem', marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid #334155' }}>
                  <div>
                    <span style={{ color: '#94a3b8', display: 'block', fontSize: '0.7rem' }}>Acknowledgement Number</span>
                    <strong style={{ color: '#38bdf8', fontFamily: 'monospace', fontSize: '0.9rem' }}>
                      {tracking.acknowledgementNumber}
                    </strong>
                  </div>

                  <div>
                    <span style={{ color: '#94a3b8', display: 'block', fontSize: '0.7rem' }}>Nodal Officer Assigned</span>
                    <span style={{ color: '#e2e8f0', fontWeight: 500 }}>
                      {tracking.officerInCharge || 'Public Grievance Cell'}
                    </span>
                  </div>

                  <div>
                    <span style={{ color: '#94a3b8', display: 'block', fontSize: '0.7rem' }}>Portal Intake Date</span>
                    <span style={{ color: '#e2e8f0' }}>
                      {tracking.submittedAt ? new Date(tracking.submittedAt).toLocaleString() : '--'}
                    </span>
                  </div>

                  <div>
                    <span style={{ color: '#94a3b8', display: 'block', fontSize: '0.7rem' }}>Target Redressal Date</span>
                    <span style={{ color: '#fbbf24', fontWeight: 600 }}>
                      {tracking.targetResolutionDate ? new Date(tracking.targetResolutionDate).toLocaleDateString() : '15 Days SLA'}
                    </span>
                  </div>
                </div>

                {tracking.statusDescription && (
                  <div style={{ marginTop: '0.75rem', padding: '0.6rem 0.8rem', backgroundColor: '#0f172a', borderRadius: '6px', fontSize: '0.775rem', color: '#cbd5e1' }}>
                    <span style={{ fontWeight: 600, color: '#94a3b8' }}>Latest Notes: </span>
                    {tracking.statusDescription}
                  </div>
                )}
              </div>

              {/* Progress History Timeline */}
              <div style={{ marginBottom: '1.25rem' }}>
                <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: '#e2e8f0', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <FiClock size={14} color="#38bdf8" /> Official Redressal Timeline
                </h4>

                {(!tracking.timeline || tracking.timeline.length === 0) ? (
                  <div style={{ padding: '1rem', backgroundColor: '#1e293b', borderRadius: '6px', fontSize: '0.75rem', color: '#94a3b8', textAlign: 'center' }}>
                    Registration acknowledged. Progress events will appear as municipal officers review and act on this issue.
                  </div>
                ) : (
                  <div style={{ position: 'relative', paddingLeft: '1.5rem', borderLeft: '2px solid #334155' }}>
                    {tracking.timeline.map((step, idx) => {
                      const st = getStatusColor(step.status);
                      return (
                        <div key={step.id || idx} style={{ marginBottom: '1rem', position: 'relative' }}>
                          <div
                            style={{
                              position: 'absolute',
                              left: '-1.85rem',
                              top: '2px',
                              width: '10px',
                              height: '10px',
                              borderRadius: '50%',
                              backgroundColor: st.text,
                              boxShadow: `0 0 8px ${st.text}`,
                            }}
                          />
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '0.15rem' }}>
                            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: st.text }}>
                              {step.status}
                            </span>
                            <span style={{ fontSize: '0.675rem', color: '#94a3b8' }}>
                              {step.eventTimestamp ? new Date(step.eventTimestamp).toLocaleString() : '--'}
                            </span>
                          </div>
                          <p style={{ margin: 0, fontSize: '0.775rem', color: '#cbd5e1' }}>
                            {step.remarks}
                          </p>
                          {step.actionTakenBy && (
                            <span style={{ fontSize: '0.7rem', color: '#64748b', display: 'block', marginTop: '0.2rem' }}>
                              Action By: {step.actionTakenBy}
                            </span>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Acknowledgement Documents */}
              {tracking.documents && tracking.documents.length > 0 && (
                <div style={{ marginBottom: '1.25rem' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: '#e2e8f0', marginBottom: '0.6rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                    <FiDownload size={14} color="#38bdf8" /> Official Electronic Receipts & Documents
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {tracking.documents.map((doc) => (
                      <div
                        key={doc.id || doc.documentReference}
                        style={{
                          backgroundColor: '#1e293b',
                          borderRadius: '6px',
                          border: '1px solid #334155',
                          padding: '0.65rem 0.9rem',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                        }}
                      >
                        <div>
                          <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#f8fafc', display: 'block' }}>
                            {doc.documentTitle}
                          </span>
                          <span style={{ fontSize: '0.7rem', color: '#94a3b8' }}>
                            Ref: {doc.documentReference} • Issued: {doc.issuedAt ? new Date(doc.issuedAt).toLocaleDateString() : 'Official'}
                          </span>
                        </div>
                        <a
                          href={complaintsApi.getAcknowledgementDocumentUrl(doc.documentReference)}
                          target="_blank"
                          rel="noopener noreferrer"
                          download={doc.documentReference}
                          style={{
                            backgroundColor: '#0284c7',
                            color: '#ffffff',
                            textDecoration: 'none',
                            padding: '0.35rem 0.75rem',
                            borderRadius: '6px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem',
                          }}
                        >
                          <FiDownload size={12} /> Download Receipt
                        </a>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : null}
        </div>

        {/* Footer Actions */}
        <div
          style={{
            padding: '1rem 1.5rem',
            borderTop: '1px solid #1e293b',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            backgroundColor: '#1e293b',
          }}
        >
          <button
            onClick={handleSync}
            disabled={syncing || loading}
            style={{
              backgroundColor: '#334155',
              color: '#f8fafc',
              border: 'none',
              borderRadius: '6px',
              padding: '0.5rem 0.9rem',
              fontSize: '0.775rem',
              fontWeight: 600,
              cursor: syncing ? 'not-allowed' : 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
            }}
          >
            <FiRefreshCw size={13} className={syncing ? 'animate-spin' : ''} />
            {syncing ? 'Polling Upstream...' : 'Poll / Sync Live Status'}
          </button>

          <button
            onClick={onClose}
            style={{
              backgroundColor: '#0284c7',
              color: '#ffffff',
              border: 'none',
              borderRadius: '6px',
              padding: '0.5rem 1.25rem',
              fontSize: '0.8rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

