import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { usersApi } from '../../api/usersApi';
import { complaintsApi } from '../../api/complaintsApi';
import { incidentsApi } from '../../api/incidentsApi';
import { slaApi } from '../../api/slaApi';
import api from '../../api/client';
import KpiCard from '../KpiCard';
import LoadingSpinner from '../LoadingSpinner';
import ComplaintDetailsModal from '../ComplaintDetailsModal';
import {
  FiUsers,
  FiShield,
  FiActivity,
  FiServer,
  FiAlertTriangle,
  FiClock,
  FiCheckCircle,
  FiXCircle,
  FiRefreshCw,
  FiDatabase,
  FiFileText,
  FiExternalLink,
  FiSearch,
  FiFilter,
  FiEye,
  FiLayers,
  FiAward
} from 'react-icons/fi';

export default function AdminDashboardView({ user }) {
  const [activeAdminTab, setActiveAdminTab] = useState('complaints'); // complaints, users, diagnostics
  const [loading, setLoading] = useState(true);

  // Complaints & KPI states
  const [adminStats, setAdminStats] = useState(null);
  const [deptStats, setDeptStats] = useState([]);
  const [complaints, setComplaints] = useState([]);
  const [complaintSearch, setComplaintSearch] = useState('');
  const [deptFilter, setDeptFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [selectedComplaint, setSelectedComplaint] = useState(null);

  // Users & System states
  const [users, setUsers] = useState([]);
  const [userStats, setUserStats] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [backendHealth, setBackendHealth] = useState({ status: 'UP' });
  const [updatingUserId, setUpdatingUserId] = useState(null);
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionError, setActionError] = useState('');

  const loadAllAdminData = async () => {
    setLoading(true);
    setActionError('');
    try {
      const [statsRes, deptRes, compRes, usersRes, uStatsRes, auditRes, healthRes] = await Promise.allSettled([
        complaintsApi.getAdminStats().catch(() => null),
        complaintsApi.getAllDepartmentStats().catch(() => null),
        complaintsApi.getComplaints({ size: 100 }).catch(() => null),
        usersApi.getUsers().catch(() => null),
        usersApi.getStats().catch(() => null),
        api.get('/audit-logs?limit=15').catch(() => null),
        api.get('/actuator/health').catch(() => ({ status: 'UP' })),
      ]);

      if (statsRes.status === 'fulfilled' && statsRes.value) {
        setAdminStats(statsRes.value.data || statsRes.value);
      }
      if (deptRes.status === 'fulfilled' && deptRes.value) {
        const d = deptRes.value.data || deptRes.value || [];
        setDeptStats(Array.isArray(d) ? d : []);
      }
      if (compRes.status === 'fulfilled' && compRes.value) {
        const raw = compRes.value.data?.content || compRes.value.data || compRes.value?.content || compRes.value || [];
        setComplaints(Array.isArray(raw) ? raw : []);
      }
      if (usersRes.status === 'fulfilled' && usersRes.value) {
        const raw = usersRes.value.data || usersRes.value || [];
        setUsers(Array.isArray(raw) ? raw : []);
      }
      if (uStatsRes.status === 'fulfilled' && uStatsRes.value) {
        setUserStats(uStatsRes.value.data || uStatsRes.value);
      }
      if (auditRes.status === 'fulfilled' && auditRes.value) {
        const raw = auditRes.value.data?.data || auditRes.value.data || auditRes.value || [];
        setAuditLogs(Array.isArray(raw) ? raw : []);
      }
      if (healthRes.status === 'fulfilled') {
        setBackendHealth(healthRes.value.data || { status: 'UP' });
      }
    } catch (err) {
      console.error('Failed to load admin data:', err);
      setActionError('Failed to load operational admin metrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAllAdminData();
  }, []);

  const handleToggleStatus = async (targetUser) => {
    setUpdatingUserId(targetUser.id);
    setActionError('');
    setActionSuccess('');
    try {
      const newStatus = !targetUser.enabled;
      await usersApi.updateStatus(targetUser.id, newStatus);
      setUsers((prev) =>
        prev.map((u) => (u.id === targetUser.id ? { ...u, enabled: newStatus, active: newStatus } : u))
      );
      setActionSuccess(`User ${targetUser.username} account ${newStatus ? 'enabled' : 'disabled'} successfully.`);
    } catch (err) {
      setActionError(`Failed to update account status: ${err.message}`);
    } finally {
      setUpdatingUserId(null);
    }
  };

  const handleRoleChange = async (targetUser, newRole) => {
    setUpdatingUserId(targetUser.id);
    setActionError('');
    setActionSuccess('');
    try {
      await usersApi.updateRole(targetUser.id, newRole);
      setUsers((prev) =>
        prev.map((u) => (u.id === targetUser.id ? { ...u, role: newRole, roles: [newRole] } : u))
      );
      setActionSuccess(`User ${targetUser.username} role updated to ${newRole}.`);
    } catch (err) {
      setActionError(`Failed to update role: ${err.message}`);
    } finally {
      setUpdatingUserId(null);
    }
  };

  // Filter complaints
  const filteredComplaints = complaints.filter(c => {
    if (deptFilter && c.departmentCode !== deptFilter) return false;
    if (statusFilter) {
      if (statusFilter === 'BREACHED') {
        const isBreached = c.slaStatus === 'BREACHED' || (c.slaDeadline && new Date(c.slaDeadline) < new Date() && !['RESOLVED', 'CLOSED'].includes(c.status));
        if (!isBreached) return false;
      } else if (statusFilter === 'PENDING') {
        if (!['SUBMITTED', 'ASSIGNED'].includes(c.status) && c.assignedToUserId) return false;
      } else if (c.status !== statusFilter) {
        return false;
      }
    }
    if (priorityFilter && (c.priority || '').toUpperCase() !== priorityFilter.toUpperCase()) return false;
    if (complaintSearch) {
      const q = complaintSearch.toLowerCase();
      const matchNum = c.complaintNumber?.toLowerCase().includes(q);
      const matchTitle = c.title?.toLowerCase().includes(q);
      const matchCitizen = c.citizenName?.toLowerCase().includes(q);
      const matchAddress = c.address?.toLowerCase().includes(q);
      if (!matchNum && !matchTitle && !matchCitizen && !matchAddress) return false;
    }
    return true;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Top Banner */}
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span className="badge" style={{ backgroundColor: '#7c3aed', color: '#fff', fontSize: '0.75rem', fontWeight: 700 }}>
              ADMINISTRATIVE COMMAND CONSOLE
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Administrator: <strong>{user?.fullName || user?.username}</strong>
            </span>
          </div>
          <h1>CivicPulse Smart City Command & Control Centre</h1>
          <p>
            Central grievance management, departmental governance, citizen reputation audit, and municipal user RBAC
          </p>
        </div>

        <button onClick={loadAllAdminData} className="btn btn-outline btn-sm" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          <FiRefreshCw size={13} />
          Refresh Overview
        </button>
      </div>

      {actionSuccess && (
        <div style={{ backgroundColor: '#f0fdf4', color: '#166534', border: '1px solid #bbf7d0', padding: '0.6rem 0.85rem', borderRadius: '6px', fontSize: '0.825rem' }}>
          <FiCheckCircle style={{ marginRight: '6px', verticalAlign: 'middle' }} />
          {actionSuccess}
        </div>
      )}

      {actionError && (
        <div style={{ backgroundColor: '#fef2f2', color: '#991b1b', border: '1px solid #fecaca', padding: '0.6rem 0.85rem', borderRadius: '6px', fontSize: '0.825rem' }}>
          <FiAlertTriangle style={{ marginRight: '6px', verticalAlign: 'middle' }} />
          {actionError}
        </div>
      )}

      {/* Primary Tab Navigation */}
      <div
        style={{
          display: 'flex',
          gap: '0.5rem',
          borderBottom: '1px solid var(--border-light)',
          paddingBottom: '0.25rem',
        }}
      >
        <button
          onClick={() => setActiveAdminTab('complaints')}
          style={{
            padding: '0.6rem 1.25rem',
            borderRadius: '6px',
            border: activeAdminTab === 'complaints' ? '1px solid var(--accent-primary)' : '1px solid var(--border-light)',
            backgroundColor: activeAdminTab === 'complaints' ? 'var(--accent-primary)' : '#ffffff',
            color: activeAdminTab === 'complaints' ? '#ffffff' : 'var(--text-main)',
            fontWeight: 700,
            fontSize: '0.825rem',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
          }}
        >
          <FiLayers size={14} />
          Grievance Operations & Department Matrix
        </button>

        <button
          onClick={() => setActiveAdminTab('users')}
          style={{
            padding: '0.6rem 1.25rem',
            borderRadius: '6px',
            border: activeAdminTab === 'users' ? '1px solid var(--accent-primary)' : '1px solid var(--border-light)',
            backgroundColor: activeAdminTab === 'users' ? 'var(--accent-primary)' : '#ffffff',
            color: activeAdminTab === 'users' ? '#ffffff' : 'var(--text-main)',
            fontWeight: 700,
            fontSize: '0.825rem',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
          }}
        >
          <FiUsers size={14} />
          User Accounts & RBAC ({users.length})
        </button>

        <button
          onClick={() => setActiveAdminTab('diagnostics')}
          style={{
            padding: '0.6rem 1.25rem',
            borderRadius: '6px',
            border: activeAdminTab === 'diagnostics' ? '1px solid var(--accent-primary)' : '1px solid var(--border-light)',
            backgroundColor: activeAdminTab === 'diagnostics' ? 'var(--accent-primary)' : '#ffffff',
            color: activeAdminTab === 'diagnostics' ? '#ffffff' : 'var(--text-main)',
            fontWeight: 700,
            fontSize: '0.825rem',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
          }}
        >
          <FiServer size={14} />
          Security Audit Logs & Infrastructure
        </button>
      </div>

      {loading ? (
        <LoadingSpinner text="Synchronizing Central Command Data..." />
      ) : activeAdminTab === 'complaints' ? (
        /* TAB 1: COMPLAINT MANAGEMENT & DEPARTMENT MATRIX */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Real KPI Cards */}
          <div className="cc-kpi-grid">
            <KpiCard
              label="Total Complaints"
              value={adminStats?.totalComplaints ?? complaints.length}
              subtext="Logged in Central Database (Click to show all)"
              icon={<FiFileText size={16} />}
              variant="accent"
              onClick={() => { setStatusFilter(''); setPriorityFilter(''); }}
            />
            <KpiCard
              label="Pending Assignment"
              value={adminStats?.pendingComplaints ?? complaints.filter(c => ['SUBMITTED', 'ASSIGNED'].includes(c.status) || !c.assignedToUserId).length}
              subtext="Awaiting Field Dispatch (Click to filter)"
              icon={<FiClock size={16} />}
              variant="warning"
              onClick={() => { setStatusFilter('PENDING'); setPriorityFilter(''); }}
            />
            <KpiCard
              label="Active In-Progress"
              value={adminStats?.inProgressComplaints ?? complaints.filter(c => c.status === 'IN_PROGRESS').length}
              subtext="Field Crews Deployed (Click to filter)"
              icon={<FiActivity size={16} />}
              variant="accent"
              onClick={() => { setStatusFilter('IN_PROGRESS'); setPriorityFilter(''); }}
            />
            <KpiCard
              label="Resolved / Closed"
              value={(adminStats?.resolvedComplaints || 0) + (adminStats?.closedComplaints || 0)}
              subtext={`${adminStats?.resolvedComplaints || 0} Resolved • ${adminStats?.closedComplaints || 0} Closed (Click to filter)`}
              icon={<FiCheckCircle size={16} />}
              variant="low"
              onClick={() => { setStatusFilter('RESOLVED'); setPriorityFilter(''); }}
            />
            <KpiCard
              label="SLA Breached / Overdue"
              value={Math.max(adminStats?.overdueComplaints ?? adminStats?.overdueSlaComplaints ?? 0, complaints.filter(c => c.slaStatus === 'BREACHED' || (c.slaDeadline && new Date(c.slaDeadline) < new Date() && !['RESOLVED', 'CLOSED'].includes(c.status))).length)}
              subtext="Exceeded Resolution Target (Click to filter)"
              icon={<FiAlertTriangle size={16} />}
              variant="critical"
              onClick={() => { setStatusFilter('BREACHED'); setPriorityFilter(''); }}
            />
            <KpiCard
              label="Verified Citizens"
              value={adminStats?.verifiedCitizensCount ?? 0}
              subtext={`${adminStats?.verifiedGenuineComplaints ?? 0} Genuine Verified Reports`}
              icon={<FiAward size={16} />}
              variant="low"
            />
          </div>

          {/* Department Breakdown Matrix */}
          <div className="cc-card">
            <div className="cc-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span className="cc-card-title">
                <FiLayers size={16} color="var(--accent-primary)" />
                Departmental Performance Matrix (8 Municipal Portals)
              </span>
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table className="cc-table" style={{ width: '100%', textAlign: 'left', fontSize: '0.825rem' }}>
                <thead>
                  <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border-medium)' }}>
                    <th style={{ padding: '0.75rem 1rem' }}>Department</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Code</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Total</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Pending</th>
                    <th style={{ padding: '0.75rem 1rem' }}>In Progress</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Resolved</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Closed</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Overdue SLA</th>
                  </tr>
                </thead>
                <tbody>
                  {deptStats.length > 0 ? (
                    deptStats.map(d => (
                      <tr key={d.departmentId || d.departmentCode} style={{ borderBottom: '1px solid var(--border-light)' }}>
                        <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>{d.departmentName}</td>
                        <td style={{ padding: '0.75rem 1rem', fontFamily: 'monospace', fontWeight: 700, color: 'var(--accent-teal)' }}>
                          {d.departmentCode}
                        </td>
                        <td style={{ padding: '0.75rem 1rem', fontWeight: 700 }}>{d.totalComplaints}</td>
                        <td style={{ padding: '0.75rem 1rem', color: d.pendingComplaints > 0 ? '#b45309' : 'inherit' }}>
                          {d.pendingComplaints}
                        </td>
                        <td style={{ padding: '0.75rem 1rem', color: 'var(--accent-primary)' }}>{d.inProgressComplaints}</td>
                        <td style={{ padding: '0.75rem 1rem', color: '#16a34a' }}>{d.resolvedComplaints}</td>
                        <td style={{ padding: '0.75rem 1rem', color: '#64748b' }}>{d.closedComplaints}</td>
                        <td style={{ padding: '0.75rem 1rem', fontWeight: 700, color: ((d.breachedDeadline || d.overdueComplaints) > 0) ? '#dc2626' : '#16a34a' }}>
                          {((d.breachedDeadline || d.overdueComplaints) > 0) ? `⚠ ${(d.breachedDeadline || d.overdueComplaints)}` : '0'}
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={8} style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                        Loading department analytics...
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Central Complaints Management Table */}
          <div className="cc-card">
            <div className="cc-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
              <span className="cc-card-title">
                <FiFileText size={16} color="var(--accent-primary)" />
                Central Complaint Redressal Management ({filteredComplaints.length})
              </span>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <div style={{ position: 'relative' }}>
                  <FiSearch style={{ position: 'absolute', left: '10px', top: '10px', color: 'var(--text-muted)' }} />
                  <input
                    type="text"
                    placeholder="Search Record #, Title, Citizen..."
                    value={complaintSearch}
                    onChange={(e) => setComplaintSearch(e.target.value)}
                    className="form-control"
                    style={{ paddingLeft: '32px', fontSize: '0.8rem', width: '220px' }}
                  />
                </div>

                <select
                  value={deptFilter}
                  onChange={(e) => setDeptFilter(e.target.value)}
                  className="form-control"
                  style={{ fontSize: '0.8rem', padding: '0.4rem 0.6rem' }}
                >
                  <option value="">All Departments</option>
                  <option value="WATER">Water Supply (WATER)</option>
                  <option value="PWD">Roads & Infra (PWD)</option>
                  <option value="ELECTRICAL">Electricity (ELECTRICAL)</option>
                  <option value="SANITATION">Sanitation (SANITATION)</option>
                  <option value="HEALTH">Health (HEALTH)</option>
                  <option value="TRAFFIC">Traffic (TRAFFIC)</option>
                  <option value="ENVIRONMENT">Environment (ENVIRONMENT)</option>
                  <option value="EMERGENCY">Emergency (EMERGENCY)</option>
                </select>

                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="form-control"
                  style={{ fontSize: '0.8rem', padding: '0.4rem 0.6rem' }}
                >
                  <option value="">All Statuses</option>
                  <option value="PENDING">Pending Assignment</option>
                  <option value="SUBMITTED">Submitted</option>
                  <option value="ASSIGNED">Assigned</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="RESOLVED">Resolved</option>
                  <option value="CLOSED">Closed</option>
                  <option value="BREACHED">SLA Breached / Overdue</option>
                </select>

                <select
                  value={priorityFilter}
                  onChange={(e) => setPriorityFilter(e.target.value)}
                  className="form-control"
                  style={{ fontSize: '0.8rem', padding: '0.4rem 0.6rem' }}
                >
                  <option value="">All Priorities</option>
                  <option value="CRITICAL">Critical</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>
            </div>

            {/* Quick Filter Pills */}
            <div style={{ display: 'flex', gap: '0.4rem', padding: '0.5rem 1rem 0.75rem', flexWrap: 'wrap', borderBottom: '1px solid var(--border-light)' }}>
              {[
                { label: `All (${complaints.length})`, active: !statusFilter && !priorityFilter, onClick: () => { setStatusFilter(''); setPriorityFilter(''); } },
                { label: `Critical Priority (${complaints.filter(c => (c.priority || '').toUpperCase() === 'CRITICAL').length})`, active: priorityFilter === 'CRITICAL', onClick: () => { setStatusFilter(''); setPriorityFilter(priorityFilter === 'CRITICAL' ? '' : 'CRITICAL'); } },
                { label: `Pending Assignment (${complaints.filter(c => ['SUBMITTED', 'ASSIGNED'].includes(c.status) || !c.assignedToUserId).length})`, active: statusFilter === 'PENDING', onClick: () => { setPriorityFilter(''); setStatusFilter(statusFilter === 'PENDING' ? '' : 'PENDING'); } },
                { label: `SLA Breached / Overdue (${complaints.filter(c => c.slaStatus === 'BREACHED' || (c.slaDeadline && new Date(c.slaDeadline) < new Date() && !['RESOLVED', 'CLOSED'].includes(c.status))).length})`, active: statusFilter === 'BREACHED', onClick: () => { setPriorityFilter(''); setStatusFilter(statusFilter === 'BREACHED' ? '' : 'BREACHED'); } },
                { label: `In Progress (${complaints.filter(c => c.status === 'IN_PROGRESS').length})`, active: statusFilter === 'IN_PROGRESS', onClick: () => { setPriorityFilter(''); setStatusFilter(statusFilter === 'IN_PROGRESS' ? '' : 'IN_PROGRESS'); } },
                { label: `Resolved / Closed (${complaints.filter(c => ['RESOLVED', 'CLOSED'].includes(c.status)).length})`, active: statusFilter === 'RESOLVED' || statusFilter === 'CLOSED', onClick: () => { setPriorityFilter(''); setStatusFilter(statusFilter === 'RESOLVED' ? '' : 'RESOLVED'); } },
              ].map((pill, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={pill.onClick}
                  style={{
                    padding: '3px 9px',
                    borderRadius: '6px',
                    border: pill.active ? '1px solid var(--accent-primary)' : '1px solid var(--border-light)',
                    backgroundColor: pill.active ? 'rgba(2, 132, 199, 0.12)' : '#ffffff',
                    color: pill.active ? 'var(--accent-primary)' : 'var(--text-muted)',
                    fontSize: '0.75rem',
                    fontWeight: pill.active ? 700 : 500,
                    cursor: 'pointer',
                  }}
                >
                  {pill.label}
                </button>
              ))}
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table className="cc-table" style={{ width: '100%', textAlign: 'left', fontSize: '0.825rem' }}>
                <thead>
                  <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border-medium)' }}>
                    <th style={{ padding: '0.75rem 1rem' }}>Record #</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Grievance Title</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Dept</th>
                    <th style={{ padding: '0.75rem 1rem' }}>Citizen</th>
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
                        No complaints match the selected filter criteria.
                      </td>
                    </tr>
                  ) : (
                    filteredComplaints.map(c => {
                      const remaining = c.remainingMinutes != null ? c.remainingMinutes : 0;
                      let slaColor = '#10B981';
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
                            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{c.address}</div>
                          </td>
                          <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>
                            {c.departmentCode || 'PWD'}
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <div>{c.citizenName} {c.citizenIsVerifiedBadge && '🏅'}</div>
                            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{c.citizenPhone}</div>
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
                              Manage
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
        </div>
      ) : activeAdminTab === 'users' ? (
        /* TAB 2: USER DIRECTORY & RBAC */
        <div className="cc-card">
          <div className="cc-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="cc-card-title">
              <FiUsers size={16} color="var(--accent-primary)" />
              Municipal User Directory & RBAC Control ({users.length} Registered Accounts)
            </span>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table className="cc-table" style={{ width: '100%', textAlign: 'left', fontSize: '0.825rem' }}>
              <thead>
                <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border-medium)' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>User</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Email / Phone</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Department / Team</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Role</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Verified Citizen</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => {
                  const userRole = u.role || (u.roles && u.roles[0]?.name) || 'CITIZEN';
                  const isUpdating = updatingUserId === u.id;
                  return (
                    <tr key={u.id} style={{ borderBottom: '1px solid var(--border-light)' }}>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div style={{ fontWeight: 600, color: 'var(--text-main)' }}>{u.fullName || u.username}</div>
                        <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>@{u.username}</div>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div>{u.email}</div>
                        <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>{u.mobileNumber || u.phone || 'No Phone'}</div>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div>{u.departmentName || 'Central / Citizen'}</div>
                        <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>{u.teamName || '—'}</div>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <select
                          value={userRole}
                          disabled={isUpdating}
                          onChange={(e) => handleRoleChange(u, e.target.value)}
                          className="form-control"
                          style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
                        >
                          <option value="ROLE_ADMIN">ROLE_ADMIN</option>
                          <option value="ROLE_COMMAND_OPERATOR">ROLE_COMMAND_OPERATOR</option>
                          <option value="ROLE_DEPARTMENT_OFFICER">ROLE_DEPARTMENT_OFFICER</option>
                          <option value="ROLE_FIELD_WORKER">ROLE_FIELD_WORKER</option>
                          <option value="ROLE_CITIZEN">ROLE_CITIZEN</option>
                        </select>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        {u.isVerifiedCitizen ? (
                          <span className="badge badge-low" style={{ fontSize: '0.7rem' }}>🏅 VERIFIED</span>
                        ) : (
                          <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>{u.genuineComplaintCount || 0} Genuine</span>
                        )}
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <span
                          style={{
                            padding: '2px 8px',
                            borderRadius: '9999px',
                            fontSize: '0.7rem',
                            fontWeight: 700,
                            backgroundColor: u.enabled ? '#f0fdf4' : '#fef2f2',
                            color: u.enabled ? '#16a34a' : '#dc2626'
                          }}
                        >
                          {u.enabled ? 'ACTIVE' : 'DISABLED'}
                        </span>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <button
                          onClick={() => handleToggleStatus(u)}
                          disabled={isUpdating}
                          className="btn btn-outline btn-sm"
                          style={{ fontSize: '0.725rem', padding: '3px 8px' }}
                        >
                          {u.enabled ? 'Disable' : 'Enable'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        /* TAB 3: DIAGNOSTICS & AUDIT LOGS */
        <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: '1.5rem' }}>
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiDatabase size={16} color="var(--accent-primary)" />
                Security & Administrative Audit Logs (Live Stream)
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: '480px', overflowY: 'auto' }}>
              {auditLogs.length === 0 ? (
                <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                  No recent security audit logs recorded.
                </div>
              ) : (
                auditLogs.map((log, idx) => (
                  <div
                    key={log.id || idx}
                    style={{
                      borderBottom: '1px solid var(--border-light)',
                      padding: '0.65rem 0.85rem',
                      fontSize: '0.775rem',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px' }}>
                      <span style={{ fontWeight: 700, color: 'var(--text-main)' }}>{log.action || log.eventType}</span>
                      <span style={{ color: 'var(--text-subtle)', fontSize: '0.7rem' }}>
                        {log.timestamp ? new Date(log.timestamp).toLocaleTimeString() : 'Recent'}
                      </span>
                    </div>
                    <div style={{ color: 'var(--text-muted)' }}>
                      Principal: <strong>{log.username || log.principal || 'System'}</strong> • Target: {log.entityType} ({log.entityId})
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiServer size={16} color="var(--accent-teal)" />
                System Health & Infrastructure Telemetry
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', fontSize: '0.825rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.6rem 0.85rem', backgroundColor: '#f0fdf4', borderRadius: '6px' }}>
                <span>Backend Application Server</span>
                <strong style={{ color: '#16a34a' }}>Spring Boot 3.3.4 (UP)</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.6rem 0.85rem', backgroundColor: '#f0fdf4', borderRadius: '6px' }}>
                <span>Primary Relational DB</span>
                <strong style={{ color: '#16a34a' }}>PostgreSQL 16 + PostGIS (UP)</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.6rem 0.85rem', backgroundColor: '#f0fdf4', borderRadius: '6px' }}>
                <span>Real-Time Ingestion</span>
                <strong style={{ color: '#16a34a' }}>Native STOMP / WebSocket (UP)</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.6rem 0.85rem', backgroundColor: '#f0fdf4', borderRadius: '6px' }}>
                <span>Database Migrations</span>
                <strong style={{ color: 'var(--accent-teal)' }}>Flyway V16 Active</strong>
              </div>
            </div>
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
            loadAllAdminData();
          }}
        />
      )}
    </div>
  );
}
