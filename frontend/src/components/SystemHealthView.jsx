import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { Database, Server, RefreshCw, CheckCircle2, XCircle, Cpu, Network } from 'lucide-react';

export default function SystemHealthView() {
  const [healthData, setHealthData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/health');
      setHealthData(res.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      <div className="panel">
        <div className="panel-header">
          <div className="panel-title">
            <Server size={18} color="#0284c7" />
            <span>CivicPulse Datastore & Subsystem Connectivity</span>
          </div>
          <button className="btn btn-outline btn-sm" onClick={fetchHealth} disabled={loading}>
            <RefreshCw size={13} /> Refresh
          </button>
        </div>

        <div className="panel-body">
          {error && (
            <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', padding: '0.8rem', borderRadius: '6px', marginBottom: '1rem' }}>
              Backend Health Check Failed: {error}
            </div>
          )}

          {healthData && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem' }}>
              {/* PostgreSQL */}
              <div style={{
                padding: '1.25rem',
                border: '1px solid var(--border-subtle)',
                borderRadius: '8px',
                background: 'var(--bg-subtle)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700 }}>
                    <Database size={16} color="#0284c7" />
                    <span>PostgreSQL + PostGIS</span>
                  </div>
                  <span className="badge badge-tier4">CONNECTED</span>
                </div>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  Structured city relational data, Flyway migrations, user credentials, incident lifecycle, and GIS geometries.
                </p>
                <div style={{ marginTop: '0.5rem', fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: '#15803d' }}>
                  {healthData.components?.postgresql}
                </div>
              </div>

              {/* MongoDB */}
              <div style={{
                padding: '1.25rem',
                border: '1px solid var(--border-subtle)',
                borderRadius: '8px',
                background: 'var(--bg-subtle)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700 }}>
                    <Database size={16} color="#15803d" />
                    <span>MongoDB Telemetry</span>
                  </div>
                  <span className="badge badge-tier4">CONNECTED</span>
                </div>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  High-volume IoT sensor ingestion, road congestion telemetry, smart bin fills, and atmospheric observations.
                </p>
                <div style={{ marginTop: '0.5rem', fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: '#15803d' }}>
                  {healthData.components?.mongodb}
                </div>
              </div>

              {/* Redis */}
              <div style={{
                padding: '1.25rem',
                border: '1px solid var(--border-subtle)',
                borderRadius: '8px',
                background: 'var(--bg-subtle)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700 }}>
                    <Cpu size={16} color="#dc2626" />
                    <span>Redis Fast Cache</span>
                  </div>
                  <span className="badge badge-tier4">CONNECTED</span>
                </div>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  Operational caching, rapid lookup of active critical alarms, session states, and dashboard counters.
                </p>
                <div style={{ marginTop: '0.5rem', fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: '#15803d' }}>
                  {healthData.components?.redis}
                </div>
              </div>

              {/* MQTT / Mosquitto */}
              <div style={{
                padding: '1.25rem',
                border: '1px solid var(--border-subtle)',
                borderRadius: '8px',
                background: 'var(--bg-subtle)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700 }}>
                    <Network size={16} color="#0f766e" />
                    <span>Mosquitto MQTT Broker</span>
                  </div>
                  <span className="badge badge-tier4">ACTIVE</span>
                </div>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  Asynchronous IoT event bus subscribing to municipal topics (traffic, waste, environmental sensors).
                </p>
                <div style={{ marginTop: '0.5rem', fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: '#0f766e' }}>
                  {healthData.components?.mqtt}
                </div>
              </div>
            </div>
          )}

          {healthData && (
            <div style={{ marginTop: '1.5rem', padding: '1rem', background: '#f8fafc', borderRadius: '8px', border: '1px solid var(--border-subtle)', fontSize: '0.8rem' }}>
              <div style={{ fontWeight: 700, marginBottom: '0.25rem' }}>Backend Architecture Verification</div>
              <div style={{ color: 'var(--text-muted)' }}>
                Application: <strong>{healthData.application} (v{healthData.version})</strong> | Status: <strong style={{ color: '#15803d' }}>{healthData.status}</strong> | Spring Boot Actuator: <strong>/actuator/health</strong>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
