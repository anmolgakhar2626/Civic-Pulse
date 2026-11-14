import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import {
  FiSettings,
  FiServer,
  FiUser,
  FiCheckCircle,
  FiShield,
  FiDatabase,
  FiRadio,
} from 'react-icons/fi';

export const SettingsPage = () => {
  const { user } = useAuth();
  const { connected } = useWebSocket();

  return (
    <div>
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <h1>System Configuration & Operations Profile</h1>
          <p>Connected city backend services, environmental threshold rules, and operator credentials</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '1.25rem' }}>
        {/* Backend & Integration Status */}
        <div className="cc-card">
          <div className="cc-card-header">
            <span className="cc-card-title">
              <FiServer size={15} color="var(--accent-primary)" />
              Municipal Data Layer Connectivity
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '6px' }}>
              <div>
                <strong>PostgreSQL 16 + PostGIS Spatial Engine</strong>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Earthdistance GiST Spatial Indexes & Spatial Queries</div>
              </div>
              <span className="badge badge-low">CONNECTED</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '6px' }}>
              <div>
                <strong>MongoDB 8.0 Telemetry Store</strong>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Historical Sensor Readings & Observations Collection</div>
              </div>
              <span className="badge badge-low">CONNECTED</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '6px' }}>
              <div>
                <strong>Redis 8.6 In-Memory Cache</strong>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Weather & Air Quality Observation Caching (10m TTL)</div>
              </div>
              <span className="badge badge-low">CONNECTED</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '6px' }}>
              <div>
                <strong>Mosquitto MQTT 2.0 Broker</strong>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Real-time sensor telemetry ingestion on port 1883</div>
              </div>
              <span className="badge badge-low">LISTENING</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '6px' }}>
              <div>
                <strong>STOMP WebSocket Feed (/ws)</strong>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Topics: /topic/incidents, /topic/sensors, /topic/sla, /topic/alerts</div>
              </div>
              <span className={`badge ${connected ? 'badge-low' : 'badge-critical'}`}>
                {connected ? 'BROADCASTING' : 'OFFLINE'}
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '6px' }}>
              <div>
                <strong>Open-Meteo REST Client</strong>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Weather & Air Quality Live External APIs with Timeout Handling</div>
              </div>
              <span className="badge badge-low">OPERATIONAL</span>
            </div>
          </div>
        </div>

        {/* User Profile & Role Capabilities */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiUser size={15} color="var(--accent-teal)" />
                Operator Session Profile
              </span>
            </div>

            <div style={{ fontSize: '0.8rem' }}>
              <div style={{ marginBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-subtle)' }}>Logged-in Username:</span>{' '}
                <strong>{user?.username || 'admin'}</strong>
              </div>

              <div style={{ marginBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-subtle)' }}>Assigned RBAC Roles:</span>{' '}
                <span className="badge badge-status" style={{ fontWeight: 600 }}>
                  {user?.roles?.join(', ') || 'ROLE_ADMIN'}
                </span>
              </div>

              <div style={{ marginBottom: '0.5rem' }}>
                <span style={{ color: 'var(--text-subtle)' }}>Security Authentication:</span>{' '}
                <strong>Stateless JWT (HMAC-SHA256)</strong>
              </div>
            </div>
          </div>

          {/* Configured Safety Thresholds */}
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiShield size={15} color="var(--accent-primary)" />
                Active Safety Threshold Rules
              </span>
            </div>

            <div style={{ fontSize: '0.775rem', display: 'flex', flexDirection: 'column', gap: '0.4rem', color: 'var(--text-muted)' }}>
              <div>&bull; PM2.5 Critical Spike: <strong>&gt; 300 µg/m³</strong> (Tier 1 Escalation)</div>
              <div>&bull; PM2.5 Severe Smog: <strong>&gt; 150 µg/m³</strong> (Tier 2 High)</div>
              <div>&bull; Water Level Flood Warning: <strong>&gt; 3.0 meters</strong> (Tier 1 Critical)</div>
              <div>&bull; Waste Bin Overflow: <strong>&ge; 90% capacity</strong> (Tier 3 Medium)</div>
              <div>&bull; Waste Bin Emergency Obstruction: <strong>&ge; 95% on primary corridor</strong> (Tier 1)</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;
