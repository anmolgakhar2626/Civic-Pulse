import React, { useState, useEffect, useCallback } from 'react';
import { analyticsApi } from '../api/analyticsApi';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  LineChart,
  Line,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import {
  FiPieChart,
  FiBarChart2,
  FiClock,
  FiRefreshCw,
  FiTrendingUp,
  FiTrendingDown,
  FiActivity,
  FiCalendar,
  FiAlertOctagon,
  FiCheckCircle,
  FiDownload,
  FiMapPin,
  FiServer,
  FiFilter,
  FiAlertTriangle,
  FiInfo,
} from 'react-icons/fi';

const SEVERITY_COLORS = {
  CRITICAL: '#b91c1c',
  HIGH: '#c2410c',
  MEDIUM: '#b45309',
  LOW: '#15803d',
};

const STATUS_COLORS = {
  OPEN: '#ef4444',
  IN_PROGRESS: '#f59e0b',
  RESOLVED: '#10b981',
  CLOSED: '#64748b',
};

const SLA_COLORS = {
  ON_TRACK: '#15803d',
  AT_RISK: '#c2410c',
  BREACHED: '#b91c1c',
  RESOLVED: '#0284c7',
};

const INFRA_COLORS = {
  OPERATIONAL: '#10b981',
  DEGRADED: '#f59e0b',
  OFFLINE: '#ef4444',
  UNDER_MAINTENANCE: '#6366f1',
};

