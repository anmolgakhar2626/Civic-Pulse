import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';
import { slaApi } from '../api/slaApi';
import PriorityBadge from '../components/PriorityBadge';
import SlaTimer from '../components/SlaTimer';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';
import {
  FiClock,
  FiAlertOctagon,
  FiAlertTriangle,
  FiCheckCircle,
  FiRefreshCw,
  FiArrowRight,
  FiSliders,
  FiPlus,
  FiSearch,
  FiFilter,
  FiX,
  FiLayers,
  FiShield,
  FiActivity,
  FiInfo
} from 'react-icons/fi';

export const SlaBoardPage = () => {
  const { user, hasRole } = useAuth();
  const isAdmin = hasRole(['ADMIN']);
  const { latestSlaEvent } = useWebSocket();

  // Primary data states
  const [slas, setSlas] = useState([]);
  const [overview, setOverview] = useState(null);
  const [policies, setPolicies] = useState([]);
  const [escalations, setEscalations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Active top-level tab: 'RECORDS' | 'ESCALATIONS' | 'POLICIES'
  const [viewTab, setViewTab] = useState('RECORDS');

  // Filter states
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [selectedEscalation, setSelectedEscalation] = useState('ALL');

  // Modals
  const [selectedSlaDetail, setSelectedSlaDetail] = useState(null);
  const [showPolicyModal, setShowPolicyModal] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState(null);
  const [policyForm, setPolicyForm] = useState({
    category: 'Flood',
    severity: 'CRITICAL',
    responseTimeMinutes: 15,
    resolutionTimeMinutes: 60,
    enabled: true
  });
  const [policySubmitting, setPolicySubmitting] = useState(false);
  const [policyError, setPolicyError] = useState('');

  // Fetch all SLA dashboard data
  const loadDashboardData = async () => {
    try {
      setRefreshing(true);
      const [recordsRes, breachedRes, overviewRes, policiesRes, escalationsRes] = await Promise.all([
        slaApi.getActiveSlas({ size: 100 }).catch(() => []),
        slaApi.getBreachedSlas({ size: 100 }).catch(() => []),
        slaApi.getOverview().catch(() => null),
        slaApi.getPolicies().catch(() => []),
        slaApi.getEscalations(50).catch(() => [])
      ]);

      const rawActive = recordsRes.content || recordsRes.data || (Array.isArray(recordsRes) ? recordsRes : []);
      const rawBreached = breachedRes.content || breachedRes.data || (Array.isArray(breachedRes) ? breachedRes : []);

      const recordMap = new Map();
      rawActive.forEach((r) => recordMap.set(r.id || r.incidentId, r));
      rawBreached.forEach((r) => {
        const key = r.id || r.incidentId;
        const existing = recordMap.get(key) || {};
        recordMap.set(key, { ...existing, ...r, status: 'BREACHED', currentStatus: 'BREACHED', breached: true });
      });

      const rawRecords = Array.from(recordMap.values());
      setSlas(rawRecords);

      if (overviewRes) {
        setOverview(overviewRes.data || overviewRes);
      }
      setPolicies(Array.isArray(policiesRes) ? policiesRes : (policiesRes.data || []));
      setEscalations(Array.isArray(escalationsRes) ? escalationsRes : (escalationsRes.data || []));
    } catch (err) {
      console.error('Failed to load SLA dashboard data', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  // Real-time WebSocket updates
  const processedBreachedIncidentsRef = useRef(new Set());

  useEffect(() => {
    if (latestSlaEvent) {
      const isBreach = (latestSlaEvent.slaStatus || '').toUpperCase() === 'BREACHED';
      const eventIncId = latestSlaEvent.incidentId || latestSlaEvent.slaRecordId;

      setSlas((prev) => {
        const idx = prev.findIndex((s) => s.incidentId === latestSlaEvent.incidentId || s.id === latestSlaEvent.slaRecordId);
        if (idx >= 0) {
          const updated = [...prev];
          updated[idx] = {
            ...updated[idx],
            status: latestSlaEvent.slaStatus || updated[idx].status,
            currentStatus: latestSlaEvent.slaStatus || updated[idx].currentStatus,
            remainingMinutes: latestSlaEvent.remainingMinutes != null ? latestSlaEvent.remainingMinutes : updated[idx].remainingMinutes,
            escalationLevel: latestSlaEvent.escalationLevel || updated[idx].escalationLevel,
            breached: isBreach || updated[idx].breached,
          };
          return updated;
        } else if (latestSlaEvent.slaStatus) {
          return [
            {
              id: latestSlaEvent.slaRecordId || latestSlaEvent.id || ('sla-' + Date.now()),
              incidentId: latestSlaEvent.incidentId,
              incidentNumber: latestSlaEvent.incidentNumber || 'INC-LIVE',
              status: latestSlaEvent.slaStatus,
              currentStatus: latestSlaEvent.slaStatus,
              remainingMinutes: latestSlaEvent.remainingMinutes ?? 0,
              escalationLevel: latestSlaEvent.escalationLevel || 'NONE',
              breached: isBreach,
            },
            ...prev,
          ];
        }
        return prev;
      });

      if (isBreach && eventIncId && !processedBreachedIncidentsRef.current.has(eventIncId)) {
        processedBreachedIncidentsRef.current.add(eventIncId);
        setOverview((prev) => {
          if (!prev) return prev;
          return {
            ...prev,
            breachedCount: (prev.breachedCount || 0) + 1,
            totalActive: Math.max((prev.totalActive || 0), 1),
          };
        });
      }
    }
  }, [latestSlaEvent]);

  // Compute metric counts
  const activeRecords = useMemo(() => {
    return slas.filter(s => {
      const st = (s.status || s.currentStatus || '').toUpperCase();
      return st !== 'COMPLETED' && st !== 'RESOLVED';
    });
  }, [slas]);

  const computedBreachedCount = useMemo(() => {
    return activeRecords.filter(s => {
      const st = (s.status || s.currentStatus || '').toUpperCase();
      return st === 'BREACHED' || s.breached === true;
    }).length;
  }, [activeRecords]);

  const computedWarningCount = useMemo(() => {
    return activeRecords.filter(s => {
      const st = (s.status || s.currentStatus || '').toUpperCase();
      return (st === 'WARNING' || st === 'AT_RISK') && st !== 'BREACHED' && !s.breached;
    }).length;
  }, [activeRecords]);

  const computedOnTrackCount = useMemo(() => {
    return activeRecords.filter(s => {
      const st = (s.status || s.currentStatus || '').toUpperCase();
      return st === 'ON_TRACK' && !s.breached;
    }).length;
  }, [activeRecords]);

  const computedCompletedCount = useMemo(() => {
    return slas.filter(s => {
      const st = (s.status || s.currentStatus || '').toUpperCase();
      return st === 'COMPLETED' || st === 'RESOLVED';
    }).length;
  }, [slas]);

  const totalActive = Math.max(overview?.totalActive ?? 0, activeRecords.length);
  const onTrackCount = overview?.onTrackCount ?? computedOnTrackCount;
  const warningCount = Math.max(overview?.warningCount ?? 0, computedWarningCount);
  const breachedCount = Math.max(overview?.breachedCount ?? 0, computedBreachedCount);
  const completedCount = Math.max(overview?.completedCount ?? 0, computedCompletedCount);

  // Filtered SLA records
  const filteredRecords = useMemo(() => {
    return slas.filter((item) => {
      // Search filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const num = (item.incidentNumber || item.incidentId || '').toLowerCase();
        const title = (item.incidentTitle || '').toLowerCase();
        const cat = (item.category || '').toLowerCase();
        if (!num.includes(q) && !title.includes(q) && !cat.includes(q)) return false;
      }

      // Status filter
      const normStatus = (item.status || item.currentStatus || 'ON_TRACK').toUpperCase();
      const isItemBreached = normStatus === 'BREACHED' || item.breached === true || (item.remainingMinutes != null && item.remainingMinutes < 0);
      if (selectedStatus !== 'ALL') {
        if (selectedStatus === 'WARNING' && normStatus !== 'WARNING' && normStatus !== 'AT_RISK') return false;
        if (selectedStatus === 'COMPLETED' && normStatus !== 'COMPLETED' && normStatus !== 'RESOLVED') return false;
        if (selectedStatus === 'BREACHED' && !isItemBreached) return false;
        if (selectedStatus !== 'WARNING' && selectedStatus !== 'COMPLETED' && selectedStatus !== 'BREACHED' && normStatus !== selectedStatus) return false;
      }

      // Category filter
      if (selectedCategory !== 'ALL' && item.category && item.category.toLowerCase() !== selectedCategory.toLowerCase()) {
        return false;
      }

      // Severity filter
      if (selectedSeverity !== 'ALL') {
        const sev = (item.severity || (item.priorityTier ? item.priorityTier : '')).toUpperCase();
        if (sev !== selectedSeverity) return false;
      }

      // Escalation level filter
      if (selectedEscalation !== 'ALL') {
        const esc = (item.escalationLevel || 'NONE').toUpperCase();
        if (esc !== selectedEscalation) return false;
      }

      return true;
    });
  }, [slas, searchQuery, selectedStatus, selectedCategory, selectedSeverity, selectedEscalation]);

  // Policy Form Submission (Admin)
  const handleSavePolicy = async (e) => {
    e.preventDefault();
    setPolicyError('');
    setPolicySubmitting(true);
    try {
      if (editingPolicy) {
        await slaApi.updatePolicy(editingPolicy.id, policyForm);
      } else {
        await slaApi.createPolicy(policyForm);
      }
      setShowPolicyModal(false);
      setEditingPolicy(null);
      // Reload policies
      const refreshed = await slaApi.getPolicies();
      setPolicies(Array.isArray(refreshed) ? refreshed : (refreshed.data || []));
    } catch (err) {
      setPolicyError(err.response?.data?.message || 'Failed to save SLA Policy');
    } finally {
      setPolicySubmitting(false);
    }
  };

  const openCreatePolicyModal = () => {
    setEditingPolicy(null);
    setPolicyForm({
      category: 'Flood',
      severity: 'CRITICAL',
      responseTimeMinutes: 15,
      resolutionTimeMinutes: 60,
      enabled: true
    });
    setPolicyError('');
    setShowPolicyModal(true);
  };

  const openEditPolicyModal = (policy) => {
    setEditingPolicy(policy);
    setPolicyForm({
      category: policy.category,
      severity: policy.severity,
      responseTimeMinutes: policy.responseTimeMinutes,
      resolutionTimeMinutes: policy.resolutionTimeMinutes,
      enabled: policy.enabled
    });
    setPolicyError('');
    setShowPolicyModal(true);
  };

  const formatRemainingMinutes = (mins) => {
    if (mins == null) return '--';
    if (mins < 0) {
      const overdue = Math.abs(mins);
      const h = Math.floor(overdue / 60);
      const m = overdue % 60;
      return h > 0 ? `+${h}h ${m}m Overdue` : `+${m}m Overdue`;
    }
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return h > 0 ? `${h}h ${m}m left` : `${m}m left`;
  };

  return (
    <div>
      {/* Page Header */}
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <h1>SLA Compliance & Automated Escalation</h1>
          <p>Real-time response & resolution deadline monitoring, multi-tier escalation, and compliance audit trail</p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          {isAdmin && (
            <button onClick={openCreatePolicyModal} className="btn btn-primary btn-sm">
              <FiPlus size={14} /> New Policy
            </button>
          )}
          <button onClick={loadDashboardData} disabled={refreshing} className="btn btn-outline btn-sm">
            <FiRefreshCw size={13} className={refreshing ? 'spin' : ''} />
            {refreshing ? 'Syncing...' : 'Refresh Board'}
          </button>
        </div>
      </div>

      {/* KPI Metric Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1.25rem' }}>
        {/* Total Active */}
        <div className="cc-card cc-kpi-card" onClick={() => { setViewTab('RECORDS'); setSelectedStatus('ALL'); }} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>ACTIVE SLAS</span>
            <FiActivity size={16} color="var(--color-primary)" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.4rem 0 0.1rem', color: 'var(--text-main)' }}>
            {totalActive}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Under live monitoring</div>
        </div>

        {/* On Track */}
        <div className="cc-card cc-kpi-card" onClick={() => { setViewTab('RECORDS'); setSelectedStatus('ON_TRACK'); }} style={{ cursor: 'pointer', borderLeft: '4px solid #16a34a' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: '#16a34a', fontWeight: 600 }}>ON TRACK</span>
            <FiCheckCircle size={16} color="#16a34a" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.4rem 0 0.1rem', color: '#16a34a' }}>
            {onTrackCount}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Within target limits</div>
        </div>

        {/* Warning / Approaching */}
        <div className="cc-card cc-kpi-card" onClick={() => { setViewTab('RECORDS'); setSelectedStatus('WARNING'); }} style={{ cursor: 'pointer', borderLeft: '4px solid #d97706' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: '#d97706', fontWeight: 600 }}>WARNING</span>
            <FiAlertTriangle size={16} color="#d97706" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.4rem 0 0.1rem', color: '#d97706' }}>
            {warningCount}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Deadline approaching (&le; 20%)</div>
        </div>

        {/* Breached */}
        <div className="cc-card cc-kpi-card" onClick={() => { setViewTab('RECORDS'); setSelectedStatus('BREACHED'); }} style={{ cursor: 'pointer', borderLeft: '4px solid #dc2626' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: '#dc2626', fontWeight: 600 }}>BREACHED</span>
            <FiAlertOctagon size={16} color="#dc2626" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.4rem 0 0.1rem', color: '#dc2626' }}>
            {breachedCount}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Target deadline passed</div>
        </div>

        {/* Completed */}
        <div className="cc-card cc-kpi-card" onClick={() => { setViewTab('RECORDS'); setSelectedStatus('COMPLETED'); }} style={{ cursor: 'pointer', borderLeft: '4px solid #0284c7' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: '#0284c7', fontWeight: 600 }}>COMPLETED</span>
            <FiCheckCircle size={16} color="#0284c7" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '0.4rem 0 0.1rem', color: '#0284c7' }}>
            {completedCount}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Incidents resolved</div>
        </div>
      </div>

      {/* Main Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
        <button
          onClick={() => setViewTab('RECORDS')}
          className={`btn btn-sm ${viewTab === 'RECORDS' ? 'btn-primary' : 'btn-outline'}`}
        >
          <FiClock size={13} /> Active SLA Compliance ({slas.length})
        </button>

        <button
          onClick={() => setViewTab('ESCALATIONS')}
          className={`btn btn-sm ${viewTab === 'ESCALATIONS' ? 'btn-primary' : 'btn-outline'}`}
        >
          <FiShield size={13} /> Escalation Audit Log ({escalations.length})
        </button>

        <button
          onClick={() => setViewTab('POLICIES')}
          className={`btn btn-sm ${viewTab === 'POLICIES' ? 'btn-primary' : 'btn-outline'}`}
        >
          <FiSliders size={13} /> SLA Policy Rules ({policies.length})
        </button>
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: LIVE SLA RECORDS TABLE & FILTERS                                   */}
      {/* ========================================================================= */}
      {viewTab === 'RECORDS' && (
        <>
          {/* Filter Bar */}
          <div className="cc-card" style={{ marginBottom: '1.25rem', padding: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '0.85rem', alignItems: 'center' }}>
              {/* Search */}
              <div style={{ position: 'relative' }}>
                <FiSearch style={{ position: 'absolute', left: 10, top: 10, color: 'var(--text-muted)' }} size={14} />
                <input
                  type="text"
                  placeholder="Search incident # or title..."
                  className="form-control"
                  style={{ paddingLeft: '2rem', height: 34, fontSize: '0.8rem' }}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>

              {/* Status */}
              <div>
                <select
                  className="form-control"
                  style={{ height: 34, fontSize: '0.8rem' }}
                  value={selectedStatus}
                  onChange={(e) => setSelectedStatus(e.target.value)}
                >
                  <option value="ALL">All Statuses</option>
                  <option value="ON_TRACK">On Track</option>
                  <option value="WARNING">Warning / At Risk</option>
                  <option value="BREACHED">Breached</option>
                  <option value="COMPLETED">Completed</option>
                </select>
              </div>

              {/* Category */}
              <div>
                <select
                  className="form-control"
                  style={{ height: 34, fontSize: '0.8rem' }}
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                >
                  <option value="ALL">All Categories</option>
                  <option value="Flood">Flood</option>
                  <option value="Fire">Fire</option>
                  <option value="Traffic">Traffic</option>
                  <option value="Pollution">Pollution</option>
                  <option value="Infrastructure">Infrastructure</option>
                </select>
              </div>

              {/* Severity */}
              <div>
                <select
                  className="form-control"
                  style={{ height: 34, fontSize: '0.8rem' }}
                  value={selectedSeverity}
                  onChange={(e) => setSelectedSeverity(e.target.value)}
                >
                  <option value="ALL">All Severities</option>
                  <option value="CRITICAL">Critical</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </div>

              {/* Escalation Level */}
              <div>
                <select
                  className="form-control"
                  style={{ height: 34, fontSize: '0.8rem' }}
                  value={selectedEscalation}
                  onChange={(e) => setSelectedEscalation(e.target.value)}
                >
                  <option value="ALL">All Escalations</option>
                  <option value="NONE">Level: None</option>
                  <option value="LEVEL_1">Level 1 (Supervisor)</option>
                  <option value="LEVEL_2">Level 2 (Dept Head)</option>
                  <option value="LEVEL_3">Level 3 (City Director)</option>
                </select>
              </div>

              {/* Reset Filters */}
              {(searchQuery || selectedStatus !== 'ALL' || selectedCategory !== 'ALL' || selectedSeverity !== 'ALL' || selectedEscalation !== 'ALL') && (
                <button
                  onClick={() => {
                    setSearchQuery('');
                    setSelectedStatus('ALL');
                    setSelectedCategory('ALL');
                    setSelectedSeverity('ALL');
                    setSelectedEscalation('ALL');
                  }}
                  className="btn btn-outline btn-sm"
                  style={{ height: 34 }}
                >
                  <FiX size={13} /> Reset Filters
                </button>
              )}
            </div>
          </div>

          {/* Table */}
          <div className="cc-card" style={{ padding: 0 }}>
            {loading ? (
              <LoadingSpinner text="Computing live SLA compliance metrics..." />
            ) : filteredRecords.length === 0 ? (
              <EmptyState
                title="No SLA records match criteria"
                description="Adjust search terms or filters to view incident SLA compliance records."
              />
            ) : (
              <div className="cc-table-wrapper" style={{ border: 'none' }}>
                <table className="cc-table">
                  <thead>
                    <tr>
                      <th>Incident</th>
                      <th>Category & Severity</th>
                      <th>SLA Status</th>
                      <th>First Response</th>
                      <th>Resolution Deadline</th>
                      <th>Remaining Time</th>
                      <th>Escalation</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredRecords.map((item) => {
                      const statusNorm = (item.status || item.currentStatus || 'ON_TRACK').toUpperCase();
                      const escLevel = item.escalationLevel || 'NONE';
                      const escBadgeClass = escLevel === 'LEVEL_3' ? 'badge-critical' : escLevel === 'LEVEL_2' ? 'badge-high' : escLevel === 'LEVEL_1' ? 'badge-medium' : 'badge-status';

                      const hasResponded = item.firstRespondedAt || item.acknowledgedAt;

                      return (
                        <tr key={item.id || item.incidentId}>
                          <td>
                            <div style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                              {item.incidentNumber || (item.incidentId ? item.incidentId.substring(0, 8) : 'N/A')}
                            </div>
                            {item.incidentTitle && (
                              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', maxWidth: 220, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                {item.incidentTitle}
                              </div>
                            )}
                          </td>

                          <td>
                            <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                              <span style={{ fontSize: '0.75rem', fontWeight: 600 }}>{item.category || 'General'}</span>
                              <PriorityBadge tier={item.priorityTier || item.severity} />
                            </div>
                          </td>

                          <td>
                            <SlaTimer
                              status={statusNorm}
                              remainingMinutes={item.remainingMinutes}
                              targetMinutes={item.targetMinutes}
                              deadlineAt={item.resolutionDeadline || item.deadlineAt}
                            />
                          </td>

                          <td>
                            {hasResponded ? (
                              <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: '#16a34a', fontSize: '0.75rem', fontWeight: 600 }}>
                                <FiCheckCircle size={13} />
                                <span>Responded</span>
                              </div>
                            ) : item.responseDeadline ? (
                              <div style={{ fontSize: '0.75rem' }}>
                                <span style={{ color: new Date(item.responseDeadline) < new Date() ? '#dc2626' : '#d97706', fontWeight: 600 }}>
                                  {new Date(item.responseDeadline) < new Date() ? 'Response Overdue' : 'Pending Response'}
                                </span>
                                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                                  Due: {new Date(item.responseDeadline).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </div>
                              </div>
                            ) : (
                              <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>--</span>
                            )}
                          </td>

                          <td>
                            <div style={{ fontSize: '0.775rem' }}>
                              {item.resolutionDeadline || item.deadlineAt ? (
                                <>
                                  <div style={{ fontWeight: 600 }}>
                                    {new Date(item.resolutionDeadline || item.deadlineAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                  </div>
                                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                                    {new Date(item.resolutionDeadline || item.deadlineAt).toLocaleDateString([], { month: 'short', day: 'numeric' })}
                                  </div>
                                </>
                              ) : '--'}
                            </div>
                          </td>

                          <td>
                            <span style={{
                              fontWeight: 700,
                              fontSize: '0.8rem',
                              color: item.remainingMinutes < 0 ? '#dc2626' : (item.remainingMinutes <= 30 ? '#d97706' : 'var(--text-main)')
                            }}>
                              {formatRemainingMinutes(item.remainingMinutes)}
                            </span>
                          </td>

                          <td>
                            <span className={`badge ${escBadgeClass}`} style={{ fontSize: '0.675rem', letterSpacing: '0.03em' }}>
                              {escLevel.replace('_', ' ')}
                            </span>
                          </td>

                          <td style={{ textAlign: 'right' }}>
                            <div style={{ display: 'inline-flex', gap: '0.35rem' }}>
                              <button
                                onClick={() => setSelectedSlaDetail(item)}
                                className="btn btn-outline btn-sm"
                                style={{ padding: '2px 8px', fontSize: '0.725rem' }}
                                title="View SLA Audit & Deadlines"
                              >
                                <FiInfo size={12} /> Details
                              </button>
                              {(item.incidentUuid || item.incidentId) && (
                                <Link
                                  to={`/incidents/${item.incidentUuid || item.incidentId}`}
                                  className="btn btn-outline btn-sm"
                                  style={{ padding: '2px 8px', fontSize: '0.725rem' }}
                                >
                                  Dossier <FiArrowRight size={11} />
                                </Link>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: ESCALATION AUDIT HISTORY TABLE                                     */}
      {/* ========================================================================= */}
      {viewTab === 'ESCALATIONS' && (
        <div className="cc-card" style={{ padding: 0 }}>
          <div className="cc-card-header" style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border-color)' }}>
            <div>
              <h3 className="cc-card-title" style={{ margin: 0, fontSize: '0.95rem' }}>Automated SLA Escalation Audit Trail</h3>
              <p style={{ margin: 0, fontSize: '0.775rem', color: 'var(--text-muted)' }}>
                Immutable event history recording deadline breaches and progressive supervisor escalations
              </p>
            </div>
          </div>

          {escalations.length === 0 ? (
            <EmptyState
              title="No SLA Escalation Events"
              description="No automated escalations have been triggered yet. Active SLAs remain compliant."
            />
          ) : (
            <div className="cc-table-wrapper" style={{ border: 'none' }}>
              <table className="cc-table">
                <thead>
                  <tr>
                    <th>Escalation Time</th>
                    <th>Incident</th>
                    <th>Escalation Transition</th>
                    <th>Status Shift</th>
                    <th>Trigger Reason</th>
                    <th style={{ textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {escalations.map((esc) => {
                    const levelClass = esc.newEscalationLevel === 'LEVEL_3' ? 'badge-critical' : esc.newEscalationLevel === 'LEVEL_2' ? 'badge-high' : 'badge-medium';
                    return (
                      <tr key={esc.id}>
                        <td style={{ fontSize: '0.775rem', whiteSpace: 'nowrap' }}>
                          <div style={{ fontWeight: 600 }}>
                            {esc.escalatedAt ? new Date(esc.escalatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '--'}
                          </div>
                          <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                            {esc.escalatedAt ? new Date(esc.escalatedAt).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' }) : '--'}
                          </div>
                        </td>

                        <td>
                          <div style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                            {esc.incidentNumber || (esc.incidentId ? esc.incidentId.substring(0, 8) : 'Incident')}
                          </div>
                          {esc.incidentTitle && (
                            <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', maxWidth: 200, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {esc.incidentTitle}
                            </div>
                          )}
                        </td>

                        <td>
                          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                            <span className="badge badge-status" style={{ fontSize: '0.675rem' }}>{esc.previousEscalationLevel || 'NONE'}</span>
                            <FiArrowRight size={11} color="var(--text-muted)" />
                            <span className={`badge ${levelClass}`} style={{ fontSize: '0.675rem' }}>{esc.newEscalationLevel}</span>
                          </div>
                        </td>

                        <td>
                          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem' }}>
                            <span style={{ color: 'var(--text-muted)' }}>{esc.previousStatus || 'ON_TRACK'}</span>
                            <FiArrowRight size={11} color="var(--text-muted)" />
                            <strong style={{ color: esc.newStatus === 'BREACHED' ? '#dc2626' : '#d97706' }}>{esc.newStatus}</strong>
                          </div>
                        </td>

                        <td style={{ fontSize: '0.775rem', color: 'var(--text-muted)' }}>
                          {esc.reason || 'SLA deadline threshold triggered automated escalation'}
                        </td>

                        <td style={{ textAlign: 'right' }}>
                          {esc.incidentId && (
                            <Link
                              to={`/incidents/${esc.incidentId}`}
                              className="btn btn-outline btn-sm"
                              style={{ padding: '2px 8px', fontSize: '0.725rem' }}
                            >
                              Dossier <FiArrowRight size={11} />
                            </Link>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: SLA POLICY RULES MANAGEMENT                                       */}
      {/* ========================================================================= */}
      {viewTab === 'POLICIES' && (
        <div className="cc-card" style={{ padding: 0 }}>
          <div className="cc-card-header" style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h3 className="cc-card-title" style={{ margin: 0, fontSize: '0.95rem' }}>SLA Policies & Time Limits</h3>
              <p style={{ margin: 0, fontSize: '0.775rem', color: 'var(--text-muted)' }}>
                Target first-response and full resolution thresholds mapped by Incident Category and Severity
              </p>
            </div>
            {isAdmin && (
              <button onClick={openCreatePolicyModal} className="btn btn-primary btn-sm">
                <FiPlus size={14} /> Add Policy Rule
              </button>
            )}
          </div>

          <div className="cc-table-wrapper" style={{ border: 'none' }}>
            <table className="cc-table">
              <thead>
                <tr>
                  <th>Category</th>
                  <th>Severity Level</th>
                  <th>First-Response Limit</th>
                  <th>Resolution Limit</th>
                  <th>Status</th>
                  <th>Last Updated</th>
                  {isAdmin && <th style={{ textAlign: 'right' }}>Action</th>}
                </tr>
              </thead>
              <tbody>
                {policies.map((pol) => (
                  <tr key={pol.id}>
                    <td style={{ fontWeight: 600 }}>{pol.category}</td>
                    <td>
                      <PriorityBadge tier={pol.severity} />
                    </td>
                    <td>
                      <strong style={{ color: 'var(--color-primary)' }}>{pol.responseTimeMinutes} mins</strong>
                      <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginLeft: 6 }}>
                        ({(pol.responseTimeMinutes / 60).toFixed(1)} hrs)
                      </span>
                    </td>
                    <td>
                      <strong style={{ color: '#0f172a' }}>{pol.resolutionTimeMinutes} mins</strong>
                      <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginLeft: 6 }}>
                        ({(pol.resolutionTimeMinutes / 60).toFixed(1)} hrs)
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${pol.enabled ? 'badge-low' : 'badge-status'}`} style={{ fontSize: '0.675rem' }}>
                        {pol.enabled ? 'ACTIVE' : 'DISABLED'}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {pol.updatedAt ? new Date(pol.updatedAt).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' }) : '--'}
                    </td>
                    {isAdmin && (
                      <td style={{ textAlign: 'right' }}>
                        <button
                          onClick={() => openEditPolicyModal(pol)}
                          className="btn btn-outline btn-sm"
                          style={{ padding: '2px 8px', fontSize: '0.725rem' }}
                        >
                          Edit
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 1: INCIDENT SLA DETAILS VIEW                                        */}
      {/* ========================================================================= */}
      {selectedSlaDetail && (
        <div className="cc-modal-backdrop" onClick={() => setSelectedSlaDetail(null)}>
          <div className="cc-modal" style={{ maxWidth: 620 }} onClick={(e) => e.stopPropagation()}>
            <div className="cc-modal-header">
              <div>
                <h3 style={{ margin: 0, fontSize: '1.1rem' }}>
                  SLA Details: {selectedSlaDetail.incidentNumber || selectedSlaDetail.incidentId}
                </h3>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {selectedSlaDetail.incidentTitle || 'Incident SLA Tracking Breakdown'}
                </div>
              </div>
              <button onClick={() => setSelectedSlaDetail(null)} className="btn btn-outline btn-sm" style={{ padding: 4 }}>
                <FiX size={16} />
              </button>
            </div>

            <div className="cc-modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {/* Status Banner */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', background: '#f8fafc', borderRadius: 8, border: '1px solid var(--border-color)' }}>
                <div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>Compliance Status</div>
                  <SlaTimer
                    status={selectedSlaDetail.status}
                    remainingMinutes={selectedSlaDetail.remainingMinutes}
                    targetMinutes={selectedSlaDetail.targetMinutes}
                  />
                </div>
                <div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>Escalation Level</div>
                  <span className={`badge ${selectedSlaDetail.escalationLevel === 'LEVEL_3' ? 'badge-critical' : selectedSlaDetail.escalationLevel === 'LEVEL_2' ? 'badge-high' : selectedSlaDetail.escalationLevel === 'LEVEL_1' ? 'badge-medium' : 'badge-status'}`}>
                    {selectedSlaDetail.escalationLevel || 'NONE'}
                  </span>
                </div>
              </div>

              {/* Deadlines Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div style={{ padding: '0.75rem', border: '1px solid var(--border-color)', borderRadius: 6 }}>
                  <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', fontWeight: 600 }}>FIRST-RESPONSE DEADLINE</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 700, marginTop: 4 }}>
                    {selectedSlaDetail.responseDeadline ? new Date(selectedSlaDetail.responseDeadline).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' }) : '--'}
                  </div>
                  <div style={{ fontSize: '0.75rem', marginTop: 4 }}>
                    {selectedSlaDetail.firstRespondedAt ? (
                      <span style={{ color: '#16a34a', fontWeight: 600 }}>
                        &bull; Responded at {new Date(selectedSlaDetail.firstRespondedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    ) : (
                      <span style={{ color: '#d97706', fontWeight: 600 }}>&bull; Pending operator response</span>
                    )}
                  </div>
                </div>

                <div style={{ padding: '0.75rem', border: '1px solid var(--border-color)', borderRadius: 6 }}>
                  <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', fontWeight: 600 }}>RESOLUTION DEADLINE</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 700, marginTop: 4 }}>
                    {selectedSlaDetail.resolutionDeadline || selectedSlaDetail.deadlineAt ? new Date(selectedSlaDetail.resolutionDeadline || selectedSlaDetail.deadlineAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' }) : '--'}
                  </div>
                  <div style={{ fontSize: '0.75rem', marginTop: 4 }}>
                    {selectedSlaDetail.resolvedAt ? (
                      <span style={{ color: '#0284c7', fontWeight: 600 }}>
                        &bull; Resolved at {new Date(selectedSlaDetail.resolvedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    ) : (
                      <span style={{ color: selectedSlaDetail.remainingMinutes < 0 ? '#dc2626' : 'var(--text-muted)', fontWeight: 600 }}>
                        &bull; {formatRemainingMinutes(selectedSlaDetail.remainingMinutes)}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Attributes List */}
              <div style={{ fontSize: '0.8rem', display: 'flex', flexDirection: 'column', gap: '0.4rem', background: '#f8fafc', padding: '0.75rem', borderRadius: 6 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Target Resolution Duration:</span>
                  <strong>{selectedSlaDetail.targetMinutes} minutes ({(selectedSlaDetail.targetMinutes / 60).toFixed(1)} hours)</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Tracking Started:</span>
                  <span>{selectedSlaDetail.startedAt ? new Date(selectedSlaDetail.startedAt).toLocaleString() : '--'}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Breach Recorded:</span>
                  <span style={{ color: selectedSlaDetail.breached ? '#dc2626' : '#16a34a', fontWeight: 600 }}>
                    {selectedSlaDetail.breached ? `YES (at ${new Date(selectedSlaDetail.breachedAt).toLocaleTimeString()})` : 'NO'}
                  </span>
                </div>
              </div>
            </div>

            <div className="cc-modal-footer" style={{ display: 'flex', justifyContent: 'space-between' }}>
              {(selectedSlaDetail.incidentUuid || selectedSlaDetail.incidentId) && (
                <Link
                  to={`/incidents/${selectedSlaDetail.incidentUuid || selectedSlaDetail.incidentId}`}
                  className="btn btn-primary btn-sm"
                >
                  Open Incident Dossier <FiArrowRight size={12} />
                </Link>
              )}
              <button onClick={() => setSelectedSlaDetail(null)} className="btn btn-outline btn-sm">
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 2: ADD / EDIT SLA POLICY MODAL (Admin)                              */}
      {/* ========================================================================= */}
      {showPolicyModal && (
        <div className="cc-modal-backdrop" onClick={() => setShowPolicyModal(false)}>
          <div className="cc-modal" style={{ maxWidth: 480 }} onClick={(e) => e.stopPropagation()}>
            <div className="cc-modal-header">
              <h3 style={{ margin: 0, fontSize: '1.05rem' }}>
                {editingPolicy ? 'Edit SLA Policy' : 'Create New SLA Policy'}
              </h3>
              <button onClick={() => setShowPolicyModal(false)} className="btn btn-outline btn-sm" style={{ padding: 4 }}>
                <FiX size={16} />
              </button>
            </div>

            <form onSubmit={handleSavePolicy}>
              <div className="cc-modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                {policyError && (
                  <div style={{ padding: '0.5rem 0.75rem', background: '#fee2e2', color: '#b91c1c', borderRadius: 6, fontSize: '0.8rem' }}>
                    {policyError}
                  </div>
                )}

                <div>
                  <label style={{ display: 'block', fontSize: '0.775rem', fontWeight: 600, marginBottom: 4 }}>
                    Incident Category *
                  </label>
                  <select
                    className="form-control"
                    value={policyForm.category}
                    onChange={(e) => setPolicyForm({ ...policyForm, category: e.target.value })}
                    required
                  >
                    <option value="Flood">Flood</option>
                    <option value="Fire">Fire</option>
                    <option value="Traffic">Traffic</option>
                    <option value="Pollution">Pollution</option>
                    <option value="Infrastructure">Infrastructure</option>
                    <option value="DEFAULT">DEFAULT (Wildcard Fallback)</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.775rem', fontWeight: 600, marginBottom: 4 }}>
                    Severity Level *
                  </label>
                  <select
                    className="form-control"
                    value={policyForm.severity}
                    onChange={(e) => setPolicyForm({ ...policyForm, severity: e.target.value })}
                    required
                  >
                    <option value="CRITICAL">Critical</option>
                    <option value="HIGH">High</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="LOW">Low</option>
                  </select>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.775rem', fontWeight: 600, marginBottom: 4 }}>
                      Response Limit (min) *
                    </label>
                    <input
                      type="number"
                      min="1"
                      className="form-control"
                      value={policyForm.responseTimeMinutes}
                      onChange={(e) => setPolicyForm({ ...policyForm, responseTimeMinutes: parseInt(e.target.value, 10) || 1 })}
                      required
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '0.775rem', fontWeight: 600, marginBottom: 4 }}>
                      Resolution Limit (min) *
                    </label>
                    <input
                      type="number"
                      min="1"
                      className="form-control"
                      value={policyForm.resolutionTimeMinutes}
                      onChange={(e) => setPolicyForm({ ...policyForm, resolutionTimeMinutes: parseInt(e.target.value, 10) || 1 })}
                      required
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: 4 }}>
                  <input
                    type="checkbox"
                    id="policyEnabled"
                    checked={policyForm.enabled}
                    onChange={(e) => setPolicyForm({ ...policyForm, enabled: e.target.checked })}
                  />
                  <label htmlFor="policyEnabled" style={{ fontSize: '0.8rem', fontWeight: 500, cursor: 'pointer' }}>
                    Policy active and enabled for assignment
                  </label>
                </div>
              </div>

              <div className="cc-modal-footer">
                <button type="button" onClick={() => setShowPolicyModal(false)} className="btn btn-outline btn-sm">
                  Cancel
                </button>
                <button type="submit" disabled={policySubmitting} className="btn btn-primary btn-sm">
                  {policySubmitting ? 'Saving...' : (editingPolicy ? 'Update Policy' : 'Create Policy')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default SlaBoardPage;
