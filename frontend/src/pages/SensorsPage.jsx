import React, { useState, useEffect } from 'react';
import { sensorsApi } from '../api/sensorsApi';
import { useWebSocket } from '../context/WebSocketContext';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts';
import {
  FiActivity,
  FiZap,
  FiAlertTriangle,
  FiCheckCircle,
  FiRefreshCw,
  FiSliders,
} from 'react-icons/fi';

export const SensorsPage = () => {
  const [sensors, setSensors] = useState([]);
  const [selectedSensor, setSelectedSensor] = useState(null);
  const [readings, setReadings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [readingsLoading, setReadingsLoading] = useState(false);
  const [spikeValue, setSpikeValue] = useState(350);
  const [spiking, setSpiking] = useState(false);

  const { latestSensorEvent } = useWebSocket();

  const fetchSensors = async () => {
    try {
      setLoading(true);
      const res = await sensorsApi.getSensors();
      const raw = res.data || res || [];
      const list = Array.isArray(raw) ? raw : [];
      setSensors(list);
      if (list.length > 0 && !selectedSensor) {
        setSelectedSensor(list[0]);
      }
    } catch (err) {
      console.error('Failed to load sensors', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchReadings = async (sensorId) => {
    if (!sensorId) return;
    try {
      setReadingsLoading(true);
      const res = await sensorsApi.getSensorReadings(sensorId, 40);
      const raw = res.data || res || [];
      const formatted = (Array.isArray(raw) ? raw : []).map((r) => ({
        time: new Date(r.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        value: r.value,
        unit: r.unit,
      }));
      setReadings(formatted.reverse());
    } catch (err) {
      console.warn('Could not fetch historical readings', err);
    } finally {
      setReadingsLoading(false);
    }
  };

  useEffect(() => {
    fetchSensors();
  }, []);

  useEffect(() => {
    if (selectedSensor) {
      fetchReadings(selectedSensor.sensorId);
    }
  }, [selectedSensor]);

  // Real-time sensor stream
  useEffect(() => {
    if (latestSensorEvent) {
      setSensors((prev) =>
        prev.map((s) => {
          if (s.sensorId === latestSensorEvent.sensorId) {
            return {
              ...s,
              lastValue: latestSensorEvent.value,
              unit: latestSensorEvent.unit || s.unit,
              status: latestSensorEvent.status || s.status,
              lastSeenAt: latestSensorEvent.timestamp,
            };
          }
          return s;
        })
      );

      if (selectedSensor && selectedSensor.sensorId === latestSensorEvent.sensorId) {
        setReadings((prev) => [
          ...prev.slice(1),
          {
            time: new Date(latestSensorEvent.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            value: latestSensorEvent.value,
            unit: latestSensorEvent.unit || selectedSensor.unit,
          },
        ]);
      }
    }
  }, [latestSensorEvent]);

  const handleTriggerSpike = async () => {
    if (!selectedSensor) return;
    try {
      setSpiking(true);
      await sensorsApi.triggerSpike(selectedSensor.sensorId, parseFloat(spikeValue));
      alert(`Threshold breach spike injected into ${selectedSensor.sensorId}! Watch for automated incident & SLA.`);
    } catch (err) {
      alert('Spike simulation failed: ' + err.message);
    } finally {
      setSpiking(false);
    }
  };

  return (
    <div>
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <h1>IoT Environmental & Infrastructure Sensor Grid</h1>
          <p>Real-time telemetry streams from MQTT Mosquitto broker & MongoDB persistence</p>
        </div>

        <button onClick={fetchSensors} className="btn btn-outline btn-sm">
          <FiRefreshCw size={13} /> Refresh Sensors
        </button>
      </div>

      {loading ? (
        <LoadingSpinner text="Connecting to IoT telemetry nodes..." />
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '1.25rem' }}>
          {/* Left Column: Sensor Grid List */}
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiActivity size={15} color="var(--accent-primary)" />
                Active Sensors Grid ({sensors.length})
              </span>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Select to inspect telemetry</span>
            </div>

            {sensors.length === 0 ? (
              <EmptyState title="No active sensors" description="No telemetry devices registered." />
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))', gap: '0.75rem' }}>
                {sensors.map((sensor) => {
                  const isSelected = selectedSensor?.sensorId === sensor.sensorId;
                  const isOnline = sensor.status === 'ONLINE';

                  return (
                    <div
                      key={sensor.sensorId}
                      onClick={() => setSelectedSensor(sensor)}
                      style={{
                        padding: '0.85rem',
                        borderRadius: '6px',
                        backgroundColor: isSelected ? '#f0f9ff' : 'var(--bg-subtle)',
                        border: `1px solid ${isSelected ? 'var(--accent-primary)' : 'var(--border-light)'}`,
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                        <span style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-subtle)' }}>
                          {sensor.sensorType}
                        </span>
                        <span
                          className={`badge ${isOnline ? 'badge-low' : 'badge-critical'}`}
                          style={{ fontSize: '0.65rem', padding: '1px 5px' }}
                        >
                          {sensor.status || 'ONLINE'}
                        </span>
                      </div>

                      <div style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--text-main)', marginBottom: '0.25rem' }}>
                        {sensor.sensorId}
                      </div>

                      <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--accent-primary)' }}>
                        {sensor.lastValue != null ? sensor.lastValue : '--'}{' '}
                        <span style={{ fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-muted)' }}>
                          {sensor.unit || ''}
                        </span>
                      </div>

                      <div style={{ fontSize: '0.7rem', color: 'var(--text-subtle)', marginTop: '0.35rem' }}>
                        {sensor.location || 'Municipal grid'}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Right Column: Historical Chart & Simulator Controls */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {/* Chart Card */}
            <div className="cc-card">
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiActivity size={15} color="var(--accent-teal)" />
                  Telemetry: {selectedSensor?.sensorId || 'Sensor'} ({selectedSensor?.sensorType})
                </span>
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-main)' }}>
                  Latest: {selectedSensor?.lastValue} {selectedSensor?.unit}
                </span>
              </div>

              {readingsLoading ? (
                <LoadingSpinner text="Loading readings history..." />
              ) : readings.length === 0 ? (
                <EmptyState
                  title="No historical telemetry logged"
                  description="Waiting for MQTT sensor readings to arrive in MongoDB."
                />
              ) : (
                <div style={{ width: '100%', height: '260px' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={readings} margin={{ top: 10, right: 15, left: -15, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                      <XAxis dataKey="time" stroke="#64748b" fontSize={10} />
                      <YAxis stroke="#64748b" fontSize={10} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#ffffff',
                          border: '1px solid #e2e8f0',
                          borderRadius: '6px',
                          fontSize: '0.75rem',
                        }}
                      />
                      <Line
                        type="monotone"
                        dataKey="value"
                        stroke="#0284c7"
                        strokeWidth={2}
                        dot={{ r: 2 }}
                        activeDot={{ r: 5 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>

            {/* Threshold Injection / Testing Sandbox Card */}
            <div className="cc-card">
              <div className="cc-card-header">
                <span className="cc-card-title">
                  <FiSliders size={15} color="var(--accent-primary)" />
                  Simulated Threshold Breach Tester
                </span>
                <span className="badge badge-medium" style={{ fontSize: '0.675rem' }}>
                  Operator Verification
                </span>
              </div>

              <p style={{ fontSize: '0.775rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                Inject a threshold-breaching telemetry spike into <strong>{selectedSensor?.sensorId || 'selected sensor'}</strong> to verify automatic Incident creation, Priority evaluation, SLA assignment, and STOMP WebSocket notification.
              </p>

              <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
                <input
                  type="number"
                  className="cc-input"
                  style={{ width: '120px' }}
                  value={spikeValue}
                  onChange={(e) => setSpikeValue(e.target.value)}
                  placeholder="Value"
                />
                <button
                  onClick={handleTriggerSpike}
                  disabled={spiking || !selectedSensor}
                  className="btn btn-danger btn-sm"
                >
                  <FiZap size={13} />
                  {spiking ? 'Injecting Spike...' : `Trigger Spike (${spikeValue} ${selectedSensor?.unit || ''})`}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SensorsPage;
