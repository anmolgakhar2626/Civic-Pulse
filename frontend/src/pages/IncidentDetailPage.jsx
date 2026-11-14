import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { incidentsApi } from '../api/incidentsApi';
import { slaApi } from '../api/slaApi';
import { gisApi } from '../api/gisApi';
import PriorityBadge from '../components/PriorityBadge';
import StatusBadge from '../components/StatusBadge';
import SlaTimer from '../components/SlaTimer';
import LiveMap from '../components/LiveMap';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  FiArrowLeft,
  FiMapPin,
  FiClock,
  FiShield,
  FiCheckSquare,
  FiActivity,
  FiPlay,
  FiCheck,
  FiXCircle,
  FiCpu,
} from 'react-icons/fi';

export const IncidentDetailPage = () => {
  const { id } = useParams();
  const [incident, setIncident] = useState(null);
  const [sla, setSla] = useState(null);
  const [actions, setActions] = useState([]);
  const [nearbyResources, setNearbyResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [statusNote, setStatusNote] = useState('');

  const loadIncidentDossier = async () => {
    try {
      setLoading(true);
      const [incRes, slaRes, actRes] = await Promise.allSettled([
        incidentsApi.getIncidentById(id),
        slaApi.getSlaByIncidentId(id),
        incidentsApi.getResponseActions(id),
      ]);

      let loadedInc = null;
      if (incRes.status === 'fulfilled') {
        loadedInc = incRes.value?.data || incRes.value;
        setIncident(loadedInc);
      }
      if (slaRes.status === 'fulfilled') {
        setSla(slaRes.value?.data || slaRes.value);
      }
      if (actRes.status === 'fulfilled') {
        const raw = actRes.value?.data || actRes.value || [];
        setActions(Array.isArray(raw) ? raw : []);
      }

      // If coordinates present, load nearby emergency resources using PostGIS spatial API
      if (loadedInc?.latitude && loadedInc?.longitude) {
        try {
          const resGeo = await gisApi.getNearbyResources(loadedInc.latitude, loadedInc.longitude, 10000);
          setNearbyResources(resGeo.features || []);
        } catch (e) {
          console.warn('Could not query nearby GIS resources', e);
        }
      }
    } catch (err) {
      console.error('Failed to load incident detail', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadIncidentDossier();
  }, [id]);

  const handleStatusChange = async (newStatus) => {
    try {
      setActionLoading(true);
      await incidentsApi.updateStatus(id, newStatus, statusNote || `Status updated to ${newStatus}`);
      setStatusNote('');
      await loadIncidentDossier();
    } catch (err) {
      alert('Failed to update status: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleEvaluatePriority = async () => {
    try {
      setActionLoading(true);
      await incidentsApi.evaluatePriority(id);
      await loadIncidentDossier();
    } catch (err) {
      alert('Priority evaluation failed: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleAutoAssign = async () => {
    try {
      setActionLoading(true);
      await incidentsApi.autoAssign(id);
      await loadIncidentDossier();
    } catch (err) {
      alert('Auto-assignment failed: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleGeneratePlan = async () => {
    try {
      setActionLoading(true);
      await incidentsApi.generateResponsePlan(id);
      await loadIncidentDossier();
    } catch (err) {
      alert('Response plan generation failed: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleActionToggle = async (actionId, currentStatus) => {
    const nextStatus = currentStatus === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    try {
      await incidentsApi.updateActionStatus(actionId, nextStatus, 'Toggled from command centre');
      setActions((prev) =>
        prev.map((a) => (a.id === actionId ? { ...a, status: nextStatus } : a))
      );
    } catch (err) {
      alert('Failed to update action status: ' + err.message);
    }
  };

  if (loading) {
    return <LoadingSpinner text="Retrieving operational incident dossier..." />;
  }

  if (!incident) {
    return (
      <div>
        <Link to="/incidents" className="btn btn-outline btn-sm" style={{ marginBottom: '1rem' }}>
          <FiArrowLeft size={13} /> Back to Incidents
        </Link>
        <div className="cc-card">
          <p>Incident record not found for ID: {id}</p>
        </div>
      </div>
    );
  }

  const mapPoint = incident.latitude && incident.longitude ? [incident.latitude, incident.longitude] : [30.7333, 76.7794];

  return (
    <div>
      {/* Navigation header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <Link to="/incidents" className="btn btn-outline btn-sm">
          <FiArrowLeft size={13} /> Back to Incidents
        </Link>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            onClick={handleEvaluatePriority}
            className="btn btn-outline btn-sm"
            disabled={actionLoading}
            title="Re-run priority rule engine"
          >
            <FiCpu size={13} /> Re-evaluate Priority
          </button>
          <button
            onClick={handleAutoAssign}
            className="btn btn-outline btn-sm"
            disabled={actionLoading}
            title="Run department auto-assignment engine"
          >
            <FiShield size={13} /> Auto Assign
          </button>
          <button
            onClick={handleGeneratePlan}
            className="btn btn-outline btn-sm"
            disabled={actionLoading}
            title="Generate standard response protocol actions"
          >
            <FiCheckSquare size={13} /> Generate Response Plan
          </button>
        </div>
      </div>

      {/* Main Dossier Header Card */}
      <div className="cc-card" style={{ marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', marginBottom: '0.35rem' }}>
              <h1 style={{ fontSize: '1.35rem', fontWeight: 700, color: 'var(--text-main)' }}>
                {incident.incidentNumber}
              </h1>
              <PriorityBadge tier={incident.tier || incident.priorityTier} />
              <StatusBadge status={incident.status} />
            </div>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              {incident.incidentType} &bull; Reported via <strong>{incident.source}</strong> on{' '}
              {incident.createdAt ? new Date(incident.createdAt).toLocaleString() : '--'}
            </p>
          </div>

          {/* Quick Status Workflow Action Buttons */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '0.5rem' }}>
            <div style={{ display: 'flex', gap: '0.4rem' }}>
              {incident.status !== 'IN_PROGRESS' && incident.status !== 'RESOLVED' && incident.status !== 'CLOSED' && (
                <button
                  onClick={() => handleStatusChange('IN_PROGRESS')}
                  disabled={actionLoading}
                  className="btn btn-primary btn-sm"
                >
                  <FiPlay size={12} /> Set In Progress
                </button>
              )}
              {incident.status !== 'RESOLVED' && incident.status !== 'CLOSED' && (
                <button
                  onClick={() => handleStatusChange('RESOLVED')}
                  disabled={actionLoading}
                  className="btn btn-sm"
                  style={{ backgroundColor: '#15803d', color: '#ffffff' }}
                >
                  <FiCheck size={12} /> Resolve Incident
                </button>
              )}
              {incident.status === 'RESOLVED' && (
                <button
                  onClick={() => handleStatusChange('CLOSED')}
                  disabled={actionLoading}
                  className="btn btn-outline btn-sm"
                >
                  <FiCheck size={12} /> Close Dossier
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Incident Description */}
        <div style={{ marginTop: '1rem', padding: '0.85rem 1rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '6px', fontSize: '0.825rem', color: 'var(--text-main)' }}>
          <strong>Incident Overview: </strong>
          {incident.description || 'No description logged.'}
        </div>
      </div>

      {/* Grid: Details, SLA, Assignment, GIS Map */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '1.25rem', marginBottom: '1.25rem' }}>
        {/* Left Column: Metadata, SLA, Assignment */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Priority & SLA Card */}
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiClock size={15} color="var(--accent-primary)" />
                Priority & SLA Engine Status
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', fontSize: '0.8rem' }}>
              <div>
                <span style={{ color: 'var(--text-subtle)', display: 'block', marginBottom: '2px' }}>Calculated Priority</span>
                <PriorityBadge tier={incident.tier || incident.priorityTier} />
                <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Rule ID: <strong>{incident.priorityRuleId || incident.priorityRuleCode || 'ENGINE_DEFAULT'}</strong>
                </div>
              </div>

              <div>
                <span style={{ color: 'var(--text-subtle)', display: 'block', marginBottom: '2px' }}>SLA Target Time</span>
                <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>
                  {incident.slaMinutes != null ? `${incident.slaMinutes} minutes` : '30 mins (Tier 1)'}
                </span>
                <div style={{ marginTop: '4px' }}>
                  <SlaTimer
                    status={sla?.currentStatus || 'ON_TRACK'}
                    remainingMinutes={sla?.remainingMinutes}
                  />
                </div>
              </div>
            </div>

            {incident.priorityReason && (
              <div style={{ marginTop: '0.85rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-light)', fontSize: '0.775rem' }}>
                <strong style={{ color: 'var(--text-muted)' }}>Priority Derivation Rationale:</strong>
                <p style={{ color: 'var(--text-main)', marginTop: '2px' }}>{incident.priorityReason}</p>
              </div>
            )}
          </div>

          {/* Department Assignment Card */}
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiShield size={15} color="var(--accent-teal)" />
                Department & Field Team Assignment
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', fontSize: '0.8rem' }}>
              <div>
                <span style={{ color: 'var(--text-subtle)', display: 'block', marginBottom: '2px' }}>Assigned Department</span>
                <strong style={{ color: 'var(--text-main)', fontSize: '0.85rem' }}>
                  {incident.assignedDepartment?.name || incident.departmentCode || 'Not Assigned'}
                </strong>
              </div>

              <div>
                <span style={{ color: 'var(--text-subtle)', display: 'block', marginBottom: '2px' }}>Response Unit / Team</span>
                <strong style={{ color: 'var(--text-main)', fontSize: '0.85rem' }}>
                  {incident.assignedTeam?.name || incident.assignedTeamCode || 'Standard Rapid Response'}
                </strong>
              </div>
            </div>
          </div>

          {/* Response Actions Checklist */}
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiCheckSquare size={15} color="var(--accent-primary)" />
                Protocol Response Actions ({actions.length})
              </span>
            </div>

            {actions.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '1rem', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                No response protocol actions dispatched.
                <div style={{ marginTop: '0.5rem' }}>
                  <button onClick={handleGeneratePlan} className="btn btn-outline btn-sm">
                    Generate Response Protocol
                  </button>
                </div>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {actions.map((act) => (
                  <div
                    key={act.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.5rem 0.75rem',
                      borderRadius: '6px',
                      backgroundColor: act.status === 'COMPLETED' ? '#f0fdf4' : 'var(--bg-subtle)',
                      border: `1px solid ${act.status === 'COMPLETED' ? '#bbf7d0' : 'var(--border-light)'}`,
                      fontSize: '0.8rem',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <input
                        type="checkbox"
                        checked={act.status === 'COMPLETED'}
                        onChange={() => handleActionToggle(act.id, act.status)}
                        style={{ cursor: 'pointer' }}
                      />
                      <span style={{ textDecoration: act.status === 'COMPLETED' ? 'line-through' : 'none', color: act.status === 'COMPLETED' ? '#15803d' : 'var(--text-main)', fontWeight: 500 }}>
                        {act.actionType?.replace(/_/g, ' ') || act.title || 'Response Action'}
                      </span>
                    </div>

                    <span className="badge badge-status" style={{ fontSize: '0.675rem' }}>
                      {act.status}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: GIS Location & Nearby Resources */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiMapPin size={15} color="var(--accent-primary)" />
                Incident Geolocation & PostGIS Nearby Units
              </span>
            </div>

            <div style={{ fontSize: '0.775rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
              Address: <strong>{incident.address || 'Smart City Municipal Grid'}</strong><br />
              Coordinates: {incident.latitude?.toFixed(4)}, {incident.longitude?.toFixed(4)}
            </div>

            <LiveMap
              center={mapPoint}
              zoom={13}
              height="300px"
              incidents={[incident]}
              emergencyResources={nearbyResources}
              showControls={false}
              radiusQuery={{
                lat: incident.latitude,
                lon: incident.longitude,
                radiusMeters: 5000,
              }}
            />

            {nearbyResources.length > 0 && (
              <div style={{ marginTop: '0.85rem' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                  Nearest Emergency Facilities (within 10km):
                </span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', marginTop: '0.35rem' }}>
                  {nearbyResources.slice(0, 4).map((r) => {
                    const props = r.properties || r;
                    return (
                      <div
                        key={props.id || props.name}
                        style={{
                          fontSize: '0.75rem',
                          display: 'flex',
                          justifyContent: 'space-between',
                          padding: '4px 8px',
                          backgroundColor: 'var(--bg-subtle)',
                          borderRadius: '4px',
                        }}
                      >
                        <span style={{ fontWeight: 500 }}>{props.name} ({props.resourceType})</span>
                        <span style={{ color: 'var(--accent-teal)', fontWeight: 600 }}>
                          {props.contactNumber || '112'}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default IncidentDetailPage;
