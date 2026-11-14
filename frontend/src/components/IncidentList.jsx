import React, { useState } from 'react';
import PriorityBadge from './PriorityBadge';
import api from '../api/client';
import { AlertCircle, CheckCircle, ArrowRight, Eye, RefreshCw, Filter } from 'lucide-react';

export default function IncidentList({ incidents, onRefresh }) {
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [filterTier, setFilterTier] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [updatingId, setUpdatingId] = useState(null);

  const handleStatusChange = async (incidentId, newStatus) => {
    setUpdatingId(incidentId);
    try {
      await api.put(`/incidents/${incidentId}/status`, {
        status: newStatus,
        notes: `Operator transitioned status to ${newStatus}`,
        resolutionNotes: newStatus === 'RESOLVED' ? 'Field repair verified and completed' : null
      });
      if (onRefresh) onRefresh();
    } catch (err) {
      alert('Failed to update status: ' + err.message);
    } finally {
      setUpdatingId(null);
    }
  };

  const filteredIncidents = incidents.filter(inc => {
    if (filterTier !== 'ALL' && inc.assignedTier !== filterTier) return false;
    if (filterStatus !== 'ALL' && inc.currentStatus !== filterStatus) return false;
    return true;
  });

  return (
    <div className="panel">
      <div className="panel-header">
        <div className="panel-title">
          <AlertCircle size={18} color="#0284c7" />
          <span>Active City Incidents & SLA Monitor</span>
          <span style={{ fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-muted)' }}>
            ({filteredIncidents.length} active)
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <select
            className="form-select"
            style={{ width: 'auto', padding: '0.25rem 0.6rem', fontSize: '0.75rem' }}
            value={filterTier}
            onChange={(e) => setFilterTier(e.target.value)}
          >
            <option value="ALL">All Tiers</option>
            <option value="TIER_1">Tier 1 - Critical (≤30m)</option>
            <option value="TIER_2">Tier 2 - High (2-4h)</option>
            <option value="TIER_3">Tier 3 - Medium (12-24h)</option>
            <option value="TIER_4">Tier 4 - Low (48-72h)</option>
          </select>

          <select
            className="form-select"
            style={{ width: 'auto', padding: '0.25rem 0.6rem', fontSize: '0.75rem' }}
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="CLASSIFIED">Classified</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
          </select>

          <button className="btn btn-outline btn-sm" onClick={onRefresh}>
            <RefreshCw size={13} />
          </button>
        </div>
      </div>

      <div className="data-table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID & Source</th>
              <th>Incident & Location</th>
              <th>Priority Tier & SLA</th>
              <th>Department</th>
              <th>Lifecycle Status</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredIncidents.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                  No active incidents matching the selected criteria.
                </td>
              </tr>
            ) : (
              filteredIncidents.map((inc) => (
                <tr key={inc.id}>
                  <td>
                    <div style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: '0.78rem' }}>
                      {inc.incidentNumber}
                    </div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                      Src: {inc.source}
                    </div>
                  </td>

                  <td>
                    <div style={{ fontWeight: 600 }}>{inc.incidentType.replace(/_/g, ' ')}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {inc.address || `${inc.latitude.toFixed(4)}, ${inc.longitude.toFixed(4)}`}
                    </div>
                  </td>

                  <td>
                    <PriorityBadge
                      tier={inc.assignedTier}
                      priority={inc.priority}
                      slaMinutes={inc.slaMinutes}
                      remainingSeconds={inc.remainingSeconds}
                      slaStatus={inc.slaStatus}
                    />
                  </td>

                  <td>
                    <div style={{ fontSize: '0.8rem', fontWeight: 500 }}>
                      {inc.departmentName || 'Pending Assignment'}
                    </div>
                    {inc.assignedTeam && (
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                        Team: {inc.assignedTeam}
                      </div>
                    )}
                  </td>

                  <td>
                    <span className="badge badge-status">
                      {inc.currentStatus}
                    </span>
                  </td>

                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.35rem' }}>
                      <button
                        className="btn btn-outline btn-sm"
                        title="Inspect Incident Details"
                        onClick={() => setSelectedIncident(inc)}
                      >
                        <Eye size={13} />
                      </button>

                      {inc.currentStatus === 'CLASSIFIED' && (
                        <button
                          className="btn btn-primary btn-sm"
                          disabled={updatingId === inc.id}
                          onClick={() => handleStatusChange(inc.id, 'ACKNOWLEDGED')}
                        >
                          Acknowledge
                        </button>
                      )}

                      {inc.currentStatus === 'ACKNOWLEDGED' && (
                        <button
                          className="btn btn-primary btn-sm"
                          disabled={updatingId === inc.id}
                          onClick={() => handleStatusChange(inc.id, 'IN_PROGRESS')}
                        >
                          Dispatch Team
                        </button>
                      )}

                      {inc.currentStatus === 'IN_PROGRESS' && (
                        <button
                          className="btn btn-outline btn-sm"
                          style={{ color: '#15803d', borderColor: '#bbf7d0' }}
                          disabled={updatingId === inc.id}
                          onClick={() => handleStatusChange(inc.id, 'RESOLVED')}
                        >
                          Resolve
                        </button>
                      )}

                      {inc.currentStatus === 'RESOLVED' && (
                        <button
                          className="btn btn-outline btn-sm"
                          disabled={updatingId === inc.id}
                          onClick={() => handleStatusChange(inc.id, 'CLOSED')}
                        >
                          Verify & Close
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Incident Details Modal */}
      {selectedIncident && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(15, 23, 42, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100,
          padding: '1rem'
        }}>
          <div style={{
            background: 'white',
            borderRadius: '12px',
            maxWidth: '600px',
            width: '100%',
            padding: '1.5rem',
            boxShadow: 'var(--shadow-md)',
            maxHeight: '90vh',
            overflowY: 'auto'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div>
                <span className="badge badge-status" style={{ marginRight: '0.5rem' }}>
                  {selectedIncident.currentStatus}
                </span>
                <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700 }}>
                  {selectedIncident.incidentNumber}
                </span>
              </div>
              <button className="btn btn-outline btn-sm" onClick={() => setSelectedIncident(null)}>✕</button>
            </div>

            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.5rem' }}>
              {selectedIncident.incidentType.replace(/_/g, ' ')}
            </h3>

            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
              {selectedIncident.description}
            </p>

            <div style={{ background: 'var(--bg-subtle)', padding: '1rem', borderRadius: '8px', marginBottom: '1rem', border: '1px solid var(--border-subtle)' }}>
              <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                PRIORITY RULE ENGINE CLASSIFICATION
              </div>
              <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '0.5rem' }}>
                <PriorityBadge
                  tier={selectedIncident.assignedTier}
                  priority={selectedIncident.priority}
                  slaMinutes={selectedIncident.slaMinutes}
                  remainingSeconds={selectedIncident.remainingSeconds}
                  slaStatus={selectedIncident.slaStatus}
                />
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Target Resolution SLA:</div>
                  <div style={{ fontWeight: 700, fontFamily: 'var(--font-mono)' }}>
                    {selectedIncident.slaMinutes} Minutes
                  </div>
                </div>
              </div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-main)' }}>
                <strong>Deterministic Reasons:</strong> {selectedIncident.reasonNotes || 'Standard rule profile match'}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', fontSize: '0.8rem', marginBottom: '1.25rem' }}>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Department:</span>{' '}
                <strong>{selectedIncident.departmentName || 'Not Assigned'}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Source:</span>{' '}
                <strong>{selectedIncident.source}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Coordinates:</span>{' '}
                <span style={{ fontFamily: 'var(--font-mono)' }}>
                  {selectedIncident.latitude.toFixed(4)}, {selectedIncident.longitude.toFixed(4)}
                </span>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Address:</span>{' '}
                <span>{selectedIncident.address || 'N/A'}</span>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
              <button className="btn btn-outline" onClick={() => setSelectedIncident(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
