import React, { useState } from 'react';
import api from '../api/client';
import PriorityBadge from './PriorityBadge';
import { Cpu, CheckCircle, ArrowRight, ShieldCheck } from 'lucide-react';

export default function RuleEvaluatorModal({ onClose, onCreated }) {
  const [formData, setFormData] = useState({
    incidentType: 'traffic_signal_failure',
    description: 'Traffic signals dark and uncoordinated at junction',
    junctionType: 'major',
    publicExposure: 'HIGH',
    infrastructureImportance: 'PRIMARY',
    sensorValue: '',
    emergencyAccessBlocked: false,
    exposed: false,
    latitude: 30.7046,
    longitude: 76.7179,
    address: 'Madhya Marg Major Intersection'
  });

  const [evaluation, setEvaluation] = useState(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleEvaluate = async (e) => {
    if (e) e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const payload = {
        ...formData,
        sensorValue: formData.sensorValue ? parseFloat(formData.sensorValue) : null,
      };
      const res = await api.post('/priority/evaluate', payload);
      setEvaluation(res.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateIncident = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const payload = {
        ...formData,
        source: 'OPERATOR',
        sensorValue: formData.sensorValue ? parseFloat(formData.sensorValue) : null,
      };
      await api.post('/incidents', payload);
      if (onCreated) onCreated();
      if (onClose) onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="panel" style={{ marginBottom: '1.5rem' }}>
      <div className="panel-header">
        <div className="panel-title">
          <Cpu size={18} color="#0284c7" />
          <span>Priority Rule Engine Evaluation Sandbox</span>
        </div>
        {onClose && (
          <button className="btn btn-outline btn-sm" onClick={onClose}>Close</button>
        )}
      </div>

      <div className="panel-body">
        <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
          Evaluate incidents against deterministic municipal rules. Priority and SLAs are never randomly assigned.
        </p>

        {error && (
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', padding: '0.6rem 0.8rem', borderRadius: '6px', fontSize: '0.8rem', marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem' }}>
          <form onSubmit={handleEvaluate}>
            <div className="form-group">
              <label className="form-label">Incident Type Preset / Description</label>
              <select
                className="form-select"
                value={formData.incidentType}
                onChange={(e) => setFormData({ ...formData, incidentType: e.target.value })}
              >
                <option value="traffic_signal_failure">Traffic Signal Failure</option>
                <option value="major_road_accident">Major Road Accident / Collision (Tier 1)</option>
                <option value="live_electrical_wire">Live Exposed Electrical Wire (Tier 1)</option>
                <option value="transformer_spark">Transformer Spark / Fire Hazard (Tier 1)</option>
                <option value="open_manhole">Open Manhole on Roadway (Tier 1)</option>
                <option value="pipeline_burst">Main Pipeline Burst (Tier 2)</option>
                <option value="toxic_smog">Severe Toxic Smog Cluster / AQI Breach (Tier 2)</option>
                <option value="waste_bin">Smart Bin Overflow &gt;90% (Tier 3)</option>
                <option value="dead_streetlight">Dead Streetlight Cluster (Tier 3)</option>
                <option value="sidewalk_repair">Sidewalk Crack / Paving Disrepair (Tier 4)</option>
                <option value="graffiti">Graffiti Removal (Tier 4)</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Location Context / Junction Type</label>
              <select
                className="form-select"
                value={formData.junctionType}
                onChange={(e) => setFormData({ ...formData, junctionType: e.target.value })}
              >
                <option value="major">Major Junction / Arterial Expressway</option>
                <option value="minor">Minor Intersection</option>
                <option value="residential">Residential Lane</option>
              </select>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label">Public Exposure</label>
                <select
                  className="form-select"
                  value={formData.publicExposure}
                  onChange={(e) => setFormData({ ...formData, publicExposure: e.target.value })}
                >
                  <option value="HIGH">HIGH (High pedestrian/traffic)</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="LOW">LOW</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Infrastructure Importance</label>
                <select
                  className="form-select"
                  value={formData.infrastructureImportance}
                  onChange={(e) => setFormData({ ...formData, infrastructureImportance: e.target.value })}
                >
                  <option value="CRITICAL">CRITICAL (Hospital/Corridor)</option>
                  <option value="PRIMARY">PRIMARY</option>
                  <option value="SECONDARY">SECONDARY</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '0.75rem' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={formData.emergencyAccessBlocked}
                  onChange={(e) => setFormData({ ...formData, emergencyAccessBlocked: e.target.checked })}
                />
                Emergency Access Blocked
              </label>

              <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={formData.exposed}
                  onChange={(e) => setFormData({ ...formData, exposed: e.target.checked })}
                />
                Hazard Exposed / Unprotected
              </label>
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Evaluating Rules...' : 'Execute Deterministic Rule Engine'}
            </button>
          </form>

          {/* Evaluation Result View */}
          <div style={{
            background: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-sm)',
            padding: '1.25rem',
            border: '1px solid var(--border-subtle)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between'
          }}>
            {evaluation ? (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)' }}>RULE ENGINE EVALUATION</span>
                  <PriorityBadge
                    tier={evaluation.tier}
                    priority={evaluation.priority}
                    slaMinutes={evaluation.slaMinutes}
                  />
                </div>

                <div style={{ marginBottom: '0.75rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>ASSIGNED TARGET SLA:</div>
                  <div style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'var(--font-mono)' }}>
                    {evaluation.slaMinutes} Minutes ({Math.round(evaluation.slaMinutes / 60 * 10) / 10} hours)
                  </div>
                </div>

                <div style={{ marginBottom: '0.75rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>RECOMMENDED DEPARTMENT:</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600 }}>{evaluation.recommendedDepartment}</div>
                </div>

                <div style={{ marginBottom: '0.75rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>DETERMINISTIC REASONS:</div>
                  <ul style={{ paddingLeft: '1.2rem', fontSize: '0.8rem', color: 'var(--text-main)', marginTop: '0.2rem' }}>
                    {evaluation.reasons?.map((r, i) => (
                      <li key={i}>{r}</li>
                    ))}
                  </ul>
                </div>

                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>RECOMMENDED ACTIONS:</div>
                  <ul style={{ paddingLeft: '1.2rem', fontSize: '0.8rem', color: 'var(--text-main)', marginTop: '0.2rem' }}>
                    {evaluation.recommendedActions?.map((a, i) => (
                      <li key={i}>{a}</li>
                    ))}
                  </ul>
                </div>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-muted)', textAlign: 'center' }}>
                <ShieldCheck size={36} color="#94a3b8" style={{ marginBottom: '0.5rem' }} />
                <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>Awaiting Parameter Input</div>
                <div style={{ fontSize: '0.75rem' }}>Select contextual variables and click Execute to view the rule evaluation result.</div>
              </div>
            )}

            {evaluation && (
              <button
                className="btn btn-outline"
                style={{ marginTop: '1rem', width: '100%', borderColor: 'var(--primary-accent)', color: 'var(--primary-accent)' }}
                onClick={handleCreateIncident}
                disabled={submitting}
              >
                {submitting ? 'Creating Incident...' : 'Commit as Official City Incident'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
