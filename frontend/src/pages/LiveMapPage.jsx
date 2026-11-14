import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { mapApi } from '../api/mapApi';
import { infrastructureApi } from '../api/infrastructureApi';
import { incidentsApi } from '../api/incidentsApi';
import { gisApi } from '../api/gisApi';
import LiveMap from '../components/LiveMap';
import LoadingSpinner from '../components/LoadingSpinner';
import PriorityBadge from '../components/PriorityBadge';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import { Link } from 'react-router-dom';
import {
  FiMapPin,
  FiFilter,
  FiRefreshCw,
  FiActivity,
  FiShield,
  FiAlertTriangle,
  FiPlus,
  FiEdit2,
  FiTrash2,
  FiCrosshair,
  FiSearch,
  FiCheckCircle,
  FiLayers,
  FiRadio,
  FiClock,
  FiX,
  FiCheck,
} from 'react-icons/fi';

export const LiveMapPage = () => {
  const { hasRole } = useAuth();
  const canManageInfrastructure = hasRole(['ADMIN', 'COMMAND_OPERATOR']);
  const canDeleteInfrastructure = hasRole(['ADMIN']);

  // Core map data states
  const [incidentMarkers, setIncidentMarkers] = useState([]);
  const [allIncidents, setAllIncidents] = useState([]);
  const [infrastructureList, setInfrastructureList] = useState([]);
  const [sheltersList, setSheltersList] = useState([]);
  const [sensorsList, setSensorsList] = useState([]);
  const [summary, setSummary] = useState(null);

  // UI state
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [activeTab, setActiveTab] = useState('incidents'); // 'incidents' | 'inspector' | 'infrastructure'
  const [selectedEntity, setSelectedEntity] = useState(null); // { type: 'INCIDENT'|'INFRASTRUCTURE'|'SHELTER'|'SENSOR', data: {} }

  // Map viewport state
  const [mapCenter, setMapCenter] = useState([30.7333, 76.7794]); // Chandigarh default
  const [mapZoom, setMapZoom] = useState(13);

  // Filter states
  const [categoryFilter, setCategoryFilter] = useState('');
  const [severityFilter, setSeverityFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [timeRange, setTimeRange] = useState('ALL'); // ALL, 24H, 7D, 30D

  // Infrastructure Modal state
  const [showInfraModal, setShowInfraModal] = useState(false);
  const [infraModalMode, setInfraModalMode] = useState('CREATE'); // 'CREATE' | 'EDIT'
  const [infraFormData, setInfraFormData] = useState({
    id: null,
    name: '',
    code: '',
    category: 'ROAD',
    status: 'OPERATIONAL',
    latitude: 30.7333,
    longitude: 76.7794,
    address: '',
    importanceLevel: 'PRIMARY',
    maintenancePriority: 'MEDIUM',
    notes: '',
  });
  const [infraModalSaving, setInfraModalSaving] = useState(false);
  const [infraModalError, setInfraModalError] = useState(null);

  // Proximity Search state
  const [radiusActive, setRadiusActive] = useState(false);
  const [radiusMeters, setRadiusMeters] = useState(5000);

  // WebSocket Integration
  const { connected: wsConnected, latestIncidentEvent, latestAlertEvent } = useWebSocket();

  // Load all map layers and summary from backend
  const loadMapData = useCallback(async (isSilent = false) => {
    try {
      if (!isSilent) setLoading(true);
      else setRefreshing(true);

      const params = {};
      if (categoryFilter) params.category = categoryFilter;
      if (severityFilter) params.severity = severityFilter;
      if (statusFilter) params.status = statusFilter;

      if (timeRange === '24H') {
        params.fromDate = new Date(Date.now() - 24 * 3600 * 1000).toISOString();
      } else if (timeRange === '7D') {
        params.fromDate = new Date(Date.now() - 7 * 24 * 3600 * 1000).toISOString();
      } else if (timeRange === '30D') {
        params.fromDate = new Date(Date.now() - 30 * 24 * 3600 * 1000).toISOString();
      }

      const [incRes, infRes, shlRes, sumRes, rawIncRes, sensRes] = await Promise.allSettled([
        mapApi.getMapIncidents(params),
        mapApi.getMapInfrastructure({ category: categoryFilter || undefined }),
        mapApi.getMapShelters({}),
        mapApi.getMapSummary(),
        incidentsApi.getIncidents({ page: 0, size: 100 }),
        gisApi.getSensorsGeoJson(),
      ]);

      if (incRes.status === 'fulfilled') {
        setIncidentMarkers(incRes.value?.data || incRes.value || []);
      }
      if (infRes.status === 'fulfilled') {
        setInfrastructureList(infRes.value?.data || infRes.value || []);
      }
      if (shlRes.status === 'fulfilled') {
        setSheltersList(shlRes.value?.data || shlRes.value || []);
      }
      if (sumRes.status === 'fulfilled') {
        setSummary(sumRes.value?.data || sumRes.value || null);
      }
      if (rawIncRes.status === 'fulfilled') {
        const payload = rawIncRes.value?.data || rawIncRes.value;
        setAllIncidents(payload?.content || payload?.data || (Array.isArray(payload) ? payload : []));
      }
      if (sensRes.status === 'fulfilled') {
        setSensorsList(sensRes.value?.features || []);
      }

      setLastUpdated(new Date());
    } catch (err) {
      console.error('Failed to load map data:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [categoryFilter, severityFilter, statusFilter, timeRange]);

  // Initial load
  useEffect(() => {
    loadMapData();
  }, [loadMapData]);

  // Real-time WebSocket updates
  useEffect(() => {
    if (latestIncidentEvent || latestAlertEvent) {
      loadMapData(true);
    }
  }, [latestIncidentEvent, latestAlertEvent, loadMapData]);

  // Fallback Polling every 30 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      loadMapData(true);
    }, 30000);
    return () => clearInterval(timer);
  }, [loadMapData]);

  // Handle marker selection from map
  const handleSelectMarker = (type, data) => {
    setSelectedEntity({ type, data });
    setActiveTab('inspector');
  };

  // Focus an incident on map from the incident queue
  const handleFocusIncident = (inc) => {
    if (inc.latitude != null && inc.longitude != null && !isNaN(inc.latitude) && !isNaN(inc.longitude)) {
      setMapCenter([inc.latitude, inc.longitude]);
      setMapZoom(16);
      setSelectedEntity({ type: 'INCIDENT', data: inc });
      setActiveTab('inspector');
    }
  };

  // Filter incidents for the side queue
  const filteredSideIncidents = useMemo(() => {
    return allIncidents.filter((inc) => {
      if (categoryFilter && inc.category?.toUpperCase() !== categoryFilter.toUpperCase() && inc.incidentType?.toUpperCase() !== categoryFilter.toUpperCase()) {
        return false;
      }
      if (severityFilter && inc.severity?.toUpperCase() !== severityFilter.toUpperCase() && inc.severityIndicator?.toUpperCase() !== severityFilter.toUpperCase()) {
        return false;
      }
      if (statusFilter && inc.status?.toUpperCase() !== statusFilter.toUpperCase() && inc.currentStatus?.toUpperCase() !== statusFilter.toUpperCase()) {
        return false;
      }
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const matchesTitle = inc.title?.toLowerCase().includes(q);
        const matchesNum = inc.incidentNumber?.toLowerCase().includes(q);
        const matchesLoc = inc.locationName?.toLowerCase().includes(q) || inc.address?.toLowerCase().includes(q);
        if (!matchesTitle && !matchesNum && !matchesLoc) return false;
      }
      return true;
    });
  }, [allIncidents, categoryFilter, severityFilter, statusFilter, searchQuery]);

  // Infrastructure Modal Handlers
  const handleOpenCreateInfra = () => {
    setInfraFormData({
      id: null,
      name: '',
      code: '',
      category: 'ROAD',
      status: 'OPERATIONAL',
      latitude: mapCenter[0] || 30.7333,
      longitude: mapCenter[1] || 76.7794,
      address: '',
      importanceLevel: 'PRIMARY',
      maintenancePriority: 'MEDIUM',
      notes: '',
    });
    setInfraModalMode('CREATE');
    setInfraModalError(null);
    setShowInfraModal(true);
  };

  const handleOpenEditInfra = (inf) => {
    setInfraFormData({
      id: inf.id,
      name: inf.name || '',
      code: inf.code || '',
      category: inf.category || inf.infrastructureType || 'ROAD',
      status: inf.status || 'OPERATIONAL',
      latitude: inf.latitude,
      longitude: inf.longitude,
      address: inf.address || '',
      importanceLevel: inf.importanceLevel || 'PRIMARY',
      maintenancePriority: inf.maintenancePriority || 'MEDIUM',
      notes: inf.notes || '',
    });
    setInfraModalMode('EDIT');
    setInfraModalError(null);
    setShowInfraModal(true);
  };

  const handleSaveInfra = async (e) => {
    e.preventDefault();
    if (!infraFormData.name.trim()) {
      setInfraModalError('Asset name is required.');
      return;
    }
    if (infraFormData.latitude == null || isNaN(infraFormData.latitude)) {
      setInfraModalError('Valid latitude is required (-90 to 90).');
      return;
    }
    if (infraFormData.longitude == null || isNaN(infraFormData.longitude)) {
      setInfraModalError('Valid longitude is required (-180 to 180).');
      return;
    }

    try {
      setInfraModalSaving(true);
      setInfraModalError(null);

      const payload = {
        name: infraFormData.name.trim(),
        code: infraFormData.code ? infraFormData.code.trim() : undefined,
        category: infraFormData.category,
        status: infraFormData.status,
        latitude: parseFloat(infraFormData.latitude),
        longitude: parseFloat(infraFormData.longitude),
        address: infraFormData.address?.trim() || undefined,
        importanceLevel: infraFormData.importanceLevel,
        maintenancePriority: infraFormData.maintenancePriority,
        notes: infraFormData.notes?.trim() || undefined,
      };

      if (infraModalMode === 'CREATE') {
        const res = await infrastructureApi.createInfrastructure(payload);
        const created = res.data || res;
        setSelectedEntity({ type: 'INFRASTRUCTURE', data: created });
      } else {
        const res = await infrastructureApi.updateInfrastructure(infraFormData.id, payload);
        const updated = res.data || res;
        setSelectedEntity({ type: 'INFRASTRUCTURE', data: updated });
      }

      setShowInfraModal(false);
      loadMapData(true);
    } catch (err) {
      setInfraModalError(err.response?.data?.message || err.message || 'Failed to save infrastructure asset');
    } finally {
      setInfraModalSaving(false);
    }
  };

  const handleDeleteInfra = async (id) => {
    if (!window.confirm('Are you sure you want to delete this infrastructure asset from city monitoring?')) {
      return;
    }
    try {
      await infrastructureApi.deleteInfrastructure(id);
      if (selectedEntity?.data?.id === id) {
        setSelectedEntity(null);
      }
      loadMapData(true);
    } catch (err) {
      alert(err.response?.data?.message || err.message || 'Failed to delete asset');
    }
  };

  const handleResetFilters = () => {
    setCategoryFilter('');
    setSeverityFilter('');
    setStatusFilter('');
    setSearchQuery('');
    setTimeRange('ALL');
  };

  return (
    <div className="cc-live-map-page">
      {/* 1. Header & Live Indicator Bar */}
      <div className="cc-page-header" style={{ marginBottom: '1rem' }}>
        <div className="cc-page-title-group">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h1 style={{ margin: 0, fontSize: '1.45rem', fontWeight: 800 }}>GIS Operations & Infrastructure Command</h1>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
                fontSize: '0.72rem',
                padding: '3px 8px',
                borderRadius: '12px',
                backgroundColor: wsConnected ? '#dcfce7' : '#fef3c7',
                color: wsConnected ? '#15803d' : '#b45309',
                fontWeight: 700,
              }}
            >
              <FiRadio size={12} className={wsConnected ? 'animate-pulse' : ''} />
              {wsConnected ? 'WebSocket Live' : 'Polling Active'}
            </span>
          </div>
          <p style={{ margin: '3px 0 0 0', color: '#64748b', fontSize: '0.82rem' }}>
            Real-time geospatial intelligence, municipal infrastructure lifecycle, emergency refuge shelters & sensors
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span style={{ fontSize: '0.75rem', color: '#64748b', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <FiClock size={12} />
            Updated {lastUpdated.toLocaleTimeString()}
          </span>
          <button
            onClick={() => loadMapData(true)}
            className="btn btn-outline btn-sm"
            disabled={refreshing}
            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
          >
            <FiRefreshCw size={13} className={refreshing ? 'animate-spin' : ''} />
            {refreshing ? 'Updating...' : 'Refresh'}
          </button>
          {canManageInfrastructure && (
            <button
              onClick={handleOpenCreateInfra}
              className="btn btn-primary btn-sm"
              style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
            >
              <FiPlus size={14} /> Add Infrastructure
            </button>
          )}
        </div>
      </div>

      {/* 2. KPI Summary Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '0.75rem',
          marginBottom: '1rem',
        }}
      >
        <div className="cc-card" style={{ padding: '0.75rem 1rem', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ width: '38px', height: '38px', borderRadius: '8px', backgroundColor: '#fee2e2', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#dc2626' }}>
            <FiAlertTriangle size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.72rem', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Active Incidents</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a' }}>
              {summary ? summary.activeIncidents : incidentMarkers.filter(m => m.status === 'OPEN' || m.status === 'IN_PROGRESS').length}
            </div>
          </div>
        </div>

        <div className="cc-card" style={{ padding: '0.75rem 1rem', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ width: '38px', height: '38px', borderRadius: '8px', backgroundColor: '#fef2f2', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#b91c1c' }}>
            <span style={{ fontSize: '18px', fontWeight: 800 }}>!</span>
          </div>
          <div>
            <div style={{ fontSize: '0.72rem', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Critical Severity</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#b91c1c' }}>
              {summary ? summary.criticalIncidents : incidentMarkers.filter(m => m.severity === 'CRITICAL').length}
            </div>
          </div>
        </div>

        <div className="cc-card" style={{ padding: '0.75rem 1rem', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ width: '38px', height: '38px', borderRadius: '8px', backgroundColor: '#e0f2fe', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#0284c7' }}>
            <FiMapPin size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.72rem', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Mapped Incidents</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a' }}>
              {incidentMarkers.length}
            </div>
          </div>
        </div>

        <div className="cc-card" style={{ padding: '0.75rem 1rem', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ width: '38px', height: '38px', borderRadius: '8px', backgroundColor: '#dcfce7', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#15803d' }}>
            <FiCheckCircle size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.72rem', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Operational Assets</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#15803d' }}>
              {summary ? summary.operationalInfrastructure : infrastructureList.filter(i => i.status === 'OPERATIONAL').length}
            </div>
          </div>
        </div>

        <div className="cc-card" style={{ padding: '0.75rem 1rem', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ width: '38px', height: '38px', borderRadius: '8px', backgroundColor: '#ccfbf1', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#0f766e' }}>
            <FiShield size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.72rem', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Shelters Ready</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f766e' }}>
              {summary ? summary.availableShelters : sheltersList.filter(s => s.available !== false).length}
            </div>
          </div>
        </div>
      </div>

      {/* 3. Filter Controls Bar */}
      <div
        className="cc-card"
        style={{
          padding: '0.75rem 1rem',
          marginBottom: '1rem',
          display: 'flex',
          flexWrap: 'wrap',
          gap: '0.75rem',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center' }}>
          {/* Search Box */}
          <div style={{ position: 'relative', minWidth: '220px' }}>
            <FiSearch style={{ position: 'absolute', left: '10px', top: '10px', color: '#94a3b8' }} />
            <input
              type="text"
              placeholder="Search title, code, location..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="cc-input"
              style={{ paddingLeft: '30px', fontSize: '0.8rem', height: '34px' }}
            />
          </div>

          {/* Category Dropdown */}
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="cc-select"
            style={{ fontSize: '0.8rem', height: '34px' }}
          >
            <option value="">All Categories</option>
            <option value="TRAFFIC">Traffic & Transport</option>
            <option value="WATER">Water Supply & Drain</option>
            <option value="FIRE">Fire & Emergency</option>
            <option value="FLOOD">Flood & Weather</option>
            <option value="POWER">Power & Electrical</option>
            <option value="ROAD">Roads & Bridges</option>
            <option value="HOSPITAL">Hospitals & Health</option>
            <option value="SHELTER">Public Shelters</option>
            <option value="WASTE">Sanitation & Waste</option>
          </select>

          {/* Severity Dropdown */}
          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            className="cc-select"
            style={{ fontSize: '0.8rem', height: '34px' }}
          >
            <option value="">All Severities</option>
            <option value="CRITICAL">Critical Severity</option>
            <option value="HIGH">High Severity</option>
            <option value="MEDIUM">Medium Severity</option>
            <option value="LOW">Low Severity</option>
          </select>

          {/* Status Dropdown */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="cc-select"
            style={{ fontSize: '0.8rem', height: '34px' }}
          >
            <option value="">All Statuses</option>
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
          </select>

          {/* Time Range */}
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value)}
            className="cc-select"
            style={{ fontSize: '0.8rem', height: '34px' }}
          >
            <option value="ALL">All Time</option>
            <option value="24H">Last 24 Hours</option>
            <option value="7D">Last 7 Days</option>
            <option value="30D">Last 30 Days</option>
          </select>

          {(categoryFilter || severityFilter || statusFilter || searchQuery || timeRange !== 'ALL') && (
            <button onClick={handleResetFilters} className="btn btn-outline btn-sm" style={{ height: '34px', fontSize: '0.75rem' }}>
              Reset Filters
            </button>
          )}
        </div>

        {/* Proximity Radius Toggle */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <button
            onClick={() => setRadiusActive(!radiusActive)}
            className={`btn btn-sm ${radiusActive ? 'btn-primary' : 'btn-outline'}`}
            style={{ display: 'flex', alignItems: 'center', gap: '5px', height: '34px', fontSize: '0.75rem' }}
          >
            <FiCrosshair size={13} />
            {radiusActive ? `Radius: ${radiusMeters / 1000}km` : 'Proximity Buffer'}
          </button>
          {radiusActive && (
            <select
              value={radiusMeters}
              onChange={(e) => setRadiusMeters(Number(e.target.value))}
              className="cc-select"
              style={{ fontSize: '0.75rem', height: '34px' }}
            >
              <option value="2000">2 km Buffer</option>
              <option value="5000">5 km Buffer</option>
              <option value="10000">10 km Buffer</option>
            </select>
          )}
        </div>
      </div>

      {/* 4. Dual-Pane Main Operations Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(500px, 1fr) 420px',
          gap: '1rem',
          alignItems: 'start',
        }}
        className="cc-map-grid"
      >
        {/* Left Pane: Interactive GIS Map */}
        <div className="cc-card" style={{ padding: 0, overflow: 'hidden', height: '620px' }}>
          {loading ? (
            <LoadingSpinner text="Querying spatial layers and infrastructure from PostgreSQL..." />
          ) : (
            <LiveMap
              incidents={incidentMarkers}
              infrastructure={infrastructureList}
              shelters={sheltersList}
              sensors={sensorsList}
              center={mapCenter}
              zoom={mapZoom}
              height="620px"
              selectedId={selectedEntity?.data?.id}
              onSelectMarker={handleSelectMarker}
              radiusQuery={
                radiusActive
                  ? {
                      lat: mapCenter[0],
                      lon: mapCenter[1],
                      radiusMeters,
                    }
                  : null
              }
            />
          )}
        </div>

        {/* Right Pane: Multi-Tab Drawer */}
        <div className="cc-card" style={{ height: '620px', display: 'flex', flexDirection: 'column', padding: '0' }}>
          {/* Tab Navigation */}
          <div
            style={{
              display: 'flex',
              borderBottom: '1px solid #e2e8f0',
              backgroundColor: '#f8fafc',
              borderTopLeftRadius: '8px',
              borderTopRightRadius: '8px',
            }}
          >
            <button
              onClick={() => setActiveTab('incidents')}
              style={{
                flex: 1,
                padding: '10px 8px',
                fontSize: '0.78rem',
                fontWeight: activeTab === 'incidents' ? 700 : 500,
                color: activeTab === 'incidents' ? '#0284c7' : '#64748b',
                backgroundColor: activeTab === 'incidents' ? '#ffffff' : 'transparent',
                border: 'none',
                borderBottom: activeTab === 'incidents' ? '2px solid #0284c7' : 'none',
                cursor: 'pointer',
              }}
            >
              Incidents ({filteredSideIncidents.length})
            </button>

            <button
              onClick={() => setActiveTab('inspector')}
              style={{
                flex: 1,
                padding: '10px 8px',
                fontSize: '0.78rem',
                fontWeight: activeTab === 'inspector' ? 700 : 500,
                color: activeTab === 'inspector' ? '#0284c7' : '#64748b',
                backgroundColor: activeTab === 'inspector' ? '#ffffff' : 'transparent',
                border: 'none',
                borderBottom: activeTab === 'inspector' ? '2px solid #0284c7' : 'none',
                cursor: 'pointer',
              }}
            >
              Inspector {selectedEntity ? '●' : ''}
            </button>

            <button
              onClick={() => setActiveTab('infrastructure')}
              style={{
                flex: 1,
                padding: '10px 8px',
                fontSize: '0.78rem',
                fontWeight: activeTab === 'infrastructure' ? 700 : 500,
                color: activeTab === 'infrastructure' ? '#0284c7' : '#64748b',
                backgroundColor: activeTab === 'infrastructure' ? '#ffffff' : 'transparent',
                border: 'none',
                borderBottom: activeTab === 'infrastructure' ? '2px solid #0284c7' : 'none',
                cursor: 'pointer',
              }}
            >
              Assets ({infrastructureList.length})
            </button>
          </div>

          {/* Tab 1: Incident Queue */}
          {activeTab === 'incidents' && (
            <div style={{ flex: 1, overflowY: 'auto', padding: '0.75rem' }}>
              <div style={{ fontSize: '0.72rem', color: '#64748b', marginBottom: '0.5rem', fontWeight: 600 }}>
                CRITICAL GIS POLICY: Records lacking valid coordinates appear here for field verification but are never plotted on the map.
              </div>

              {filteredSideIncidents.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '2rem 1rem', color: '#94a3b8' }}>
                  <FiAlertTriangle size={32} style={{ margin: '0 auto 8px auto', opacity: 0.5 }} />
                  <p style={{ margin: 0, fontSize: '0.85rem' }}>No incidents matching criteria</p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {filteredSideIncidents.map((inc) => {
                    const hasCoords = inc.latitude != null && inc.longitude != null && !isNaN(inc.latitude) && !isNaN(inc.longitude);
                    const isSelected = selectedEntity?.data?.id === inc.id;

                    return (
                      <div
                        key={inc.id || inc.incidentNumber}
                        onClick={() => {
                          if (hasCoords) handleFocusIncident(inc);
                          else {
                            setSelectedEntity({ type: 'INCIDENT', data: inc });
                            setActiveTab('inspector');
                          }
                        }}
                        style={{
                          padding: '0.65rem 0.75rem',
                          borderRadius: '6px',
                          border: isSelected ? '2px solid #0284c7' : '1px solid #e2e8f0',
                          backgroundColor: isSelected ? '#f0f9ff' : '#ffffff',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#64748b' }}>
                            {inc.incidentNumber}
                          </span>
                          <div style={{ display: 'flex', gap: '4px' }}>
                            <PriorityBadge tier={inc.severity || inc.severityIndicator || 'MEDIUM'} />
                            <StatusBadge status={inc.status || inc.currentStatus || 'OPEN'} />
                          </div>
                        </div>

                        <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#0f172a', marginBottom: '2px' }}>
                          {inc.title || inc.incidentType}
                        </div>

                        <div style={{ fontSize: '0.73rem', color: '#64748b', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span>{inc.locationName || inc.address || 'Smart City Command Centre'}</span>
                          {hasCoords ? (
                            <span style={{ color: '#0284c7', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '2px' }}>
                              <FiCrosshair size={11} /> Focus
                            </span>
                          ) : (
                            <span style={{ color: '#d97706', backgroundColor: '#fef3c7', padding: '1px 5px', borderRadius: '4px', fontSize: '0.68rem', fontWeight: 700 }}>
                              No Coords
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* Tab 2: Inspector Detail */}
          {activeTab === 'inspector' && (
            <div style={{ flex: 1, overflowY: 'auto', padding: '1rem' }}>
              {!selectedEntity ? (
                <div style={{ textAlign: 'center', padding: '3rem 1rem', color: '#94a3b8' }}>
                  <FiMapPin size={36} style={{ margin: '0 auto 8px auto', opacity: 0.4 }} />
                  <p style={{ margin: 0, fontSize: '0.85rem', fontWeight: 500 }}>
                    Click any marker on the map or select an item from the incident queue to inspect real-time dossier details.
                  </p>
                </div>
              ) : (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', paddingBottom: '0.5rem', borderBottom: '1px solid #e2e8f0' }}>
                    <span
                      style={{
                        fontSize: '0.72rem',
                        fontWeight: 800,
                        textTransform: 'uppercase',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        backgroundColor: '#e0f2fe',
                        color: '#0369a1',
                      }}
                    >
                      {selectedEntity.type} INSPECTOR
                    </span>
                    <button
                      onClick={() => setSelectedEntity(null)}
                      style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
                    >
                      <FiX size={16} />
                    </button>
                  </div>

                  {/* INCIDENT DETAILS */}
                  {selectedEntity.type === 'INCIDENT' && (
                    <div style={{ fontSize: '0.8rem', lineHeight: 1.6 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                        <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600 }}>
                          {selectedEntity.data.incidentNumber}
                        </span>
                        <div style={{ display: 'flex', gap: '4px' }}>
                          <PriorityBadge tier={selectedEntity.data.severity || 'MEDIUM'} />
                          <StatusBadge status={selectedEntity.data.status || 'OPEN'} />
                        </div>
                      </div>

                      <h3 style={{ fontSize: '1rem', fontWeight: 800, color: '#0f172a', margin: '4px 0 8px 0' }}>
                        {selectedEntity.data.title || selectedEntity.data.incidentType}
                      </h3>

                      <div style={{ backgroundColor: '#f8fafc', padding: '8px 10px', borderRadius: '6px', marginBottom: '10px' }}>
                        <div><strong>Category:</strong> {selectedEntity.data.category || selectedEntity.data.incidentType}</div>
                        <div><strong>Location:</strong> {selectedEntity.data.locationName || selectedEntity.data.address || 'Unspecified'}</div>
                        <div>
                          <strong>Coordinates:</strong>{' '}
                          {selectedEntity.data.latitude != null && selectedEntity.data.longitude != null
                            ? `${selectedEntity.data.latitude.toFixed(5)}, ${selectedEntity.data.longitude.toFixed(5)}`
                            : <span style={{ color: '#d97706', fontWeight: 700 }}>None (Unplotted)</span>}
                        </div>
                        {selectedEntity.data.createdAt && (
                          <div><strong>Reported:</strong> {new Date(selectedEntity.data.createdAt).toLocaleString()}</div>
                        )}
                      </div>

                      {selectedEntity.data.description && (
                        <div style={{ marginBottom: '10px' }}>
                          <div style={{ fontWeight: 600, color: '#475569', marginBottom: '2px' }}>Description:</div>
                          <p style={{ margin: 0, color: '#334155', backgroundColor: '#ffffff', padding: '6px 8px', border: '1px solid #e2e8f0', borderRadius: '4px' }}>
                            {selectedEntity.data.description}
                          </p>
                        </div>
                      )}

                      {selectedEntity.data.id && (
                        <div style={{ marginTop: '1rem' }}>
                          <Link
                            to={`/incidents/${selectedEntity.data.id}`}
                            className="btn btn-primary btn-sm"
                            style={{ display: 'block', textAlign: 'center', textDecoration: 'none' }}
                          >
                            Open Incident Dossier &rarr;
                          </Link>
                        </div>
                      )}
                    </div>
                  )}

                  {/* INFRASTRUCTURE DETAILS */}
                  {selectedEntity.type === 'INFRASTRUCTURE' && (
                    <div style={{ fontSize: '0.8rem', lineHeight: 1.6 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0284c7' }}>
                          {selectedEntity.data.code}
                        </span>
                        <span
                          style={{
                            fontSize: '0.68rem',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '4px',
                            backgroundColor: selectedEntity.data.status === 'OPERATIONAL' ? '#dcfce7' : '#fee2e2',
                            color: selectedEntity.data.status === 'OPERATIONAL' ? '#15803d' : '#b91c1c',
                          }}
                        >
                          {selectedEntity.data.status}
                        </span>
                      </div>

                      <h3 style={{ fontSize: '1rem', fontWeight: 800, color: '#0f172a', margin: '4px 0 8px 0' }}>
                        {selectedEntity.data.name}
                      </h3>

                      <div style={{ backgroundColor: '#f8fafc', padding: '8px 10px', borderRadius: '6px', marginBottom: '10px' }}>
                        <div><strong>Category:</strong> {selectedEntity.data.category || selectedEntity.data.infrastructureType}</div>
                        <div><strong>Importance Level:</strong> {selectedEntity.data.importanceLevel || 'PRIMARY'}</div>
                        <div><strong>Maintenance Priority:</strong> {selectedEntity.data.maintenancePriority || 'MEDIUM'}</div>
                        <div><strong>Coordinates:</strong> {selectedEntity.data.latitude?.toFixed(5)}, {selectedEntity.data.longitude?.toFixed(5)}</div>
                        {selectedEntity.data.address && <div><strong>Address:</strong> {selectedEntity.data.address}</div>}
                        {selectedEntity.data.lastInspectedAt && (
                          <div><strong>Last Inspected:</strong> {new Date(selectedEntity.data.lastInspectedAt).toLocaleDateString()}</div>
                        )}
                      </div>

                      {selectedEntity.data.notes && (
                        <div style={{ marginBottom: '12px' }}>
                          <div style={{ fontWeight: 600, color: '#475569', marginBottom: '2px' }}>Operational Notes:</div>
                          <p style={{ margin: 0, color: '#334155', fontStyle: 'italic', backgroundColor: '#ffffff', padding: '6px 8px', border: '1px solid #e2e8f0', borderRadius: '4px' }}>
                            "{selectedEntity.data.notes}"
                          </p>
                        </div>
                      )}

                      {canManageInfrastructure && (
                        <div style={{ display: 'flex', gap: '8px', marginTop: '1rem' }}>
                          <button
                            onClick={() => handleOpenEditInfra(selectedEntity.data)}
                            className="btn btn-outline btn-sm"
                            style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}
                          >
                            <FiEdit2 size={13} /> Edit Asset
                          </button>
                          {canDeleteInfrastructure && (
                            <button
                              onClick={() => handleDeleteInfra(selectedEntity.data.id)}
                              className="btn btn-outline btn-sm"
                              style={{ color: '#dc2626', borderColor: '#fca5a5' }}
                            >
                              <FiTrash2 size={13} />
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  )}

                  {/* SHELTER DETAILS */}
                  {selectedEntity.type === 'SHELTER' && (
                    <div style={{ fontSize: '0.8rem', lineHeight: 1.6 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0f766e' }}>
                          REFUGE FACILITY
                        </span>
                        <span
                          style={{
                            fontSize: '0.68rem',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '4px',
                            backgroundColor: selectedEntity.data.available !== false ? '#ccfbf1' : '#fee2e2',
                            color: selectedEntity.data.available !== false ? '#0f766e' : '#b91c1c',
                          }}
                        >
                          {selectedEntity.data.available !== false ? 'Available' : 'Engaged'}
                        </span>
                      </div>

                      <h3 style={{ fontSize: '1rem', fontWeight: 800, color: '#0f172a', margin: '4px 0 8px 0' }}>
                        {selectedEntity.data.name}
                      </h3>

                      <div style={{ backgroundColor: '#f8fafc', padding: '8px 10px', borderRadius: '6px', marginBottom: '10px' }}>
                        <div><strong>Type:</strong> {selectedEntity.data.resourceType}</div>
                        <div><strong>Refuge Capacity:</strong> {selectedEntity.data.capacity || 'N/A'} persons</div>
                        <div><strong>Operational Hours:</strong> {selectedEntity.data.operationalHours || '24/7'}</div>
                        {selectedEntity.data.contactNumber && <div><strong>Contact:</strong> {selectedEntity.data.contactNumber}</div>}
                        {selectedEntity.data.address && <div><strong>Address:</strong> {selectedEntity.data.address}</div>}
                        <div><strong>Coordinates:</strong> {selectedEntity.data.latitude?.toFixed(5)}, {selectedEntity.data.longitude?.toFixed(5)}</div>
                      </div>
                    </div>
                  )}

                  {/* SENSOR DETAILS */}
                  {selectedEntity.type === 'SENSOR' && (
                    <div style={{ fontSize: '0.8rem', lineHeight: 1.6 }}>
                      <h3 style={{ fontSize: '1rem', fontWeight: 800, color: '#0f172a', margin: '4px 0 8px 0' }}>
                        {selectedEntity.data.sensorId} ({selectedEntity.data.sensorType})
                      </h3>
                      <div style={{ backgroundColor: '#f8fafc', padding: '8px 10px', borderRadius: '6px' }}>
                        <div><strong>Location:</strong> {selectedEntity.data.locationName || 'City Grid'}</div>
                        <div><strong>Telemetry Reading:</strong> {selectedEntity.data.lastValue} {selectedEntity.data.unit}</div>
                        <div><strong>Status:</strong> {selectedEntity.data.status || 'ONLINE'}</div>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Tab 3: Infrastructure Management Table */}
          {activeTab === 'infrastructure' && (
            <div style={{ flex: 1, overflowY: 'auto', padding: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569' }}>
                  City Infrastructure Directory ({infrastructureList.length})
                </span>
                {canManageInfrastructure && (
                  <button
                    onClick={handleOpenCreateInfra}
                    className="btn btn-primary btn-sm"
                    style={{ fontSize: '0.72rem', padding: '3px 8px', display: 'flex', alignItems: 'center', gap: '3px' }}
                  >
                    <FiPlus size={12} /> Add Asset
                  </button>
                )}
              </div>

              {infrastructureList.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '2rem 1rem', color: '#94a3b8' }}>
                  <p style={{ margin: 0, fontSize: '0.85rem' }}>No infrastructure assets found</p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {infrastructureList.map((inf) => {
                    const isSelected = selectedEntity?.data?.id === inf.id;

                    return (
                      <div
                        key={inf.id || inf.code}
                        style={{
                          padding: '0.6rem 0.75rem',
                          borderRadius: '6px',
                          border: isSelected ? '2px solid #0284c7' : '1px solid #e2e8f0',
                          backgroundColor: isSelected ? '#f0f9ff' : '#ffffff',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          gap: '8px',
                        }}
                      >
                        <div
                          onClick={() => {
                            setMapCenter([inf.latitude, inf.longitude]);
                            setMapZoom(16);
                            setSelectedEntity({ type: 'INFRASTRUCTURE', data: inf });
                            setActiveTab('inspector');
                          }}
                          style={{ flex: 1, cursor: 'pointer' }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span style={{ fontSize: '0.7rem', fontWeight: 700, color: '#0284c7' }}>
                              {inf.code}
                            </span>
                            <span
                              style={{
                                fontSize: '0.65rem',
                                fontWeight: 700,
                                padding: '1px 5px',
                                borderRadius: '3px',
                                backgroundColor: inf.status === 'OPERATIONAL' ? '#dcfce7' : inf.status === 'UNDER_MAINTENANCE' ? '#f3e8ff' : '#fee2e2',
                                color: inf.status === 'OPERATIONAL' ? '#15803d' : inf.status === 'UNDER_MAINTENANCE' ? '#7e22ce' : '#b91c1c',
                              }}
                            >
                              {inf.status}
                            </span>
                          </div>
                          <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#0f172a' }}>
                            {inf.name}
                          </div>
                          <div style={{ fontSize: '0.72rem', color: '#64748b' }}>
                            {inf.category || inf.infrastructureType} &bull; {inf.address || `${inf.latitude?.toFixed(4)}, ${inf.longitude?.toFixed(4)}`}
                          </div>
                        </div>

                        {canManageInfrastructure && (
                          <div style={{ display: 'flex', gap: '4px' }}>
                            <button
                              onClick={() => handleOpenEditInfra(inf)}
                              className="btn btn-outline btn-sm"
                              style={{ padding: '3px 6px', height: '26px' }}
                              title="Edit Asset"
                            >
                              <FiEdit2 size={12} />
                            </button>
                            {canDeleteInfrastructure && (
                              <button
                                onClick={() => handleDeleteInfra(inf.id)}
                                className="btn btn-outline btn-sm"
                                style={{ padding: '3px 6px', height: '26px', color: '#dc2626' }}
                                title="Delete Asset"
                              >
                                <FiTrash2 size={12} />
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* 5. Infrastructure Create / Edit Modal Dialog */}
      {showInfraModal && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.65)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            padding: '1rem',
          }}
        >
          <div
            className="cc-card"
            style={{
              width: '100%',
              maxWidth: '540px',
              maxHeight: '90vh',
              overflowY: 'auto',
              backgroundColor: '#ffffff',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', paddingBottom: '0.5rem', borderBottom: '1px solid #e2e8f0' }}>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 800, margin: 0, color: '#0f172a' }}>
                {infraModalMode === 'CREATE' ? 'Register Municipal Infrastructure Asset' : 'Update Infrastructure Asset'}
              </h2>
              <button
                onClick={() => setShowInfraModal(false)}
                style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
              >
                <FiX size={18} />
              </button>
            </div>

            {infraModalError && (
              <div
                style={{
                  padding: '8px 12px',
                  backgroundColor: '#fee2e2',
                  color: '#b91c1c',
                  borderRadius: '6px',
                  fontSize: '0.8rem',
                  marginBottom: '1rem',
                  fontWeight: 600,
                }}
              >
                {infraModalError}
              </div>
            )}

            <form onSubmit={handleSaveInfra}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                    Asset Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={infraFormData.name}
                    onChange={(e) => setInfraFormData({ ...infraFormData, name: e.target.value })}
                    className="cc-input"
                    placeholder="e.g. Sector 22 Civil Hospital & Emergency Unit"
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                      Tracking Code
                    </label>
                    <input
                      type="text"
                      value={infraFormData.code}
                      onChange={(e) => setInfraFormData({ ...infraFormData, code: e.target.value })}
                      className="cc-input"
                      placeholder="e.g. INF-HOSP-01 (Auto if blank)"
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                      Category *
                    </label>
                    <select
                      value={infraFormData.category}
                      onChange={(e) => setInfraFormData({ ...infraFormData, category: e.target.value })}
                      className="cc-select"
                    >
                      <option value="ROAD">Roads & Corridors</option>
                      <option value="BRIDGE">Bridges & Overpasses</option>
                      <option value="HOSPITAL">Hospitals & Emergency</option>
                      <option value="WATER_PIPELINE">Water Pipeline</option>
                      <option value="DRAINAGE">Drainage & Storm Drain</option>
                      <option value="SUBSTATION">Power Substation</option>
                      <option value="SHELTER">Public Shelter</option>
                      <option value="OTHER">Other Public Asset</option>
                    </select>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                      Operational Status *
                    </label>
                    <select
                      value={infraFormData.status}
                      onChange={(e) => setInfraFormData({ ...infraFormData, status: e.target.value })}
                      className="cc-select"
                    >
                      <option value="OPERATIONAL">OPERATIONAL</option>
                      <option value="DEGRADED">DEGRADED</option>
                      <option value="OFFLINE">OFFLINE</option>
                      <option value="UNDER_MAINTENANCE">UNDER_MAINTENANCE</option>
                    </select>
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                      Maintenance Priority
                    </label>
                    <select
                      value={infraFormData.maintenancePriority}
                      onChange={(e) => setInfraFormData({ ...infraFormData, maintenancePriority: e.target.value })}
                      className="cc-select"
                    >
                      <option value="LOW">LOW</option>
                      <option value="MEDIUM">MEDIUM</option>
                      <option value="HIGH">HIGH</option>
                      <option value="CRITICAL">CRITICAL</option>
                    </select>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                      Latitude * (-90 to 90)
                    </label>
                    <input
                      type="number"
                      step="0.0001"
                      required
                      value={infraFormData.latitude}
                      onChange={(e) => setInfraFormData({ ...infraFormData, latitude: parseFloat(e.target.value) })}
                      className="cc-input"
                      placeholder="e.g. 30.7320"
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                      Longitude * (-180 to 180)
                    </label>
                    <input
                      type="number"
                      step="0.0001"
                      required
                      value={infraFormData.longitude}
                      onChange={(e) => setInfraFormData({ ...infraFormData, longitude: parseFloat(e.target.value) })}
                      className="cc-input"
                      placeholder="e.g. 76.7710"
                    />
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                    Address or Facility Landmark
                  </label>
                  <input
                    type="text"
                    value={infraFormData.address}
                    onChange={(e) => setInfraFormData({ ...infraFormData, address: e.target.value })}
                    className="cc-input"
                    placeholder="e.g. Sector 22-A Medical Complex, Chandigarh"
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, marginBottom: '4px', color: '#334155' }}>
                    Operational Notes & Technical Observations
                  </label>
                  <textarea
                    rows={3}
                    value={infraFormData.notes}
                    onChange={(e) => setInfraFormData({ ...infraFormData, notes: e.target.value })}
                    className="cc-input"
                    style={{ resize: 'vertical' }}
                    placeholder="Enter maintenance schedule notes, specifications, or load limits..."
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                  <button
                    type="button"
                    onClick={() => setShowInfraModal(false)}
                    className="btn btn-outline btn-sm"
                    disabled={infraModalSaving}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm"
                    disabled={infraModalSaving}
                  >
                    {infraModalSaving ? 'Saving...' : infraModalMode === 'CREATE' ? 'Register Asset' : 'Save Changes'}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default LiveMapPage;
