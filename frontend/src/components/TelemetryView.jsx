import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { Car, Trash2, Wind, CloudRain, RefreshCw, AlertTriangle } from 'lucide-react';

export default function TelemetryView() {
  const [traffic, setTraffic] = useState([]);
  const [waste, setWaste] = useState([]);
  const [airQuality, setAirQuality] = useState([]);
  const [weather, setWeather] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchTelemetry = async () => {
    setLoading(true);
    try {
      const [trafRes, wasteRes, aqiRes, weaRes] = await Promise.allSettled([
        api.get('/traffic/readings'),
        api.get('/waste/bins'),
        api.get('/air-quality/readings'),
        api.get('/weather/current')
      ]);

      if (trafRes.status === 'fulfilled') setTraffic(trafRes.value.data || []);
      if (wasteRes.status === 'fulfilled') setWaste(wasteRes.value.data || []);
      if (aqiRes.status === 'fulfilled') setAirQuality(aqiRes.value.data || []);
      if (weaRes.status === 'fulfilled') setWeather(weaRes.value.data || null);
    } catch (e) {
      console.error('Failed to load telemetry', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTelemetry();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {/* Live Atmospheric Intelligence Bar */}
      {weather && (
        <div className="panel" style={{ borderLeft: '4px solid #0284c7' }}>
          <div className="panel-header">
            <div className="panel-title">
              <CloudRain size={18} color="#0284c7" />
              <span>Live Weather & Flood Vulnerability (Open-Meteo API)</span>
            </div>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
              Live external meteorological feed
            </span>
          </div>
          <div className="panel-body" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Condition</div>
              <div style={{ fontSize: '1.1rem', fontWeight: 700 }}>{weather.condition}</div>
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Surface Temperature</div>
              <div style={{ fontSize: '1.1rem', fontWeight: 700 }}>{weather.temperature}°C</div>
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Relative Humidity</div>
              <div style={{ fontSize: '1.1rem', fontWeight: 700 }}>{weather.humidity}%</div>
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Precipitation / Rain</div>
              <div style={{ fontSize: '1.1rem', fontWeight: 700 }}>{weather.precipitation} mm</div>
            </div>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Flood Risk State</div>
              <div style={{
                fontSize: '1rem',
                fontWeight: 700,
                color: weather.floodRisk === 'HIGH' ? '#dc2626' : (weather.floodRisk === 'MODERATE' ? '#d97706' : '#15803d')
              }}>
                {weather.floodRisk} RISK
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Grid of IoT Sensors */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '1.25rem' }}>
        {/* Smart Mobility & Traffic */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title">
              <Car size={18} color="#0284c7" />
              <span>Traffic Sensors & Road Ingestion</span>
              <span className="demo-badge">SIMULATED / MQTT</span>
            </div>
            <button className="btn btn-outline btn-sm" onClick={fetchTelemetry}>
              <RefreshCw size={12} />
            </button>
          </div>
          <div className="panel-body">
            {traffic.length === 0 ? (
              <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No telemetry readings recorded.</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {traffic.slice(0, 4).map((t, idx) => (
                  <div key={idx} style={{
                    padding: '0.75rem',
                    background: 'var(--bg-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border-subtle)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{t.junctionName}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                        Sensor: {t.sensorId} | Avg Speed: {t.averageSpeed} km/h
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <span className={`badge ${t.congestionLevel === 'HEAVY' ? 'badge-tier2' : 'badge-tier4'}`}>
                        {t.congestionLevel}
                      </span>
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                        {t.vehicleCount} veh/h
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Smart Waste Sensors */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title">
              <Trash2 size={18} color="#b45309" />
              <span>Smart Waste Telemetry</span>
              <span className="demo-badge">SIMULATED / MQTT</span>
            </div>
          </div>
          <div className="panel-body">
            {waste.length === 0 ? (
              <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No bin readings recorded.</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {waste.slice(0, 4).map((b, idx) => (
                  <div key={idx} style={{
                    padding: '0.75rem',
                    background: 'var(--bg-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border-subtle)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{b.locationName}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                        Bin ID: {b.binId} | Battery: {b.batteryLevel}%
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <span className={`badge ${b.fillPercentage >= 90 ? 'badge-tier2' : (b.fillPercentage >= 70 ? 'badge-tier3' : 'badge-tier4')}`}>
                        {b.fillPercentage}% FULL
                      </span>
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                        {b.fillPercentage >= 90 ? 'Automated Incident Triggered' : 'Normal State'}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Environmental Air Quality Stations */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title">
              <Wind size={18} color="#0f766e" />
              <span>Air Quality Stations</span>
              <span className="demo-badge">SIMULATED / OPEN-METEO</span>
            </div>
          </div>
          <div className="panel-body">
            {airQuality.length === 0 ? (
              <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No air stations recorded.</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {airQuality.slice(0, 4).map((aq, idx) => (
                  <div key={idx} style={{
                    padding: '0.75rem',
                    background: 'var(--bg-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border-subtle)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{aq.locationName}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                        Station: {aq.stationId} | PM2.5: {aq.pm25} µg/m³
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <span className={`badge ${aq.aqi > 200 ? 'badge-tier1' : (aq.aqi > 100 ? 'badge-tier2' : 'badge-tier4')}`}>
                        AQI {aq.aqi} ({aq.status})
                      </span>
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                        NO2: {aq.no2} | SO2: {aq.so2}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
