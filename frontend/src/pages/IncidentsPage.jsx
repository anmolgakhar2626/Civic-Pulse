import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { incidentsApi } from '../api/incidentsApi';
import PriorityBadge from '../components/PriorityBadge';
import StatusBadge from '../components/StatusBadge';
import Modal from '../components/Modal';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import {
  FiPlus,
  FiSearch,
  FiFilter,
  FiChevronLeft,
  FiChevronRight,
  FiAlertTriangle,
  FiEye,
  FiEdit2,
  FiTrash2,
  FiRefreshCw,
  FiCheckCircle,
  FiMapPin,
  FiActivity,
  FiInfo,
  FiCheck,
  FiClock,
  FiUser,
} from 'react-icons/fi';

const CATEGORIES = [
  'Flood',
  'Fire',
  'Traffic',
  'Pollution',
  'Infrastructure',
  'Public Safety',
  'Sanitation',
  'Energy',
  'Emergency',
  'Other',
];

const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export const IncidentsPage = () => {
  const { user, hasRole } = useAuth();
  const isAdmin = hasRole(['ADMIN']);
  const canEdit = hasRole(['ADMIN', 'COMMAND_OPERATOR']);
  const canUpdateStatus = hasRole(['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER']);

  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successBanner, setSuccessBanner] = useState('');

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const size = 10;

  // Filters
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [severityFilter, setSeverityFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  // Active / Selected Incident for View, Edit, Status, Delete
  const [selectedIncident, setSelectedIncident] = useState(null);

  // Form States
  const [newIncident, setNewIncident] = useState({
    title: '',
    description: '',
    category: 'Flood',
    severity: 'MEDIUM',
    status: 'OPEN',
    locationName: '',
    latitude: 30.7333,
    longitude: 76.7794,
    source: 'CITIZEN',
  });
  const [editIncident, setEditIncident] = useState({
    title: '',
    description: '',
    category: 'Flood',
    severity: 'MEDIUM',
    locationName: '',
    latitude: 30.7333,
    longitude: 76.7794,
  });
  const [statusUpdate, setStatusUpdate] = useState({
    status: 'IN_PROGRESS',
    notes: '',
  });

  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const { latestIncidentEvent } = useWebSocket();

  const fetchIncidents = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const params = {
        page,
        size,
        sort: 'createdAt,desc',
      };
      if (search.trim()) params.search = search.trim();
      if (categoryFilter) params.category = categoryFilter;
      if (severityFilter) params.severity = severityFilter;
      if (statusFilter) params.status = statusFilter;

      const res = await incidentsApi.getIncidents(params);
      const content = res.content || res.data || (Array.isArray(res) ? res : []);
      setIncidents(content);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || content.length);
    } catch (err) {
      console.error('Failed to load incidents', err);
      setError(err.message || 'Failed to fetch incident records from the database.');
    } finally {
      setLoading(false);
    }
  }, [page, search, categoryFilter, severityFilter, statusFilter]);

  useEffect(() => {
    fetchIncidents();
  }, [fetchIncidents]);

  // Live WebSocket refresh
  useEffect(() => {
    if (latestIncidentEvent) {
      fetchIncidents();
    }
  }, [latestIncidentEvent, fetchIncidents]);

  const showNotification = (msg) => {
    setSuccessBanner(msg);
    setTimeout(() => setSuccessBanner(''), 4500);
  };

  // Coordinate Validation
  const validateCoordinates = (lat, lng) => {
    const errors = {};
    const latNum = parseFloat(lat);
    const lngNum = parseFloat(lng);

    if (isNaN(latNum) || latNum < -90 || latNum > 90) {
      errors.latitude = 'Latitude must be a valid number between -90.0 and 90.0';
    }
    if (isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
      errors.longitude = 'Longitude must be a valid number between -180.0 and 180.0';
    }
    return errors;
  };

  // --- CREATE INCIDENT ---
  const handleOpenCreateModal = () => {
    setFormErrors({});
    setNewIncident({
      title: '',
      description: '',
      category: 'Flood',
      severity: 'MEDIUM',
      status: 'OPEN',
      locationName: 'Sector 17 Commercial Complex',
      latitude: 30.7333,
      longitude: 76.7794,
      source: 'CITIZEN',
    });
    setIsCreateModalOpen(true);
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    const coordErrors = validateCoordinates(newIncident.latitude, newIncident.longitude);
    const errors = { ...coordErrors };

    if (!newIncident.title?.trim()) {
      errors.title = 'Title is required';
    }
    if (!newIncident.description?.trim()) {
      errors.description = 'Description is required';
    }
    if (!newIncident.locationName?.trim()) {
      errors.locationName = 'Location name is required';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    setFormErrors({});
    try {
      const created = await incidentsApi.createIncident({
        title: newIncident.title.trim(),
        description: newIncident.description.trim(),
        category: newIncident.category,
        severity: newIncident.severity,
        status: newIncident.status,
        location_name: newIncident.locationName.trim(),
        latitude: parseFloat(newIncident.latitude),
        longitude: parseFloat(newIncident.longitude),
        source: newIncident.source,
      });

      setIsCreateModalOpen(false);
      showNotification(`Incident ${created.incidentNumber || 'created'} reported successfully.`);
      fetchIncidents();
    } catch (err) {
      setFormErrors({ submit: err.message || 'Failed to create incident.' });
    } finally {
      setSubmitting(false);
    }
  };

  // --- VIEW DETAILS ---
  const handleViewDetails = (incident) => {
    setSelectedIncident(incident);
    setIsDetailsModalOpen(true);
  };

  // --- EDIT INCIDENT ---
  const handleOpenEditModal = (incident) => {
    setSelectedIncident(incident);
    setFormErrors({});
    setEditIncident({
      title: incident.title || incident.incidentType || '',
      description: incident.description || '',
      category: incident.category || incident.incidentType || 'Flood',
      severity: incident.severity || incident.severityIndicator || 'MEDIUM',
      locationName: incident.locationName || incident.address || '',
      latitude: incident.latitude || 30.7333,
      longitude: incident.longitude || 76.7794,
    });
    setIsEditModalOpen(true);
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    const coordErrors = validateCoordinates(editIncident.latitude, editIncident.longitude);
    const errors = { ...coordErrors };

    if (!editIncident.title?.trim()) {
      errors.title = 'Title is required';
    }
    if (!editIncident.description?.trim()) {
      errors.description = 'Description is required';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    setFormErrors({});
    try {
      await incidentsApi.updateIncident(selectedIncident.id, {
        title: editIncident.title.trim(),
        description: editIncident.description.trim(),
        category: editIncident.category,
        severity: editIncident.severity,
        location_name: editIncident.locationName.trim(),
        latitude: parseFloat(editIncident.latitude),
        longitude: parseFloat(editIncident.longitude),
      });

      setIsEditModalOpen(false);
      showNotification(`Incident ${selectedIncident.incidentNumber} updated successfully.`);
      fetchIncidents();
    } catch (err) {
      setFormErrors({ submit: err.message || 'Failed to update incident.' });
    } finally {
      setSubmitting(false);
    }
  };

  // --- UPDATE STATUS ---
  const handleOpenStatusModal = (incident) => {
    setSelectedIncident(incident);
    setStatusUpdate({
      status: incident.status || 'IN_PROGRESS',
      notes: '',
    });
    setIsStatusModalOpen(true);
  };

  const handleStatusSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await incidentsApi.updateStatus(
        selectedIncident.id,
        statusUpdate.status,
        statusUpdate.notes || `Status changed to ${statusUpdate.status}`
      );
      setIsStatusModalOpen(false);
      showNotification(`Status updated to ${statusUpdate.status} for ${selectedIncident.incidentNumber}`);
      fetchIncidents();
    } catch (err) {
      alert('Failed to update status: ' + err.message);
    } finally {
      setSubmitting(false);
    }
  };

  // --- DELETE INCIDENT ---
  const handleOpenDeleteModal = (incident) => {
    setSelectedIncident(incident);
    setIsDeleteModalOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!selectedIncident) return;
    setSubmitting(true);
    try {
      await incidentsApi.deleteIncident(selectedIncident.id);
      setIsDeleteModalOpen(false);
      showNotification(`Incident ${selectedIncident.incidentNumber} permanently deleted.`);
      fetchIncidents();
    } catch (err) {
      alert('Failed to delete incident: ' + err.message);
    } finally {
      setSubmitting(false);
    }
  };

  // Reset Filters
  const handleResetFilters = () => {
    setSearch('');
    setCategoryFilter('');
    setSeverityFilter('');
    setStatusFilter('');
    setPage(0);
  };

  // Calculate Quick Metric Summary
  const countCritical = incidents.filter(
    (i) => (i.severity || i.severityIndicator || '').toUpperCase() === 'CRITICAL'
  ).length;
  const countActive = incidents.filter((i) =>
    ['OPEN', 'IN_PROGRESS', 'DETECTED', 'ASSIGNED'].includes((i.status || '').toUpperCase())
  ).length;
  const countResolved = incidents.filter((i) =>
    ['RESOLVED', 'CLOSED'].includes((i.status || '').toUpperCase())
  ).length;

  return (
    <div>
      {/* Page Header */}
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <h1>CivicPulse Incident Management</h1>
          <p>
            Real-time municipal emergency dispatch, JDBC PostgreSQL persistence, and lifecycle tracking
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <button onClick={fetchIncidents} className="btn btn-outline" title="Refresh incidents">
            <FiRefreshCw size={14} className={loading ? 'spin' : ''} />
            Refresh
          </button>
          <button onClick={handleOpenCreateModal} className="btn btn-primary">
            <FiPlus size={16} />
            Create Incident
          </button>
        </div>
      </div>

      {/* Success Notification Banner */}
      {successBanner && (
        <div
          style={{
            marginBottom: '1rem',
            padding: '0.75rem 1rem',
            backgroundColor: '#f0fdf4',
            border: '1px solid #bbf7d0',
            color: '#15803d',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            fontSize: '0.875rem',
          }}
        >
          <FiCheckCircle size={16} />
          <span>{successBanner}</span>
        </div>
      )}

      {/* Error Banner */}
      {error && (
        <div
          style={{
            marginBottom: '1rem',
            padding: '0.75rem 1rem',
            backgroundColor: '#fef2f2',
            border: '1px solid #fecaca',
            color: '#b91c1c',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '0.875rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <FiAlertTriangle size={16} />
            <span>{error}</span>
          </div>
          <button onClick={fetchIncidents} className="btn btn-outline btn-sm">
            Retry
          </button>
        </div>
      )}

      {/* Quick Summary Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
          marginBottom: '1.25rem',
        }}
      >
        <div className="cc-card" style={{ padding: '1rem' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', fontWeight: 600 }}>
            TOTAL INCIDENTS
          </div>
          <div style={{ fontSize: '1.6rem', fontWeight: 700, marginTop: '0.25rem' }}>
            {totalElements}
          </div>
        </div>
        <div className="cc-card" style={{ padding: '1rem' }}>
          <div style={{ fontSize: '0.75rem', color: '#0369a1', fontWeight: 600 }}>
            ACTIVE &amp; IN PROGRESS
          </div>
          <div style={{ fontSize: '1.6rem', fontWeight: 700, marginTop: '0.25rem', color: '#0284c7' }}>
            {countActive}
          </div>
        </div>
        <div className="cc-card" style={{ padding: '1rem' }}>
          <div style={{ fontSize: '0.75rem', color: '#15803d', fontWeight: 600 }}>
            RESOLVED / CLOSED
          </div>
          <div style={{ fontSize: '1.6rem', fontWeight: 700, marginTop: '0.25rem', color: '#16a34a' }}>
            {countResolved}
          </div>
        </div>
        <div className="cc-card" style={{ padding: '1rem' }}>
          <div style={{ fontSize: '0.75rem', color: '#b91c1c', fontWeight: 600 }}>
            CRITICAL SEVERITY
          </div>
          <div style={{ fontSize: '1.6rem', fontWeight: 700, marginTop: '0.25rem', color: '#dc2626' }}>
            {countCritical}
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="cc-filter-bar" style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div style={{ position: 'relative', flex: '1 1 240px' }}>
          <input
            type="text"
            className="cc-input"
            style={{ width: '100%', paddingLeft: '2.25rem' }}
            placeholder="Search by title, location, or incident #..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
          <FiSearch
            size={14}
            style={{ position: 'absolute', left: '10px', top: '11px', color: 'var(--text-subtle)' }}
          />
        </div>

        {/* Category Filter */}
        <select
          className="cc-select"
          value={categoryFilter}
          onChange={(e) => {
            setCategoryFilter(e.target.value);
            setPage(0);
          }}
          style={{ minWidth: '150px' }}
        >
          <option value="">All Categories</option>
          {CATEGORIES.map((cat) => (
            <option key={cat} value={cat}>
              {cat}
            </option>
          ))}
        </select>

        {/* Severity Filter */}
        <select
          className="cc-select"
          value={severityFilter}
          onChange={(e) => {
            setSeverityFilter(e.target.value);
            setPage(0);
          }}
          style={{ minWidth: '140px' }}
        >
          <option value="">All Severities</option>
          {SEVERITIES.map((sev) => (
            <option key={sev} value={sev}>
              {sev}
            </option>
          ))}
        </select>

        {/* Status Filter */}
        <select
          className="cc-select"
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPage(0);
          }}
          style={{ minWidth: '140px' }}
        >
          <option value="">All Statuses</option>
          {STATUSES.map((stat) => (
            <option key={stat} value={stat}>
              {stat}
            </option>
          ))}
        </select>

        {(search || categoryFilter || severityFilter || statusFilter) && (
          <button onClick={handleResetFilters} className="btn btn-outline btn-sm">
            Reset Filters
          </button>
        )}
      </div>

      {/* Incidents Data Table */}
      <div className="cc-card" style={{ padding: 0 }}>
        {loading ? (
          <LoadingSpinner text="Retrieving incidents via JDBC from PostgreSQL..." />
        ) : incidents.length === 0 ? (
          <EmptyState
            title="No matching incidents"
            description="No incidents match the active search or filters."
            action={
              <button onClick={handleOpenCreateModal} className="btn btn-primary btn-sm">
                <FiPlus size={13} />
                Create New Incident
              </button>
            }
          />
        ) : (
          <div className="cc-table-wrapper" style={{ border: 'none' }}>
            <table className="cc-table">
              <thead>
                <tr>
                  <th>Incident No.</th>
                  <th>Title &amp; Category</th>
                  <th>Severity</th>
                  <th>Status</th>
                  <th>Location</th>
                  <th>Reporter / Assignee</th>
                  <th>Reported Time</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {incidents.map((inc) => (
                  <tr key={inc.id}>
                    {/* Incident Number */}
                    <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: '0.85rem' }}>
                      <button
                        onClick={() => handleViewDetails(inc)}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: 'var(--accent-primary, #0284c7)',
                          cursor: 'pointer',
                          fontWeight: 600,
                          padding: 0,
                          textDecoration: 'underline',
                        }}
                      >
                        {inc.incidentNumber}
                      </button>
                    </td>

                    {/* Title & Category */}
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                        {inc.title || inc.incidentType || 'Untitled Incident'}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
                        {inc.category || inc.incidentType || 'General'}
                      </div>
                    </td>

                    {/* Severity */}
                    <td>
                      <PriorityBadge tier={inc.severity || inc.severityIndicator || inc.priority} />
                    </td>

                    {/* Status */}
                    <td>
                      <StatusBadge status={inc.status} />
                    </td>

                    {/* Location */}
                    <td style={{ fontSize: '0.775rem', color: 'var(--text-muted)', maxWidth: '180px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <FiMapPin size={12} style={{ flexShrink: 0, color: 'var(--text-subtle)' }} />
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {inc.locationName || inc.address || `${inc.latitude?.toFixed(4)}, ${inc.longitude?.toFixed(4)}`}
                        </span>
                      </div>
                    </td>

                    {/* Reporter / Assignee */}
                    <td style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
                      <div>
                        <strong>By:</strong> {inc.reportedByName || inc.createdByUsername || 'Citizen'}
                      </div>
                      {inc.assignedToName && (
                        <div>
                          <strong>To:</strong> {inc.assignedToName}
                        </div>
                      )}
                    </td>

                    {/* Created Time */}
                    <td style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
                      {inc.createdAt ? new Date(inc.createdAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : '--'}
                    </td>

                    {/* Actions */}
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.35rem', justifyContent: 'flex-end' }}>
                        {/* View Dossier */}
                        <button
                          onClick={() => handleViewDetails(inc)}
                          className="btn btn-outline btn-sm"
                          title="View Details"
                        >
                          <FiEye size={12} />
                        </button>

                        {/* Quick Update Status */}
                        {canUpdateStatus && (
                          <button
                            onClick={() => handleOpenStatusModal(inc)}
                            className="btn btn-outline btn-sm"
                            title="Update Status"
                          >
                            <FiActivity size={12} />
                          </button>
                        )}

                        {/* Edit Incident */}
                        {canEdit && (
                          <button
                            onClick={() => handleOpenEditModal(inc)}
                            className="btn btn-outline btn-sm"
                            title="Edit Incident"
                          >
                            <FiEdit2 size={12} />
                          </button>
                        )}

                        {/* Delete Incident */}
                        {isAdmin && (
                          <button
                            onClick={() => handleOpenDeleteModal(inc)}
                            className="btn btn-outline btn-sm"
                            style={{ color: '#dc2626', borderColor: '#fecaca' }}
                            title="Delete Incident"
                          >
                            <FiTrash2 size={12} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        <div
          style={{
            padding: '0.75rem 1.25rem',
            borderTop: '1px solid var(--border-light)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            fontSize: '0.8rem',
            color: 'var(--text-subtle)',
          }}
        >
          <div>
            Showing <strong>{incidents.length}</strong> of <strong>{totalElements}</strong> incidents
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="btn btn-outline btn-sm"
            >
              <FiChevronLeft size={13} />
              Previous
            </button>
            <span>
              Page {page + 1} of {Math.max(1, totalPages)}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="btn btn-outline btn-sm"
            >
              Next
              <FiChevronRight size={13} />
            </button>
          </div>
        </div>
      </div>

      {/* ======================================================== */}
      {/* MODAL 1: CREATE INCIDENT                                 */}
      {/* ======================================================== */}
      <Modal
        title="Report New Smart City Incident"
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        maxWidth="540px"
      >
        <form onSubmit={handleCreateSubmit}>
          {formErrors.submit && (
            <div
              style={{
                marginBottom: '1rem',
                padding: '0.5rem 0.75rem',
                backgroundColor: '#fef2f2',
                border: '1px solid #fecaca',
                color: '#b91c1c',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.8rem',
              }}
            >
              {formErrors.submit}
            </div>
          )}

          {/* Title */}
          <div className="form-group" style={{ marginBottom: '0.85rem' }}>
            <label className="form-label" style={{ fontWeight: 600 }}>
              Incident Title *
            </label>
            <input
              type="text"
              className="cc-input"
              style={{ width: '100%' }}
              placeholder="e.g. Flash Flood at Downtown Underpass"
              value={newIncident.title}
              onChange={(e) => setNewIncident({ ...newIncident, title: e.target.value })}
              required
            />
            {formErrors.title && (
              <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{formErrors.title}</span>
            )}
          </div>

          {/* Description */}
          <div className="form-group" style={{ marginBottom: '0.85rem' }}>
            <label className="form-label" style={{ fontWeight: 600 }}>
              Description &amp; Hazard Details *
            </label>
            <textarea
              className="cc-input"
              rows={3}
              style={{ width: '100%', resize: 'vertical' }}
              placeholder="Describe the severity, observed impact, and urgent hazards..."
              value={newIncident.description}
              onChange={(e) => setNewIncident({ ...newIncident, description: e.target.value })}
              required
            />
            {formErrors.description && (
              <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{formErrors.description}</span>
            )}
          </div>

          {/* Category & Severity Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '0.85rem' }}>
            <div className="form-group">
              <label className="form-label" style={{ fontWeight: 600 }}>
                Category *
              </label>
              <select
                className="cc-select"
                style={{ width: '100%' }}
                value={newIncident.category}
                onChange={(e) => setNewIncident({ ...newIncident, category: e.target.value })}
              >
                {CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" style={{ fontWeight: 600 }}>
                Severity *
              </label>
              <select
                className="cc-select"
                style={{ width: '100%' }}
                value={newIncident.severity}
                onChange={(e) => setNewIncident({ ...newIncident, severity: e.target.value })}
              >
                {SEVERITIES.map((sev) => (
                  <option key={sev} value={sev}>
                    {sev}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Location Name */}
          <div className="form-group" style={{ marginBottom: '0.85rem' }}>
            <label className="form-label" style={{ fontWeight: 600 }}>
              Location Name / Landmark *
            </label>
            <input
              type="text"
              className="cc-input"
              style={{ width: '100%' }}
              placeholder="e.g. Sector 17 Plaza, Chandigarh"
              value={newIncident.locationName}
              onChange={(e) => setNewIncident({ ...newIncident, locationName: e.target.value })}
              required
            />
            {formErrors.locationName && (
              <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{formErrors.locationName}</span>
            )}
          </div>

          {/* Coordinates Grid with Real-time Validation */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '0.85rem' }}>
            <div className="form-group">
              <label className="form-label" style={{ fontWeight: 600 }}>
                Latitude (-90 to 90) *
              </label>
              <input
                type="number"
                step="any"
                className="cc-input"
                style={{
                  width: '100%',
                  borderColor: formErrors.latitude ? '#dc2626' : undefined,
                }}
                value={newIncident.latitude}
                onChange={(e) => setNewIncident({ ...newIncident, latitude: e.target.value })}
                required
              />
              {formErrors.latitude && (
                <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{formErrors.latitude}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" style={{ fontWeight: 600 }}>
                Longitude (-180 to 180) *
              </label>
              <input
                type="number"
                step="any"
                className="cc-input"
                style={{
                  width: '100%',
                  borderColor: formErrors.longitude ? '#dc2626' : undefined,
                }}
                value={newIncident.longitude}
                onChange={(e) => setNewIncident({ ...newIncident, longitude: e.target.value })}
                required
              />
              {formErrors.longitude && (
                <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{formErrors.longitude}</span>
              )}
            </div>
          </div>

          {/* Quick Coordinate Fill Helper */}
          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
            <button
              type="button"
              className="btn btn-outline btn-sm"
              onClick={() => setNewIncident({ ...newIncident, latitude: 30.7333, longitude: 76.7794 })}
            >
              Set Chandigarh (30.73, 76.78)
            </button>
            <button
              type="button"
              className="btn btn-outline btn-sm"
              onClick={() => setNewIncident({ ...newIncident, latitude: 12.9716, longitude: 77.5946 })}
            >
              Set Bengaluru (12.97, 77.59)
            </button>
          </div>

          {/* Buttons */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.25rem' }}>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setIsCreateModalOpen(false)}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Persisting via JDBC...' : 'Submit Incident'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ======================================================== */}
      {/* MODAL 2: INCIDENT DETAILS / DOSSIER                      */}
      {/* ======================================================== */}
      {selectedIncident && (
        <Modal
          title={`Incident Dossier: ${selectedIncident.incidentNumber}`}
          isOpen={isDetailsModalOpen}
          onClose={() => setIsDetailsModalOpen(false)}
          maxWidth="600px"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* Badges Bar */}
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <PriorityBadge tier={selectedIncident.severity || selectedIncident.severityIndicator} />
              <StatusBadge status={selectedIncident.status} />
              <span className="badge badge-status" style={{ fontSize: '0.75rem' }}>
                {selectedIncident.category || selectedIncident.incidentType}
              </span>
            </div>

            {/* Title & Description */}
            <div>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 600, margin: '0 0 0.5rem 0' }}>
                {selectedIncident.title || selectedIncident.incidentType}
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', lineHeight: '1.5', margin: 0 }}>
                {selectedIncident.description}
              </p>
            </div>

            {/* Location & Coordinates */}
            <div
              style={{
                backgroundColor: 'var(--bg-subtle, #f8fafc)',
                padding: '0.75rem 1rem',
                borderRadius: 'var(--radius-md)',
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '0.75rem',
                fontSize: '0.825rem',
              }}
            >
              <div>
                <strong style={{ color: 'var(--text-subtle)' }}>Location Landmark:</strong>
                <div>{selectedIncident.locationName || selectedIncident.address || 'Smart City Zone'}</div>
              </div>
              <div>
                <strong style={{ color: 'var(--text-subtle)' }}>Coordinates:</strong>
                <div>
                  {selectedIncident.latitude?.toFixed(5)}, {selectedIncident.longitude?.toFixed(5)}
                </div>
              </div>
            </div>

            {/* Reporter & Timestamps */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '0.75rem',
                fontSize: '0.825rem',
              }}
            >
              <div>
                <strong style={{ color: 'var(--text-subtle)' }}>Reported By:</strong>
                <div>{selectedIncident.reportedByName || selectedIncident.createdByUsername || 'Citizen'}</div>
              </div>
              <div>
                <strong style={{ color: 'var(--text-subtle)' }}>Assigned To:</strong>
                <div>{selectedIncident.assignedToName || 'Unassigned'}</div>
              </div>
              <div>
                <strong style={{ color: 'var(--text-subtle)' }}>Created Time:</strong>
                <div>
                  {selectedIncident.createdAt ? new Date(selectedIncident.createdAt).toLocaleString() : '--'}
                </div>
              </div>
              <div>
                <strong style={{ color: 'var(--text-subtle)' }}>Last Updated:</strong>
                <div>
                  {selectedIncident.updatedAt ? new Date(selectedIncident.updatedAt).toLocaleString() : '--'}
                </div>
              </div>
            </div>

            {/* Footer Actions */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                paddingTop: '1rem',
                borderTop: '1px solid var(--border-light)',
              }}
            >
              <Link to={`/incidents/${selectedIncident.id}`} className="btn btn-outline btn-sm">
                Open Full Tactical Dossier
              </Link>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                {canUpdateStatus && (
                  <button
                    onClick={() => {
                      setIsDetailsModalOpen(false);
                      handleOpenStatusModal(selectedIncident);
                    }}
                    className="btn btn-outline btn-sm"
                  >
                    <FiActivity size={12} />
                    Update Status
                  </button>
                )}
                {canEdit && (
                  <button
                    onClick={() => {
                      setIsDetailsModalOpen(false);
                      handleOpenEditModal(selectedIncident);
                    }}
                    className="btn btn-primary btn-sm"
                  >
                    <FiEdit2 size={12} />
                    Edit
                  </button>
                )}
              </div>
            </div>
          </div>
        </Modal>
      )}

      {/* ======================================================== */}
      {/* MODAL 3: EDIT INCIDENT                                   */}
      {/* ======================================================== */}
      {selectedIncident && (
        <Modal
          title={`Edit Incident: ${selectedIncident.incidentNumber}`}
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          maxWidth="540px"
        >
          <form onSubmit={handleEditSubmit}>
            {formErrors.submit && (
              <div
                style={{
                  marginBottom: '1rem',
                  padding: '0.5rem 0.75rem',
                  backgroundColor: '#fef2f2',
                  border: '1px solid #fecaca',
                  color: '#b91c1c',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.8rem',
                }}
              >
                {formErrors.submit}
              </div>
            )}

            <div className="form-group" style={{ marginBottom: '0.85rem' }}>
              <label className="form-label" style={{ fontWeight: 600 }}>
                Incident Title *
              </label>
              <input
                type="text"
                className="cc-input"
                style={{ width: '100%' }}
                value={editIncident.title}
                onChange={(e) => setEditIncident({ ...editIncident, title: e.target.value })}
                required
              />
            </div>

            <div className="form-group" style={{ marginBottom: '0.85rem' }}>
              <label className="form-label" style={{ fontWeight: 600 }}>
                Description &amp; Hazard Details *
              </label>
              <textarea
                className="cc-input"
                rows={3}
                style={{ width: '100%', resize: 'vertical' }}
                value={editIncident.description}
                onChange={(e) => setEditIncident({ ...editIncident, description: e.target.value })}
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '0.85rem' }}>
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>
                  Category *
                </label>
                <select
                  className="cc-select"
                  style={{ width: '100%' }}
                  value={editIncident.category}
                  onChange={(e) => setEditIncident({ ...editIncident, category: e.target.value })}
                >
                  {CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>
                  Severity *
                </label>
                <select
                  className="cc-select"
                  style={{ width: '100%' }}
                  value={editIncident.severity}
                  onChange={(e) => setEditIncident({ ...editIncident, severity: e.target.value })}
                >
                  {SEVERITIES.map((sev) => (
                    <option key={sev} value={sev}>
                      {sev}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '0.85rem' }}>
              <label className="form-label" style={{ fontWeight: 600 }}>
                Location Landmark / Name
              </label>
              <input
                type="text"
                className="cc-input"
                style={{ width: '100%' }}
                value={editIncident.locationName}
                onChange={(e) => setEditIncident({ ...editIncident, locationName: e.target.value })}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '0.85rem' }}>
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>
                  Latitude (-90 to 90)
                </label>
                <input
                  type="number"
                  step="any"
                  className="cc-input"
                  style={{ width: '100%' }}
                  value={editIncident.latitude}
                  onChange={(e) => setEditIncident({ ...editIncident, latitude: e.target.value })}
                />
                {formErrors.latitude && (
                  <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{formErrors.latitude}</span>
                )}
              </div>

              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 600 }}>
                  Longitude (-180 to 180)
                </label>
                <input
                  type="number"
                  step="any"
                  className="cc-input"
                  style={{ width: '100%' }}
                  value={editIncident.longitude}
                  onChange={(e) => setEditIncident({ ...editIncident, longitude: e.target.value })}
                />
                {formErrors.longitude && (
                  <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{formErrors.longitude}</span>
                )}
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.25rem' }}>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setIsEditModalOpen(false)}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Updating...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ======================================================== */}
      {/* MODAL 4: UPDATE STATUS                                   */}
      {/* ======================================================== */}
      {selectedIncident && (
        <Modal
          title={`Update Status: ${selectedIncident.incidentNumber}`}
          isOpen={isStatusModalOpen}
          onClose={() => setIsStatusModalOpen(false)}
          maxWidth="460px"
        >
          <form onSubmit={handleStatusSubmit}>
            <div className="form-group" style={{ marginBottom: '1rem' }}>
              <label className="form-label" style={{ fontWeight: 600 }}>
                Target Lifecycle Status *
              </label>
              <select
                className="cc-select"
                style={{ width: '100%' }}
                value={statusUpdate.status}
                onChange={(e) => setStatusUpdate({ ...statusUpdate, status: e.target.value })}
              >
                {STATUSES.map((stat) => (
                  <option key={stat} value={stat}>
                    {stat}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: '1rem' }}>
              <label className="form-label" style={{ fontWeight: 600 }}>
                Transition Remarks / Notes
              </label>
              <textarea
                className="cc-input"
                rows={3}
                style={{ width: '100%', resize: 'vertical' }}
                placeholder="e.g. Field dispatch team reached location. Debris cleared."
                value={statusUpdate.notes}
                onChange={(e) => setStatusUpdate({ ...statusUpdate, notes: e.target.value })}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.25rem' }}>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setIsStatusModalOpen(false)}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Updating Status...' : 'Apply Transition'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ======================================================== */}
      {/* MODAL 5: CONFIRM DELETE                                  */}
      {/* ======================================================== */}
      {selectedIncident && (
        <Modal
          title="Confirm Permanent Deletion"
          isOpen={isDeleteModalOpen}
          onClose={() => setIsDeleteModalOpen(false)}
          maxWidth="460px"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div
              style={{
                backgroundColor: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: 'var(--radius-md)',
                padding: '0.85rem',
                color: '#b91c1c',
                display: 'flex',
                gap: '0.5rem',
                alignItems: 'flex-start',
              }}
            >
              <FiAlertTriangle size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
              <div style={{ fontSize: '0.85rem' }}>
                <strong>Warning:</strong> You are about to permanently delete incident{' '}
                <strong>{selectedIncident.incidentNumber}</strong> ({selectedIncident.title || selectedIncident.incidentType}).
                All audit trails and assignments for this incident will be removed.
              </div>
            </div>

            <p style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', margin: 0 }}>
              This operation requires Administrator privileges and cannot be undone. Are you sure you want to proceed?
            </p>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setIsDeleteModalOpen(false)}
                disabled={submitting}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeleteConfirm}
                className="btn btn-danger"
                style={{
                  backgroundColor: '#dc2626',
                  color: '#ffffff',
                  borderColor: '#dc2626',
                }}
                disabled={submitting}
              >
                {submitting ? 'Deleting...' : 'Delete Incident'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default IncidentsPage;
