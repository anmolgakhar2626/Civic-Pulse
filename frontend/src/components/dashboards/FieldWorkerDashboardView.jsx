import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { complaintsApi } from '../../api/complaintsApi';
import { incidentsApi } from '../../api/incidentsApi';
import KpiCard from '../KpiCard';
import StatusBadge from '../StatusBadge';
import PriorityBadge from '../PriorityBadge';
import LoadingSpinner from '../LoadingSpinner';
import ComplaintDetailsModal from '../ComplaintDetailsModal';
import {
  FiTool,
  FiMapPin,
  FiCheckCircle,
  FiClock,
  FiNavigation,
  FiAlertCircle,
  FiRefreshCw,
  FiCompass,
  FiEye,
  FiFileText,
  FiLayers
} from 'react-icons/fi';

export default function FieldWorkerDashboardView({ user }) {
  const [loading, setLoading] = useState(true);
  const [assignedComplaints, setAssignedComplaints] = useState([]);
  const [incidents, setIncidents] = useState([]);
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [viewTab, setViewTab] = useState('complaints'); // complaints, incidents
  const [actionMsg, setActionMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const loadWorkerTasks = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const [compRes, incRes] = await Promise.allSettled([
        complaintsApi.getComplaints({ assignedToUserId: user?.id, size: 50 }).catch(() => null),
        incidentsApi.getIncidents({ size: 30, sort: 'createdAt,desc' }).catch(() => null),
      ]);

      if (compRes.status === 'fulfilled' && compRes.value) {
        const raw = compRes.value.data?.content || compRes.value.data || compRes.value?.content || compRes.value || [];
        setAssignedComplaints(Array.isArray(raw) ? raw : []);
      }
      if (incRes.status === 'fulfilled' && incRes.value) {
        const raw = incRes.value.content || incRes.value.data || incRes.value || [];
        setIncidents(Array.isArray(raw) ? raw : []);
      }
    } catch (err) {
      console.error('Failed to load field worker task board', err);
      setErrorMsg('Failed to fetch assigned field response tasks.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorkerTasks();
  }, []);

  const activeComplaints = assignedComplaints.filter(c => !['RESOLVED', 'CLOSED'].includes(c.status));
  const resolvedComplaints = assignedComplaints.filter(c => ['RESOLVED', 'CLOSED'].includes(c.status));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span className="badge" style={{ backgroundColor: '#0d9488', color: '#fff', fontSize: '0.75rem', fontWeight: 700 }}>
              FIELD CREW RESPONSE DESK
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Operator: <strong>{user?.fullName || user?.username}</strong> {user?.teamName ? `• ${user.teamName}` : ''}
            </span>
          </div>
          <h1>My Assigned Response Tasks & Field Operations</h1>
          <p>On-site work orders, photo evidence capture, GPS locations, progress logs, and resolution proof</p>
        </div>

        <button onClick={loadWorkerTasks} className="btn btn-outline btn-sm" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          <FiRefreshCw size={13} />
          Refresh Tasks
        </button>
      </div>

      {actionMsg && (
        <div style={{ backgroundColor: '#f0fdf4', color: '#166534', border: '1px solid #bbf7d0', padding: '0.6rem 0.85rem', borderRadius: '6px', fontSize: '0.825rem' }}>
          <FiCheckCircle style={{ marginRight: '6px', verticalAlign: 'middle' }} />
          {actionMsg}
        </div>
      )}

      {errorMsg && (
        <div style={{ backgroundColor: '#fef2f2', color: '#991b1b', border: '1px solid #fecaca', padding: '0.6rem 0.85rem', borderRadius: '6px', fontSize: '0.825rem' }}>
          <FiAlertCircle style={{ marginRight: '6px', verticalAlign: 'middle' }} />
          {errorMsg}
        </div>
      )}

      {/* KPI Cards */}
      <div className="cc-kpi-grid">
        <KpiCard
          label="Assigned Grievances"
          value={assignedComplaints.length}
          subtext={`${activeComplaints.length} active in field`}
          icon={<FiFileText size={16} />}
          variant="accent"
        />
        <KpiCard
          label="In Progress (On-Site)"
          value={activeComplaints.filter((c) => c.status === 'IN_PROGRESS').length}
          subtext="Under active remediation"
          icon={<FiClock size={16} />}
          variant="high"
        />
        <KpiCard
          label="Pending Transit"
          value={activeComplaints.filter((c) => c.status === 'ASSIGNED').length}
          subtext="Assigned, ready to travel"
          icon={<FiCompass size={16} />}
          variant="warning"
        />
        <KpiCard
          label="Remediated / Resolved"
          value={resolvedComplaints.length}
          subtext="Completed field tasks"
          icon={<FiCheckCircle size={16} />}
          variant="low"
        />
      </div>

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--border-light)', paddingBottom: '0.25rem' }}>
        <button
          onClick={() => setViewTab('complaints')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: '6px',
            border: viewTab === 'complaints' ? '1px solid var(--accent-primary)' : '1px solid var(--border-light)',
            backgroundColor: viewTab === 'complaints' ? 'var(--accent-primary)' : '#ffffff',
            color: viewTab === 'complaints' ? '#ffffff' : 'var(--text-main)',
            fontSize: '0.8rem',
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          Citizen Grievance Work Orders ({assignedComplaints.length})
        </button>
        <button
          onClick={() => setViewTab('incidents')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: '6px',
            border: viewTab === 'incidents' ? '1px solid var(--accent-primary)' : '1px solid var(--border-light)',
            backgroundColor: viewTab === 'incidents' ? 'var(--accent-primary)' : '#ffffff',
            color: viewTab === 'incidents' ? '#ffffff' : 'var(--text-main)',
            fontSize: '0.8rem',
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          Incident Dispatch Queue ({incidents.length})
        </button>
      </div>

      {loading ? (
        <LoadingSpinner text="Connecting to Field Task Board..." />
      ) : viewTab === 'complaints' ? (
        /* ASSIGNED CITIZEN GRIEVANCES TABLE */
        <div className="cc-card">
          <div className="cc-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="cc-card-title">
              <FiTool size={16} color="#0d9488" />
              Assigned Grievance Tasks ({assignedComplaints.length})
            </span>
            <Link to="/map" className="btn btn-outline btn-sm" style={{ fontSize: '0.725rem' }}>
              <FiMapPin size={12} style={{ marginRight: '4px' }} />
              Open Navigation Map
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', padding: '1rem' }}>
            {assignedComplaints.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '2.5rem', color: 'var(--text-muted)' }}>
                <FiTool size={32} style={{ color: 'var(--text-subtle)', marginBottom: '0.5rem' }} />
                <div style={{ fontWeight: 600 }}>No grievance tasks currently assigned to you.</div>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.25rem' }}>
                  When your Department Officer assigns a civic grievance to you, it will appear here with SLA targets.
                </p>
              </div>
            ) : (
              assignedComplaints.map(c => {
                const remaining = c.remainingMinutes != null ? c.remainingMinutes : 0;
                let slaBadge = { label: 'ON TRACK', bg: '#f0fdf4', color: '#10B981' };
                if (['RESOLVED', 'CLOSED'].includes(c.status)) {
                  slaBadge = { label: 'RESOLVED', bg: '#ecfdf5', color: '#065f46' };
                } else if (remaining < 0) {
                  slaBadge = { label: 'OVERDUE', bg: '#fef2f2', color: '#dc2626' };
                } else if (remaining <= 120) {
                  slaBadge = { label: 'AT RISK', bg: '#fff7ed', color: '#ea580c' };
                }

                return (
                  <div
                    key={c.id}
                    style={{
                      border: '1px solid var(--border-light)',
                      borderRadius: '8px',
                      padding: '1rem',
                      backgroundColor: '#ffffff',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      gap: '0.75rem',
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem', flexWrap: 'wrap' }}>
                        <span style={{ fontFamily: 'monospace', fontWeight: 800, fontSize: '0.85rem', color: 'var(--accent-primary)', backgroundColor: '#e0f2fe', padding: '1px 6px', borderRadius: '4px' }}>
                          {c.complaintNumber}
                        </span>
                        <span style={{ fontSize: '0.7rem', fontWeight: 700, padding: '2px 8px', borderRadius: '9999px', backgroundColor: '#f1f5f9', color: '#334155' }}>
                          {c.status}
                        </span>
                        <span style={{ fontSize: '0.7rem', fontWeight: 700, padding: '2px 8px', borderRadius: '9999px', backgroundColor: slaBadge.bg, color: slaBadge.color }}>
                          {slaBadge.label}
                        </span>
                      </div>

                      <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)', margin: '0.2rem 0' }}>
                        {c.title}
                      </h3>

                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                        <span>
                          <FiMapPin size={11} style={{ marginRight: '2px', verticalAlign: 'middle' }} />
                          {c.address}
                        </span>
                        <span>Citizen: <strong>{c.citizenName}</strong> ({c.citizenPhone})</span>
                        <span>Priority: <strong>{c.priority}</strong></span>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button
                        onClick={() => setSelectedComplaint(c)}
                        className="btn btn-primary btn-sm"
                        style={{ display: 'flex', alignItems: 'center', gap: '4px', backgroundColor: 'var(--accent-primary)', color: '#ffffff', fontSize: '0.75rem', fontWeight: 600 }}
                      >
                        <FiEye size={12} />
                        Update Work & Proof
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      ) : (
        /* INCIDENTS TABLE */
        <div className="cc-card">
          <div className="cc-card-header">
            <span className="cc-card-title">
              <FiTool size={16} color="#0d9488" />
              Incidents Dispatch Queue ({incidents.length})
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', padding: '1rem' }}>
            {incidents.slice(0, 10).map((t) => (
              <div
                key={t.id}
                style={{
                  border: '1px solid var(--border-light)',
                  borderRadius: '6px',
                  padding: '0.85rem 1rem',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  backgroundColor: '#ffffff',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                    <Link to={`/incidents/${t.id}`} style={{ fontWeight: 700, color: 'var(--text-main)', fontSize: '0.9rem', textDecoration: 'none' }}>
                      {t.title || t.incidentNumber}
                    </Link>
                    <PriorityBadge tier={t.tier || t.priorityTier || t.priority} />
                    <StatusBadge status={t.status || t.currentStatus} />
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    <FiMapPin size={11} style={{ marginRight: '3px' }} />
                    {t.address || t.locationName || 'City Location'}
                  </div>
                </div>

                <Link to={`/incidents/${t.id}`} className="btn btn-outline btn-sm" style={{ fontSize: '0.75rem' }}>
                  View Incident
                </Link>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Complaint Details Modal */}
      {selectedComplaint && (
        <ComplaintDetailsModal
          complaintId={selectedComplaint.id}
          initialComplaint={selectedComplaint}
          onClose={() => setSelectedComplaint(null)}
          onUpdated={() => {
            loadWorkerTasks();
          }}
        />
      )}
    </div>
  );
}
