import React, { useState, useEffect } from 'react';
import { complaintsApi } from '../../api/complaintsApi';
import KpiCard from '../KpiCard';
import LoadingSpinner from '../LoadingSpinner';
import ComplaintDetailsModal from '../ComplaintDetailsModal';
import {
  FiBriefcase,
  FiClock,
  FiCheckSquare,
  FiAlertTriangle,
  FiRefreshCw,
  FiFilter,
  FiMapPin,
  FiCheckCircle,
  FiEye,
  FiUserCheck,
  FiAward
} from 'react-icons/fi';

export default function DepartmentOfficerDashboardView({ user }) {
  const [loading, setLoading] = useState(true);
  const [deptStats, setDeptStats] = useState(null);
  const [complaints, setComplaints] = useState([]);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [statusMsg, setStatusMsg] = useState('');
  const [selectedComplaint, setSelectedComplaint] = useState(null);

  const deptCode = user?.departmentCode || 'PWD';
  const deptName = user?.departmentName || 'Public Works Department';

  const loadDeptData = async () => {
    setLoading(true);
    setStatusMsg('');
    try {
      const [statsRes, compRes] = await Promise.allSettled([
        complaintsApi.getDepartmentStats().catch(() => null),
        complaintsApi.getComplaints({ size: 100 }).catch(() => null),
      ]);

      if (statsRes.status === 'fulfilled' && statsRes.value) {
        setDeptStats(statsRes.value.data || statsRes.value);
      }
      if (compRes.status === 'fulfilled' && compRes.value) {
        const raw = compRes.value.data?.content || compRes.value.data || compRes.value?.content || compRes.value || [];
        setComplaints(Array.isArray(raw) ? raw : []);
      }
    } catch (err) {
      console.error('Failed to load department workload', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDeptData();
  }, [deptCode]);

  const needsAssignCount = complaints.filter(c => c.status === 'SUBMITTED' || !c.assignedToUserId).length;
  const inFieldCount = complaints.filter(c => ['ASSIGNED', 'IN_PROGRESS'].includes(c.status)).length;
  const needsVerifyCount = complaints.filter(c => c.status === 'RESOLVED').length;
  const closedCount = complaints.filter(c => c.status === 'CLOSED').length;
  const criticalCount = complaints.filter(c => (c.priority || '').toUpperCase() === 'CRITICAL').length;
  const breachedCount = complaints.filter(c => c.slaStatus === 'BREACHED' || (c.slaDeadline && new Date(c.slaDeadline) < new Date() && !['RESOLVED', 'CLOSED'].includes(c.status))).length;

  const filteredComplaints = complaints.filter((c) => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'NEEDS_ASSIGNMENT') return c.status === 'SUBMITTED' || !c.assignedToUserId;
    if (statusFilter === 'IN_FIELD') return ['ASSIGNED', 'IN_PROGRESS'].includes(c.status);
    if (statusFilter === 'NEEDS_VERIFY') return c.status === 'RESOLVED';
    if (statusFilter === 'CLOSED') return c.status === 'CLOSED';
    if (statusFilter === 'CRITICAL') return (c.priority || '').toUpperCase() === 'CRITICAL';
    if (statusFilter === 'BREACHED') {
      return c.slaStatus === 'BREACHED' || (c.slaDeadline && new Date(c.slaDeadline) < new Date() && !['RESOLVED', 'CLOSED'].includes(c.status));
    }
    return true;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span className="badge badge-medium" style={{ fontSize: '0.75rem', fontWeight: 700 }}>
              DEPARTMENTAL OPERATIONS PORTAL
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Jurisdiction: <strong>{deptStats?.departmentName || deptName} ({deptStats?.departmentCode || deptCode})</strong> • Officer: <strong>{user?.fullName || user?.username}</strong>
            </span>
          </div>
          <h1>{deptStats?.departmentName || deptName} — Grievance Management Board</h1>
          <p>
            Assign field crews, review work progress evidence, verify citizen reports for the Verified Citizen badge, and confirm resolution proof
          </p>
        </div>

        <button onClick={loadDeptData} className="btn btn-outline btn-sm" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          <FiRefreshCw size={13} />
          Refresh Board
        </button>
      </div>

      {statusMsg && (
        <div style={{ backgroundColor: '#f0fdf4', color: '#166534', border: '1px solid #bbf7d0', padding: '0.65rem 0.85rem', borderRadius: '6px', fontSize: '0.825rem' }}>
          <FiCheckCircle style={{ marginRight: '6px', verticalAlign: 'middle' }} />
          {statusMsg}
        </div>
      )}

      {/* Real Department KPI Metrics */}
      <div className="cc-kpi-grid">
        <KpiCard
          label="Total Grievances"
          value={deptStats?.totalComplaints ?? complaints.length}
          subtext="Assigned to this Department"
          icon={<FiBriefcase size={16} />}
          variant="accent"
          onClick={() => setStatusFilter('ALL')}
        />
        <KpiCard
          label="Pending Assignment"
          value={deptStats?.pendingComplaints ?? needsAssignCount}
          subtext="Requires Worker Dispatch"
          icon={<FiClock size={16} />}
          variant={needsAssignCount > 0 ? 'warning' : 'accent'}
          onClick={() => setStatusFilter('NEEDS_ASSIGNMENT')}
        />
        <KpiCard
          label="Active in Field"
          value={deptStats?.inProgressComplaints ?? inFieldCount}
          subtext="Crews En Route / In Progress"
          icon={<FiCheckSquare size={16} />}
          variant="accent"
          onClick={() => setStatusFilter('IN_FIELD')}
        />
        <KpiCard
          label="Awaiting Verification"
          value={needsVerifyCount}
          subtext="Resolved Tasks Ready for Review"
          icon={<FiUserCheck size={16} />}
          variant={needsVerifyCount > 0 ? 'warning' : 'low'}
          onClick={() => setStatusFilter('NEEDS_VERIFY')}
        />
        <KpiCard
          label="SLA Breached / Overdue"
          value={Math.max(deptStats?.breachedDeadline ?? deptStats?.overdueComplaints ?? 0, breachedCount)}
          subtext="Exceeded Response Window"
          icon={<FiAlertTriangle size={16} />}
          variant="critical"
          onClick={() => setStatusFilter('BREACHED')}
        />
        <KpiCard
          label="Closed / Resolved"
          value={(deptStats?.resolvedComplaints || 0) + (deptStats?.closedComplaints || 0)}
          subtext="Successfully Redressed"
          icon={<FiCheckCircle size={16} />}
          variant="low"
          onClick={() => setStatusFilter('CLOSED')}
        />
      </div>

      {/* Main Departmental Complaints Table */}
      <div className="cc-card">
        <div className="cc-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
          <span className="cc-card-title">
            <FiBriefcase size={16} color="var(--accent-primary)" />
            Departmental Complaint Redressal Queue ({filteredComplaints.length})
          </span>

          {/* Filter Pills */}
          <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
            {[
              { id: 'ALL', label: `All (${complaints.length})` },
              { id: 'CRITICAL', label: `Critical Priority (${criticalCount})` },
              { id: 'NEEDS_ASSIGNMENT', label: `Needs Assignment (${needsAssignCount})` },
              { id: 'IN_FIELD', label: `Active in Field (${inFieldCount})` },
              { id: 'BREACHED', label: `SLA Breached (${breachedCount})` },
              { id: 'NEEDS_VERIFY', label: `Awaiting Verification (${needsVerifyCount})` },
              { id: 'CLOSED', label: `Closed (${closedCount})` },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setStatusFilter(tab.id)}
                style={{
                  padding: '3px 8px',
                  borderRadius: '6px',
                  border: statusFilter === tab.id ? '1px solid var(--accent-primary)' : '1px solid var(--border-light)',
                  backgroundColor: statusFilter === tab.id ? 'rgba(2, 132, 199, 0.1)' : '#ffffff',
                  color: statusFilter === tab.id ? 'var(--accent-primary)' : 'var(--text-muted)',
                  fontSize: '0.725rem',
                  fontWeight: statusFilter === tab.id ? 700 : 500,
                  cursor: 'pointer',
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table className="cc-table" style={{ width: '100%', textAlign: 'left', fontSize: '0.825rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border-medium)' }}>
                <th style={{ padding: '0.75rem 1rem' }}>Record #</th>
                <th style={{ padding: '0.75rem 1rem' }}>Grievance Details</th>
                <th style={{ padding: '0.75rem 1rem' }}>Citizen</th>
                <th style={{ padding: '0.75rem 1rem' }}>Assigned Worker</th>
                <th style={{ padding: '0.75rem 1rem' }}>Priority</th>
                <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                <th style={{ padding: '0.75rem 1rem' }}>Verification</th>
                <th style={{ padding: '0.75rem 1rem' }}>SLA Target</th>
                <th style={{ padding: '0.75rem 1rem' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredComplaints.length === 0 ? (
                <tr>
                  <td colSpan={9} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                    No complaints currently in this queue view.
                  </td>
                </tr>
              ) : (
                filteredComplaints.map(c => {
                  const remaining = c.remainingMinutes != null ? c.remainingMinutes : 0;
                  let slaColor = '#16a34a';
                  let slaText = `${Math.floor(remaining / 60)}h ${Math.abs(remaining % 60)}m`;
                  if (['RESOLVED', 'CLOSED'].includes(c.status)) {
                    slaColor = 'var(--accent-teal)';
                    slaText = 'Resolved';
                  } else if (remaining < 0) {
                    slaColor = '#dc2626';
                    slaText = `Overdue (${Math.abs(Math.floor(remaining / 60))}h)`;
                  } else if (remaining <= 120) {
                    slaColor = '#ea580c';
                  }

                  return (
                    <tr key={c.id} style={{ borderBottom: '1px solid var(--border-light)' }}>
                      <td style={{ padding: '0.75rem 1rem', fontFamily: 'monospace', fontWeight: 800, color: 'var(--accent-primary)' }}>
                        {c.complaintNumber}
                      </td>
                      <td style={{ padding: '0.75rem 1rem', maxWidth: '240px' }}>
                        <div style={{ fontWeight: 600, color: 'var(--text-main)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {c.title}
                        </div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                          <FiMapPin size={10} style={{ marginRight: '2px' }} />
                          {c.address}
                        </div>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div>{c.citizenName} {c.citizenIsVerifiedBadge && '🏅'}</div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{c.citizenPhone}</div>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        {c.assignedToName ? (
                          <span style={{ fontWeight: 600, color: 'var(--accent-teal)' }}>{c.assignedToName}</span>
                        ) : (
                          <span style={{ color: '#b45309', fontWeight: 600, fontSize: '0.75rem' }}>⚠ Unassigned</span>
                        )}
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <span
                          style={{
                            fontSize: '0.675rem',
                            fontWeight: 700,
                            padding: '2px 6px',
                            borderRadius: '4px',
                            backgroundColor: c.priority === 'CRITICAL' ? '#fef2f2' : c.priority === 'HIGH' ? '#fff7ed' : '#f1f5f9',
                            color: c.priority === 'CRITICAL' ? '#dc2626' : c.priority === 'HIGH' ? '#ea580c' : '#334155'
                          }}
                        >
                          {c.priority}
                        </span>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <span
                          style={{
                            fontSize: '0.675rem',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '9999px',
                            backgroundColor: '#f1f5f9',
                            color: '#334155',
                            border: '1px solid var(--border-light)'
                          }}
                        >
                          {c.status}
                        </span>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <span
                          style={{
                            fontSize: '0.675rem',
                            fontWeight: 700,
                            padding: '2px 6px',
                            borderRadius: '4px',
                            backgroundColor: c.complaintVerificationStatus === 'GENUINE' ? '#f0fdf4' : '#f8fafc',
                            color: c.complaintVerificationStatus === 'GENUINE' ? '#16a34a' : '#64748b'
                          }}
                        >
                          {c.complaintVerificationStatus || 'UNVERIFIED'}
                        </span>
                      </td>
                      <td style={{ padding: '0.75rem 1rem', fontWeight: 600, color: slaColor }}>
                        {slaText}
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <button
                          onClick={() => setSelectedComplaint(c)}
                          className="btn btn-outline btn-sm"
                          style={{ fontSize: '0.725rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                        >
                          <FiEye size={12} />
                          Manage Task
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Complaint Details & Action Modal */}
      {selectedComplaint && (
        <ComplaintDetailsModal
          complaintId={selectedComplaint.id}
          initialComplaint={selectedComplaint}
          onClose={() => setSelectedComplaint(null)}
          onUpdated={() => {
            loadDeptData();
          }}
        />
      )}
    </div>
  );
}