export const AnalyticsPage = () => {
  const [overview, setOverview] = useState(null);
  const [trends, setTrends] = useState([]);
  const [categories, setCategories] = useState([]);
  const [severities, setSeverities] = useState([]);
  const [statuses, setStatuses] = useState([]);
  const [slaPerf, setSlaPerf] = useState(null);
  const [infra, setInfra] = useState(null);
  const [responseTimes, setResponseTimes] = useState(null);
  const [locations, setLocations] = useState([]);
  const [departments, setDepartments] = useState([]);

  const [dateRange, setDateRange] = useState('30D');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [severityFilter, setSeverityFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  const getDateFilterParams = useCallback(() => {
    const now = new Date();
    let startDate = null;
    const endDate = now.toISOString();

    if (dateRange === '24H') {
      startDate = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
    } else if (dateRange === '7D') {
      startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
    } else if (dateRange === '30D') {
      startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString();
    } else if (dateRange === '90D') {
      startDate = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000).toISOString();
    }

    const params = {};
    if (startDate) {
      params.startDate = startDate;
      params.endDate = endDate;
    }
    if (categoryFilter && categoryFilter !== 'ALL') {
      params.category = categoryFilter;
    }
    if (severityFilter && severityFilter !== 'ALL') {
      params.severity = severityFilter;
    }
    return params;
  }, [dateRange, categoryFilter, severityFilter]);

  const fetchAnalyticsData = useCallback(async () => {
    try {
      setLoading(true);
      const params = getDateFilterParams();

      const [
        overRes,
        trendsRes,
        catRes,
        sevRes,
        statusRes,
        slaRes,
        infraRes,
        rtRes,
        locRes,
        deptRes,
      ] = await Promise.allSettled([
        analyticsApi.getOverview(params),
        analyticsApi.getIncidentTrends(params),
        analyticsApi.getIncidentsByCategory(params),
        analyticsApi.getIncidentsBySeverity(params),
        analyticsApi.getIncidentsByStatus(params),
        analyticsApi.getSlaPerformance(params),
        analyticsApi.getInfrastructureAnalytics(),
        analyticsApi.getResponseTimes(params),
        analyticsApi.getLocationAnalytics({ ...params, limit: 8 }),
        analyticsApi.getDepartments(params),
      ]);

      if (overRes.status === 'fulfilled') {
        setOverview(overRes.value?.data?.data || overRes.value?.data || overRes.value);
      }
      if (trendsRes.status === 'fulfilled') {
        setTrends(trendsRes.value?.data?.data || trendsRes.value?.data || []);
      }
      if (catRes.status === 'fulfilled') {
        setCategories(catRes.value?.data?.data || catRes.value?.data || []);
      }
      if (sevRes.status === 'fulfilled') {
        setSeverities(sevRes.value?.data?.data || sevRes.value?.data || []);
      }
      if (statusRes.status === 'fulfilled') {
        setStatuses(statusRes.value?.data?.data || statusRes.value?.data || []);
      }
      if (slaRes.status === 'fulfilled') {
        setSlaPerf(slaRes.value?.data?.data || slaRes.value?.data || null);
      }
      if (infraRes.status === 'fulfilled') {
        setInfra(infraRes.value?.data?.data || infraRes.value?.data || null);
      }
      if (rtRes.status === 'fulfilled') {
        setResponseTimes(rtRes.value?.data?.data || rtRes.value?.data || null);
      }
      if (locRes.status === 'fulfilled') {
        const locPayload = locRes.value?.data?.data || locRes.value?.data || {};
        setLocations(locPayload.locations || []);
      }
      if (deptRes.status === 'fulfilled') {
        const deptPayload = deptRes.value?.data?.data || deptRes.value?.data || {};
        setDepartments(deptPayload.departments || []);
      }
    } catch (err) {
      console.error('Failed to load operational analytics data', err);
    } finally {
      setLoading(false);
    }
  }, [getDateFilterParams]);

  useEffect(() => {
    fetchAnalyticsData();
  }, [fetchAnalyticsData]);

  const handleExportCsv = async () => {
    try {
      setExporting(true);
      const params = getDateFilterParams();
      const response = await analyticsApi.exportIncidentsCsv({ ...params, limit: 2000 });

      const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `civicpulse_analytics_${dateRange.toLowerCase()}_${Date.now()}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to export CSV analytics', err);
    } finally {
      setExporting(false);
    }
  };

  // Prepare chart formats
  const severityChartData = severities.map((s) => ({
    name: s.severity,
    value: s.count,
    percentage: s.percentage,
    color: SEVERITY_COLORS[s.severity] || '#64748b',
  }));

  const statusChartData = statuses.map((s) => ({
    name: s.status,
    value: s.count,
    percentage: s.percentage,
    color: STATUS_COLORS[s.status] || '#64748b',
  }));

  const responseBracketData = responseTimes?.responseTimeDistribution
    ? Object.entries(responseTimes.responseTimeDistribution).map(([name, value]) => ({
      name,
      count: value,
    }))
    : [];

  const resolutionBracketData = responseTimes?.resolutionTimeDistribution
    ? Object.entries(responseTimes.resolutionTimeDistribution).map(([name, value]) => ({
      name,
      count: value,
    }))
    : [];

  const escalationData = slaPerf?.escalationDistribution
    ? Object.entries(slaPerf.escalationDistribution).map(([name, value]) => ({
      name: name.replace('_', ' '),
      count: value,
    }))
    : [];

  const renderComparisonBadge = (comparison) => {
    if (!comparison || comparison.previousValue === 0) return null;
    const isImproved = comparison.improved;
    const delta = comparison.deltaPercentage;
    const isUp = delta > 0;

    return (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '3px',
          fontSize: '0.72rem',
          fontWeight: 600,
          padding: '2px 6px',
          borderRadius: '12px',
          marginTop: '4px',
          backgroundColor: isImproved ? '#dcfce7' : '#fee2e2',
          color: isImproved ? '#166534' : '#991b1b',
        }}
      >
        {isUp ? <FiTrendingUp size={11} /> : <FiTrendingDown size={11} />}
        {Math.abs(delta)}% vs prev period
      </span>
    );
  };

  return (
    <div style={{ paddingBottom: '3rem' }}>
      {/* Page Header & Filter Controls */}
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <h1>City Intelligence & Analytics Command</h1>
          <p>
            Real database intelligence, incident velocity, SLA tracking, and infrastructure health
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
          {/* Date range pills */}
          <div
            style={{
              display: 'flex',
              background: '#f1f5f9',
              borderRadius: '6px',
              padding: '3px',
              border: '1px solid #cbd5e1',
            }}
          >
            {['24H', '7D', '30D', '90D', 'ALL'].map((r) => (
              <button
                key={r}
                onClick={() => setDateRange(r)}
                className={`btn btn-sm ${dateRange === r ? 'btn-primary' : 'btn-outline'}`}
                style={{
                  border: 'none',
                  padding: '0.25rem 0.6rem',
                  fontSize: '0.75rem',
                  fontWeight: dateRange === r ? 600 : 500,
                }}
              >
                {r === 'ALL' ? 'All Time' : r}
              </button>
            ))}
          </div>

          {/* Category Filter */}
          <div style={{ position: 'relative' }}>
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="cc-filter-select"
              style={{
                fontSize: '0.75rem',
                padding: '0.35rem 0.6rem',
                borderRadius: '4px',
                border: '1px solid #cbd5e1',
                background: '#ffffff',
                color: 'var(--text-main)',
              }}
            >
              <option value="ALL">All Categories</option>
              <option value="Traffic">Traffic</option>
              <option value="Flood">Flood</option>
              <option value="Fire">Fire</option>
              <option value="Pollution">Pollution</option>
              <option value="Infrastructure">Infrastructure</option>
              <option value="Water Supply">Water Supply</option>
              <option value="Power Outage">Power Outage</option>
            </select>
          </div>

          {/* Severity Filter */}
          <div style={{ position: 'relative' }}>
            <select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
              className="cc-filter-select"
              style={{
                fontSize: '0.75rem',
                padding: '0.35rem 0.6rem',
                borderRadius: '4px',
                border: '1px solid #cbd5e1',
                background: '#ffffff',
                color: 'var(--text-main)',
              }}
            >
              <option value="ALL">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>

          {/* Export CSV Button */}
          <button
            onClick={handleExportCsv}
            disabled={exporting}
            className="btn btn-outline btn-sm"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
            title="Download sanitized CSV report"
          >
            <FiDownload size={13} />
            {exporting ? 'Exporting...' : 'Export CSV'}
          </button>

          {/* Refresh Button */}
          <button
            onClick={fetchAnalyticsData}
            className="btn btn-outline btn-sm"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
          >
            <FiRefreshCw size={13} /> Refresh
          </button>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner text="Computing SQL aggregations and city intelligence from PostgreSQL..." />
      ) : (
        <>
          {/* Key Operational Aggregations Summary */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '1rem',
              marginBottom: '1.25rem',
            }}
          >
            <div className="cc-card" style={{ padding: '0.85rem 1rem' }}>
              <span
                style={{
                  fontSize: '0.7rem',
                  color: 'var(--text-subtle)',
                  textTransform: 'uppercase',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                }}
              >
                Total Incidents
              </span>
              <div
                style={{
                  fontSize: '1.6rem',
                  fontWeight: 700,
                  color: 'var(--text-primary)',
                  lineHeight: 1.2,
                  marginTop: '2px',
                }}
              >
                {overview?.totalIncidents ?? 0}
              </div>
              {overview?.comparisons?.totalIncidents &&
                renderComparisonBadge(overview.comparisons.totalIncidents)}
            </div>

            <div className="cc-card" style={{ padding: '0.85rem 1rem' }}>
              <span
                style={{
                  fontSize: '0.7rem',
                  color: 'var(--text-subtle)',
                  textTransform: 'uppercase',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                }}
              >
                Active Operations
              </span>
              <div
                style={{
                  fontSize: '1.6rem',
                  fontWeight: 700,
                  color: '#c2410c',
                  lineHeight: 1.2,
                  marginTop: '2px',
                }}
              >
                {overview?.activeIncidents ?? 0}
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)', marginTop: '4px' }}>
                {overview?.resolvedIncidents ?? 0} resolved
              </span>
            </div>

            <div className="cc-card" style={{ padding: '0.85rem 1rem' }}>
              <span
                style={{
                  fontSize: '0.7rem',
                  color: 'var(--text-subtle)',
                  textTransform: 'uppercase',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                }}
              >
                Critical Incidents
              </span>
              <div
                style={{
                  fontSize: '1.6rem',
                  fontWeight: 700,
                  color: '#b91c1c',
                  lineHeight: 1.2,
                  marginTop: '2px',
                }}
              >
                {overview?.criticalIncidents ?? 0}
              </div>
              {overview?.comparisons?.criticalIncidents &&
                renderComparisonBadge(overview.comparisons.criticalIncidents)}
            </div>

            <div className="cc-card" style={{ padding: '0.85rem 1rem' }}>
              <span
                style={{
                  fontSize: '0.7rem',
                  color: 'var(--text-subtle)',
                  textTransform: 'uppercase',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                }}
              >
                SLA Compliance Rate
              </span>
              <div
                style={{
                  fontSize: '1.6rem',
                  fontWeight: 700,
                  color: (overview?.slaComplianceRate ?? 100) >= 90 ? '#15803d' : '#b91c1c',
                  lineHeight: 1.2,
                  marginTop: '2px',
                }}
              >
                {overview?.slaComplianceRate ?? 100}%
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)', marginTop: '4px' }}>
                {overview?.slaBreaches ?? 0} breached targets
              </span>
            </div>

            <div className="cc-card" style={{ padding: '0.85rem 1rem' }}>
              <span
                style={{
                  fontSize: '0.7rem',
                  color: 'var(--text-subtle)',
                  textTransform: 'uppercase',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                }}
              >
                Avg Resolution Time
              </span>
              <div
                style={{
                  fontSize: '1.6rem',
                  fontWeight: 700,
                  color: 'var(--accent-primary)',
                  lineHeight: 1.2,
                  marginTop: '2px',
                }}
              >
                {overview?.avgResolutionTimeHours ?? 0}h
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)', marginTop: '4px' }}>
                ~{overview?.avgResolutionTimeMinutes ?? 0} min total
              </span>
            </div>

            <div className="cc-card" style={{ padding: '0.85rem 1rem' }}>
              <span
                style={{
                  fontSize: '0.7rem',
                  color: 'var(--text-subtle)',
                  textTransform: 'uppercase',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                }}
              >
                Infrastructure Health
              </span>
              <div
                style={{
                  fontSize: '1.6rem',
                  fontWeight: 700,
                  color: '#0d9488',
                  lineHeight: 1.2,
                  marginTop: '2px',
                }}
              >
                {infra?.operationalRate ?? 100}%
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)', marginTop: '4px' }}>
                {infra?.operationalCount ?? 0} of {infra?.totalAssets ?? 0} operational
              </span>
            </div>
          </div>

          {/* Explainable City Intelligence Insights Banner */}
          <div
            className="cc-card"
            style={{
              marginBottom: '1.25rem',
              padding: '0.9rem 1.25rem',
              background: '#f8fafc',
              borderLeft: '4px solid var(--accent-primary)',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                marginBottom: '0.4rem',
                fontWeight: 600,
                fontSize: '0.85rem',
                color: 'var(--text-main)',
              }}
            >
              <FiInfo color="var(--accent-primary)" size={16} />
              Command Intelligence Highlights ({dateRange})
            </div>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                gap: '0.75rem',
                fontSize: '0.78rem',
                color: 'var(--text-muted)',
              }}
            >
              <div>
                • <strong>Incident Velocity:</strong> Recorded{' '}
                <strong>{overview?.totalIncidents ?? 0}</strong> incidents, of which{' '}
                <strong>{overview?.resolvedIncidents ?? 0}</strong> have been successfully resolved.
              </div>
              <div>
                • <strong>SLA Adherence:</strong> Operating at{' '}
                <strong
                  style={{
                    color: (overview?.slaComplianceRate ?? 100) >= 90 ? '#15803d' : '#b91c1c',
                  }}
                >
                  {overview?.slaComplianceRate ?? 100}%
                </strong>{' '}
                compliance with {overview?.slaBreaches ?? 0} deadline breach(es).
              </div>
              <div>
                • <strong>Top Category:</strong>{' '}
                {categories.length > 0 ? (
                  <span>
                    <strong>{categories[0].category}</strong> represents{' '}
                    <strong>{categories[0].percentage}%</strong> of caseload ({categories[0].count}{' '}
                    incidents).
                  </span>
                ) : (
                  'No category hotspots detected.'
                )}
              </div>
              <div>
                • <strong>Asset Status:</strong>{' '}
                {infra ? (
                  <span>
                    <strong>{infra.operationalRate}%</strong> assets functional ({infra.degradedCount}{' '}
                    degraded, {infra.offlineCount} offline).
                  </span>
                ) : (
                  'Assets data unavailable.'
                )}
              </div>
            </div>
          </div>

          {/* ========================================================================= */}
          {/* PRIMARY 2x2 VISUAL GRID:                                                   */}
          {/* Row 1: Incident Velocity (Created/Resolved/Breached) | SLA & Escalations  */}
          {/* Row 2: Category Distribution (Bar)                  | Severity & Status   */}
          {/* ========================================================================= */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))',
              gap: '1.25rem',
              marginBottom: '1.25rem',
            }}
          >
            {/* [1,1] Top-Left: Incident Velocity (Created, Resolved, Breached) */}
            <div className="cc-card" style={{ display: 'flex', flexDirection: 'column' }}>
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiTrendingUp size={15} color="var(--accent-primary)" />
                  Incident Velocity & Trajectory
                </span>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)' }}>
                  Created vs. Resolved vs. SLA Breached
                </span>
              </div>
              {trends.length === 0 ? (
                <EmptyState
                  title="No timeline data"
                  description="No incident records found for the selected filter interval."
                />
              ) : (
                <div style={{ width: '100%', height: '300px', padding: '0.5rem 0' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart
                      data={trends}
                      margin={{ top: 10, right: 25, left: 0, bottom: 20 }}
                    >
                      <defs>
                        <linearGradient id="colorCreated" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#0284c7" stopOpacity={0.35} />
                          <stop offset="95%" stopColor="#0284c7" stopOpacity={0.0} />
                        </linearGradient>
                        <linearGradient id="colorResolved" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#10b981" stopOpacity={0.35} />
                          <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                      <XAxis dataKey="period" stroke="#64748b" fontSize={11} />
                      <YAxis stroke="#64748b" fontSize={11} allowDecimals={false} />
                      <Tooltip />
                      <Legend wrapperStyle={{ fontSize: '11px' }} />
                      <Area
                        type="monotone"
                        dataKey="createdCount"
                        name="Created"
                        stroke="#0284c7"
                        strokeWidth={2}
                        fillOpacity={1}
                        fill="url(#colorCreated)"
                      />
                      <Area
                        type="monotone"
                        dataKey="resolvedCount"
                        name="Resolved"
                        stroke="#10b981"
                        strokeWidth={2}
                        fillOpacity={1}
                        fill="url(#colorResolved)"
                      />
                      <Line
                        type="monotone"
                        dataKey="breachedCount"
                        name="Breached"
                        stroke="#b91c1c"
                        strokeWidth={2}
                        dot={{ r: 3 }}
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>

            {/* [1,2] Top-Right: SLA Compliance & Escalation Data */}
            <div className="cc-card" style={{ display: 'flex', flexDirection: 'column' }}>
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiClock size={15} color="var(--accent-teal)" />
                  SLA Compliance & Escalation Intelligence
                </span>
                <span
                  style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    color: (slaPerf?.complianceRate ?? 100) >= 90 ? '#15803d' : '#b91c1c',
                  }}
                >
                  {slaPerf?.complianceRate ?? 100}% Compliant
                </span>
              </div>
              <div style={{ padding: '0.25rem 0', flex: 1, display: 'flex', flexDirection: 'column' }}>
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(4, 1fr)',
                    gap: '0.5rem',
                    textAlign: 'center',
                    marginBottom: '0.75rem',
                  }}
                >
                  <div style={{ background: '#f8fafc', padding: '0.45rem', borderRadius: '4px', border: '1px solid #e2e8f0' }}>
                    <div style={{ fontSize: '0.68rem', color: 'var(--text-subtle)', textTransform: 'uppercase', fontWeight: 600 }}>Tracked</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-main)' }}>{slaPerf?.totalTracked ?? 0}</div>
                  </div>
                  <div style={{ background: '#f0fdf4', padding: '0.45rem', borderRadius: '4px', border: '1px solid #bbf7d0' }}>
                    <div style={{ fontSize: '0.68rem', color: '#166534', textTransform: 'uppercase', fontWeight: 600 }}>Compliant</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#166534' }}>
                      {slaPerf?.compliantCount ?? 0}
                    </div>
                  </div>
                  <div style={{ background: '#fff7ed', padding: '0.45rem', borderRadius: '4px', border: '1px solid #fed7aa' }}>
                    <div style={{ fontSize: '0.68rem', color: '#9a3412', textTransform: 'uppercase', fontWeight: 600 }}>At Risk</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#9a3412' }}>
                      {slaPerf?.atRiskCount ?? 0}
                    </div>
                  </div>
                  <div style={{ background: '#fef2f2', padding: '0.45rem', borderRadius: '4px', border: '1px solid #fecaca' }}>
                    <div style={{ fontSize: '0.68rem', color: '#991b1b', textTransform: 'uppercase', fontWeight: 600 }}>Breached</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#991b1b' }}>
                      {slaPerf?.breachedCount ?? 0}
                    </div>
                  </div>
                </div>

                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-subtle)', marginBottom: '4px' }}>
                  Escalation Level Distribution:
                </div>
                {escalationData.length === 0 ? (
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', padding: '1rem 0', textAlign: 'center' }}>
                    No escalations triggered.
                  </div>
                ) : (
                  <div style={{ width: '100%', height: '175px' }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={escalationData} margin={{ top: 10, right: 20, left: -10, bottom: 5 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                        <XAxis dataKey="name" stroke="#64748b" fontSize={10} />
                        <YAxis stroke="#64748b" fontSize={10} allowDecimals={false} />
                        <Tooltip />
                        <Bar dataKey="count" fill="#c2410c" name="Escalated Incidents" radius={[3, 3, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                )}
              </div>
            </div>

            {/* [2,1] Bottom-Left: Incident Category Distribution (Bar Chart) */}
            <div className="cc-card" style={{ display: 'flex', flexDirection: 'column' }}>
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiBarChart2 size={15} color="var(--accent-primary)" />
                  Incident Category Distribution
                </span>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)' }}>
                  Total vs. Critical Incidents
                </span>
              </div>
              {categories.length === 0 ? (
                <EmptyState
                  title="No category data"
                  description="No incidents recorded in this timeframe."
                />
              ) : (
                <div style={{ width: '100%', height: '280px', padding: '0.5rem 0' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={categories}
                      layout="vertical"
                      margin={{ top: 10, right: 25, left: 35, bottom: 10 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                      <XAxis type="number" stroke="#64748b" fontSize={11} allowDecimals={false} />
                      <YAxis dataKey="category" type="category" stroke="#64748b" fontSize={11} width={90} />
                      <Tooltip />
                      <Legend wrapperStyle={{ fontSize: '11px' }} />
                      <Bar dataKey="count" fill="#0284c7" name="Total Incidents" radius={[0, 3, 3, 0]} />
                      <Bar dataKey="criticalCount" fill="#b91c1c" name="Critical" radius={[0, 3, 3, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>

            {/* [2,2] Bottom-Right: Severity & Lifecycle Breakdowns (Donut Charts) */}
            <div className="cc-card" style={{ display: 'flex', flexDirection: 'column' }}>
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiPieChart size={15} color="var(--accent-teal)" />
                  Severity & Lifecycle Breakdowns
                </span>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)' }}>
                  Severity Tiers & Operational Lifecycle
                </span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', height: '280px', alignItems: 'center' }}>
                <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                  <div
                    style={{
                      textAlign: 'center',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      color: 'var(--text-subtle)',
                      marginBottom: '2px',
                    }}
                  >
                    By Severity Tier
                  </div>
                  {severityChartData.length === 0 ? (
                    <EmptyState title="No data" description="" />
                  ) : (
                    <div style={{ flex: 1, width: '100%' }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={severityChartData}
                            dataKey="value"
                            nameKey="name"
                            cx="50%"
                            cy="50%"
                            innerRadius={38}
                            outerRadius={68}
                            paddingAngle={3}
                          >
                            {severityChartData.map((entry) => (
                              <Cell key={`sev-${entry.name}`} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip />
                          <Legend wrapperStyle={{ fontSize: '10px' }} />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                  )}
                </div>

                <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                  <div
                    style={{
                      textAlign: 'center',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      color: 'var(--text-subtle)',
                      marginBottom: '2px',
                    }}
                  >
                    By Lifecycle Status
                  </div>
                  {statusChartData.length === 0 ? (
                    <EmptyState title="No data" description="" />
                  ) : (
                    <div style={{ flex: 1, width: '100%' }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={statusChartData}
                            dataKey="value"
                            nameKey="name"
                            cx="50%"
                            cy="50%"
                            innerRadius={38}
                            outerRadius={68}
                            paddingAngle={3}
                          >
                            {statusChartData.map((entry) => (
                              <Cell key={`st-${entry.name}`} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip />
                          <Legend wrapperStyle={{ fontSize: '10px' }} />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* ========================================================================= */}
          {/* SECONDARY ANALYTICS: Response Times, Infrastructure, Hotspots, Workload  */}
          {/* ========================================================================= */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))', gap: '1.25rem', marginBottom: '1.25rem' }}>
            {/* Response & Resolution Time Distribution */}
            <div className="cc-card">
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiActivity size={15} color="var(--accent-teal)" />
                  Response & Resolution Duration Distributions
                </span>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)' }}>
                  Avg Resp: {responseTimes?.avgFirstResponseMinutes ?? 0}m | Avg Res: {responseTimes?.avgResolutionHours ?? 0}h
                </span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', height: '220px' }}>
                <div>
                  <div
                    style={{
                      fontSize: '0.72rem',
                      fontWeight: 600,
                      color: 'var(--text-subtle)',
                      textAlign: 'center',
                      marginBottom: '4px',
                    }}
                  >
                    First Response Time
                  </div>
                  <ResponsiveContainer width="100%" height="90%">
                    <BarChart data={responseBracketData} margin={{ top: 5, right: 10, left: -15, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                      <XAxis dataKey="name" stroke="#64748b" fontSize={10} />
                      <YAxis stroke="#64748b" fontSize={10} allowDecimals={false} />
                      <Tooltip />
                      <Bar dataKey="count" fill="#0f766e" radius={[2, 2, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>

                <div>
                  <div
                    style={{
                      fontSize: '0.72rem',
                      fontWeight: 600,
                      color: 'var(--text-subtle)',
                      textAlign: 'center',
                      marginBottom: '4px',
                    }}
                  >
                    Resolution Duration
                  </div>
                  <ResponsiveContainer width="100%" height="90%">
                    <BarChart data={resolutionBracketData} margin={{ top: 5, right: 10, left: -15, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                      <XAxis dataKey="name" stroke="#64748b" fontSize={10} />
                      <YAxis stroke="#64748b" fontSize={10} allowDecimals={false} />
                      <Tooltip />
                      <Bar dataKey="count" fill="#0284c7" radius={[2, 2, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>

            {/* Infrastructure Asset Health Breakdown */}
            <div className="cc-card">
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiServer size={15} color="var(--accent-primary)" />
                  Infrastructure Asset Health & Reliability
                </span>
                <span
                  style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    color: (infra?.operationalRate ?? 100) >= 80 ? '#10b981' : '#f59e0b',
                  }}
                >
                  {infra?.operationalRate ?? 100}% Operational
                </span>
              </div>
              <div style={{ padding: '0.5rem 0' }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.5rem', textAlign: 'center', marginBottom: '1rem' }}>
                  <div style={{ background: '#f8fafc', padding: '0.5rem', borderRadius: '4px', border: '1px solid #e2e8f0' }}>
                    <div style={{ fontSize: '0.68rem', color: 'var(--text-subtle)' }}>Total Assets</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>{infra?.totalAssets ?? 0}</div>
                  </div>
                  <div style={{ background: '#f0fdf4', padding: '0.5rem', borderRadius: '4px', border: '1px solid #bbf7d0' }}>
                    <div style={{ fontSize: '0.68rem', color: '#166534' }}>Operational</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#166534' }}>{infra?.operationalCount ?? 0}</div>
                  </div>
                  <div style={{ background: '#fffbeb', padding: '0.5rem', borderRadius: '4px', border: '1px solid #fde68a' }}>
                    <div style={{ fontSize: '0.68rem', color: '#b45309' }}>Degraded</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#b45309' }}>{infra?.degradedCount ?? 0}</div>
                  </div>
                  <div style={{ background: '#fef2f2', padding: '0.5rem', borderRadius: '4px', border: '1px solid #fecaca' }}>
                    <div style={{ fontSize: '0.68rem', color: '#991b1b' }}>Offline</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#991b1b' }}>{infra?.offlineCount ?? 0}</div>
                  </div>
                </div>

                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-subtle)', marginBottom: '6px' }}>
                  Operational Reliability Progress:
                </div>
                <div style={{ width: '100%', height: '10px', background: '#e2e8f0', borderRadius: '5px', overflow: 'hidden' }}>
                  <div
                    style={{
                      width: `${infra?.operationalRate ?? 100}%`,
                      height: '100%',
                      background: (infra?.operationalRate ?? 100) >= 80 ? '#10b981' : '#f59e0b',
                      transition: 'width 0.5s ease',
                    }}
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Geographic Concentrations (Top Wards/Locations) */}
          <div className="cc-card" style={{ marginBottom: '1.25rem' }}>
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiMapPin size={15} color="var(--accent-primary)" />
                Geographic Concentrations & Hotspot Wards
              </span>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
                Top incident clusters across the city
              </span>
            </div>
            {locations.length === 0 ? (
              <EmptyState
                title="No location data"
                description="No incidents logged with physical addresses or coordinates."
              />
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table className="cc-table" style={{ width: '100%', fontSize: '0.8rem' }}>
                  <thead>
                    <tr>
                      <th>Rank</th>
                      <th>Location / Ward Name</th>
                      <th style={{ textAlign: 'center' }}>Total Incidents</th>
                      <th style={{ textAlign: 'center' }}>Critical Incidents</th>
                      <th style={{ textAlign: 'center' }}>Coordinates</th>
                      <th style={{ textAlign: 'right' }}>Concentration Risk</th>
                    </tr>
                  </thead>
                  <tbody>
                    {locations.map((loc, idx) => (
                      <tr key={`loc-${idx}`}>
                        <td style={{ fontWeight: 600, color: 'var(--text-subtle)' }}>#{idx + 1}</td>
                        <td style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                            <FiMapPin size={13} color="var(--accent-primary)" />
                            {loc.locationName}
                          </div>
                        </td>
                        <td style={{ textAlign: 'center', fontWeight: 600 }}>{loc.count}</td>
                        <td style={{ textAlign: 'center' }}>
                          {loc.criticalCount > 0 ? (
                            <span
                              style={{
                                backgroundColor: '#fee2e2',
                                color: '#991b1b',
                                padding: '1px 6px',
                                borderRadius: '10px',
                                fontWeight: 700,
                                fontSize: '0.72rem',
                              }}
                            >
                              {loc.criticalCount} Critical
                            </span>
                          ) : (
                            <span style={{ color: 'var(--text-subtle)' }}>0</span>
                          )}
                        </td>
                        <td style={{ textAlign: 'center', color: 'var(--text-subtle)', fontFamily: 'monospace' }}>
                          {loc.latitude && loc.longitude
                            ? `${loc.latitude.toFixed(4)}, ${loc.longitude.toFixed(4)}`
                            : 'GIS Unset'}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <span
                            className={`badge ${
                              loc.criticalCount > 2
                                ? 'badge-critical'
                                : loc.count > 5
                                ? 'badge-high'
                                : 'badge-low'
                            }`}
                          >
                            {loc.criticalCount > 2 ? 'HIGH RISK' : loc.count > 5 ? 'MEDIUM RISK' : 'LOW RISK'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Department Workload Breakdown */}
          {departments.length > 0 && (
            <div className="cc-card">
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiServer size={15} color="var(--accent-primary)" />
                  Municipal Department Caseload & Compliance
                </span>
              </div>
              <div style={{ width: '100%', height: '260px' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={departments}
                    margin={{ top: 20, right: 30, left: 10, bottom: 20 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                    <XAxis dataKey="departmentName" stroke="#64748b" fontSize={11} />
                    <YAxis stroke="#64748b" fontSize={11} allowDecimals={false} />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="totalIncidents" fill="#94a3b8" name="Total Logged" radius={[2, 2, 0, 0]} />
                    <Bar dataKey="activeIncidents" fill="#c2410c" name="Active Queue" radius={[2, 2, 0, 0]} />
                    <Bar dataKey="resolvedIncidents" fill="#15803d" name="Resolved" radius={[2, 2, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default AnalyticsPage;
