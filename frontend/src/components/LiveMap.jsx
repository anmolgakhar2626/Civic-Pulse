import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle, useMap } from 'react-leaflet';
import L from 'leaflet';
import { PriorityBadge } from './PriorityBadge';
import { StatusBadge } from './StatusBadge';
import { Link } from 'react-router-dom';

// Custom CSS styles for animated and crisp GIS markers
const markerStyles = `
  @keyframes cc-pulse-critical {
    0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
    70% { box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
    100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
  }
  @keyframes cc-pulse-selected {
    0% { transform: scale(1); }
    50% { transform: scale(1.15); }
    100% { transform: scale(1); }
  }
  .cc-marker-critical {
    animation: cc-pulse-critical 1.8s infinite;
  }
  .cc-marker-selected {
    animation: cc-pulse-selected 1s ease-in-out 2;
    border-color: #06b6d4 !important;
    box-shadow: 0 0 12px #06b6d4 !important;
  }
`;

// Inject marker styles once into the document head
if (typeof document !== 'undefined' && !document.getElementById('cc-marker-styles')) {
  const styleEl = document.createElement('style');
  styleEl.id = 'cc-marker-styles';
  styleEl.innerHTML = markerStyles;
  document.head.appendChild(styleEl);
}

// Marker generators
const createIncidentIcon = (severity = 'MEDIUM', status = 'OPEN', isSelected = false) => {
  const sev = (severity || 'MEDIUM').toUpperCase();
  const st = (status || 'OPEN').toUpperCase();

  let bgColor = '#f59e0b';
  let symbol = 'M';
  let isCritical = false;

  if (sev === 'CRITICAL' || sev === 'EXTREME') {
    bgColor = '#ef4444';
    symbol = '!';
    isCritical = true;
  } else if (sev === 'HIGH') {
    bgColor = '#f97316';
    symbol = 'H';
  } else if (sev === 'LOW') {
    bgColor = '#10b981';
    symbol = 'L';
  }

  const isResolved = st === 'RESOLVED' || st === 'CLOSED';
  if (isResolved) {
    symbol = '✓';
    bgColor = '#64748b';
  }

  const pulseClass = isCritical && !isResolved ? 'cc-marker-critical' : '';
  const selectedClass = isSelected ? 'cc-marker-selected' : '';

  return L.divIcon({
    className: 'cc-leaflet-div-icon',
    html: `
      <div class="${pulseClass} ${selectedClass}" style="
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background-color: ${bgColor};
        border: 2px solid #ffffff;
        box-shadow: 0 2px 6px rgba(0,0,0,0.35);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-size: 11px;
        font-weight: 800;
        cursor: pointer;
        opacity: ${isResolved ? 0.75 : 1};
      ">
        ${symbol}
      </div>
    `,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -14],
  });
};

const createInfrastructureIcon = (category = 'OTHER', status = 'OPERATIONAL', isSelected = false) => {
  const cat = (category || 'OTHER').toUpperCase();
  const st = (status || 'OPERATIONAL').toUpperCase();

  let symbol = '🏛';
  let bgColor = '#334155';

  if (cat.includes('HOSPITAL')) {
    symbol = '🏥';
    bgColor = '#be123c';
  } else if (cat.includes('BRIDGE')) {
    symbol = '🌉';
    bgColor = '#0f766e';
  } else if (cat.includes('ROAD')) {
    symbol = '🛣';
    bgColor = '#475569';
  } else if (cat.includes('WATER') || cat.includes('DRAIN')) {
    symbol = '💧';
    bgColor = '#0369a1';
  } else if (cat.includes('SUBSTATION') || cat.includes('POWER') || cat.includes('GRID')) {
    symbol = '⚡';
    bgColor = '#b45309';
  } else if (cat.includes('SHELTER')) {
    symbol = '⛺';
    bgColor = '#047857';
  }

  let borderColor = '#10b981'; // OPERATIONAL
  if (st === 'DEGRADED') borderColor = '#f59e0b';
  else if (st === 'OFFLINE') borderColor = '#ef4444';
  else if (st === 'UNDER_MAINTENANCE') borderColor = '#a855f7';

  const selectedClass = isSelected ? 'cc-marker-selected' : '';

  return L.divIcon({
    className: 'cc-leaflet-div-icon',
    html: `
      <div class="${selectedClass}" style="
        width: 30px;
        height: 30px;
        border-radius: 6px;
        background-color: ${bgColor};
        border: 2px solid ${borderColor};
        box-shadow: 0 2px 6px rgba(0,0,0,0.35);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-size: 13px;
        cursor: pointer;
      ">
        ${symbol}
      </div>
    `,
    iconSize: [30, 30],
    iconAnchor: [15, 15],
    popupAnchor: [0, -15],
  });
};

const createShelterIcon = (available = true, isSelected = false) => {
  const borderColor = available ? '#10b981' : '#ef4444';
  const selectedClass = isSelected ? 'cc-marker-selected' : '';

  return L.divIcon({
    className: 'cc-leaflet-div-icon',
    html: `
      <div class="${selectedClass}" style="
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background-color: #0f766e;
        border: 2px solid ${borderColor};
        box-shadow: 0 2px 6px rgba(0,0,0,0.35);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-size: 13px;
        cursor: pointer;
      ">
        ⛺
      </div>
    `,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -14],
  });
};

const createSensorIcon = (status = 'ONLINE') => {
  const isOnline = (status || 'ONLINE').toUpperCase() === 'ONLINE';
  return L.divIcon({
    className: 'cc-leaflet-div-icon',
    html: `
      <div style="
        width: 22px;
        height: 22px;
        border-radius: 50%;
        background-color: ${isOnline ? '#0284c7' : '#94a3b8'};
        border: 2px solid #ffffff;
        box-shadow: 0 1px 4px rgba(0,0,0,0.25);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #ffffff;
        font-size: 10px;
        cursor: pointer;
      ">
        📡
      </div>
    `,
    iconSize: [22, 22],
    iconAnchor: [11, 11],
    popupAnchor: [0, -11],
  });
};

// Component to recenter map dynamically when center/zoom changes
function ChangeView({ center, zoom }) {
  const map = useMap();
  useEffect(() => {
    if (center && center[0] != null && center[1] != null && !isNaN(center[0]) && !isNaN(center[1])) {
      map.setView(center, zoom, { animate: true });
    }
  }, [center, zoom, map]);
  return null;
}

// Invalidate size on container layout changes
function MapResizeHandler() {
  const map = useMap();
  useEffect(() => {
    const handleResize = () => {
      setTimeout(() => {
        map.invalidateSize();
      }, 200);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [map]);
  return null;
}

export const LiveMap = ({
  incidents = [],
  infrastructure = [],
  shelters = [],
  sensors = [],
  center = [30.7333, 76.7794],
  zoom = 13,
  height = '560px',
  selectedId = null,
  onSelectMarker = null,
  showControls = true,
  radiusQuery = null,
}) => {
  const [layers, setLayers] = useState({
    incidents: true,
    infrastructure: true,
    shelters: true,
    sensors: true,
  });

  const toggleLayer = (layerName) => {
    setLayers((prev) => ({ ...prev, [layerName]: !prev[layerName] }));
  };

  return (
    <div style={{ position: 'relative', height, width: '100%', borderRadius: '8px', overflow: 'hidden' }} className="cc-map-container">
      {/* Floating Layer Toggle Pill */}
      {showControls && (
        <div
          style={{
            position: 'absolute',
            top: '12px',
            right: '12px',
            zIndex: 1000,
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(4px)',
            padding: '8px 14px',
            borderRadius: '8px',
            border: '1px solid #cbd5e1',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
            fontSize: '0.78rem',
            display: 'flex',
            gap: '12px',
            alignItems: 'center',
            color: '#1e293b',
          }}
        >
          <span style={{ fontWeight: 700, color: '#475569', display: 'flex', alignItems: 'center', gap: '4px' }}>
            Layers:
          </span>
          <label style={{ display: 'flex', alignItems: 'center', gap: '5px', cursor: 'pointer', fontWeight: 500 }}>
            <input
              type="checkbox"
              checked={layers.incidents}
              onChange={() => toggleLayer('incidents')}
            />
            <span style={{ color: '#dc2626' }}>●</span> Incidents ({incidents.length})
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '5px', cursor: 'pointer', fontWeight: 500 }}>
            <input
              type="checkbox"
              checked={layers.infrastructure}
              onChange={() => toggleLayer('infrastructure')}
            />
            <span style={{ color: '#0369a1' }}>■</span> Infrastructure ({infrastructure.length})
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '5px', cursor: 'pointer', fontWeight: 500 }}>
            <input
              type="checkbox"
              checked={layers.shelters}
              onChange={() => toggleLayer('shelters')}
            />
            <span style={{ color: '#0f766e' }}>▲</span> Shelters ({shelters.length})
          </label>
          {sensors && sensors.length > 0 && (
            <label style={{ display: 'flex', alignItems: 'center', gap: '5px', cursor: 'pointer', fontWeight: 500 }}>
              <input
                type="checkbox"
                checked={layers.sensors}
                onChange={() => toggleLayer('sensors')}
              />
              <span style={{ color: '#0284c7' }}>📡</span> Sensors ({sensors.length})
            </label>
          )}
        </div>
      )}

      <MapContainer
        center={center}
        zoom={zoom}
        style={{ height: '100%', width: '100%' }}
        scrollWheelZoom={true}
      >
        <ChangeView center={center} zoom={zoom} />
        <MapResizeHandler />

        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          maxZoom={19}
        />

        {/* Optional Radius buffer for spatial queries */}
        {radiusQuery && radiusQuery.lat && radiusQuery.lon && (
          <Circle
            center={[radiusQuery.lat, radiusQuery.lon]}
            radius={radiusQuery.radiusMeters || 5000}
            pathOptions={{
              color: '#0284c7',
              fillColor: '#0284c7',
              fillOpacity: 0.12,
              weight: 2,
              dashArray: '5, 5',
            }}
          />
        )}

        {/* 1. Incidents Layer */}
        {layers.incidents &&
          incidents.map((inc) => {
            const lat = inc.latitude;
            const lon = inc.longitude;
            // CRITICAL: Exclude records lacking coordinates from map rendering
            if (lat == null || lon == null || isNaN(lat) || isNaN(lon)) return null;

            const isSelected = selectedId === inc.id;
            const icon = createIncidentIcon(inc.severity, inc.status, isSelected);

            return (
              <Marker
                key={`incident-${inc.id || inc.incidentNumber}`}
                position={[lat, lon]}
                icon={icon}
                eventHandlers={{
                  click: () => onSelectMarker && onSelectMarker('INCIDENT', inc),
                }}
              >
                <Popup>
                  <div style={{ minWidth: '220px', padding: '4px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                      <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#64748b' }}>
                        {inc.incidentNumber || 'INCIDENT'}
                      </span>
                      <PriorityBadge tier={inc.severity || 'MEDIUM'} />
                    </div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#0f172a', marginBottom: '4px' }}>
                      {inc.title || 'Incident Marker'}
                    </div>
                    <p style={{ fontSize: '0.75rem', color: '#475569', margin: '4px 0 8px 0' }}>
                      <strong>Location:</strong> {inc.locationName || inc.address || `${lat.toFixed(4)}, ${lon.toFixed(4)}`}
                    </p>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '6px', paddingTop: '6px', borderTop: '1px solid #e2e8f0' }}>
                      <StatusBadge status={inc.status} />
                      {inc.id && (
                        <Link
                          to={`/incidents/${inc.id}`}
                          style={{ fontSize: '0.75rem', color: '#0284c7', fontWeight: 600, textDecoration: 'none' }}
                        >
                          View Dossier &rarr;
                        </Link>
                      )}
                    </div>
                  </div>
                </Popup>
              </Marker>
            );
          })}

        {/* 2. Infrastructure Layer */}
        {layers.infrastructure &&
          infrastructure.map((inf) => {
            const lat = inf.latitude;
            const lon = inf.longitude;
            if (lat == null || lon == null || isNaN(lat) || isNaN(lon)) return null;

            const isSelected = selectedId === inf.id;
            const icon = createInfrastructureIcon(inf.category || inf.infrastructureType, inf.status, isSelected);

            return (
              <Marker
                key={`infra-${inf.id || inf.code}`}
                position={[lat, lon]}
                icon={icon}
                eventHandlers={{
                  click: () => onSelectMarker && onSelectMarker('INFRASTRUCTURE', inf),
                }}
              >
                <Popup>
                  <div style={{ minWidth: '230px', padding: '4px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                      <span style={{ fontSize: '0.7rem', fontWeight: 700, color: '#0284c7', textTransform: 'uppercase' }}>
                        {inf.category || inf.infrastructureType || 'INFRASTRUCTURE'}
                      </span>
                      <span style={{
                        fontSize: '0.68rem',
                        fontWeight: 700,
                        padding: '2px 6px',
                        borderRadius: '4px',
                        backgroundColor: inf.status === 'OPERATIONAL' ? '#dcfce7' : inf.status === 'UNDER_MAINTENANCE' ? '#f3e8ff' : '#fee2e2',
                        color: inf.status === 'OPERATIONAL' ? '#15803d' : inf.status === 'UNDER_MAINTENANCE' ? '#7e22ce' : '#b91c1c'
                      }}>
                        {inf.status || 'OPERATIONAL'}
                      </span>
                    </div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#0f172a', marginBottom: '4px' }}>
                      {inf.name}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#475569', lineHeight: 1.4 }}>
                      <div><strong>Code:</strong> {inf.code}</div>
                      {inf.address && <div><strong>Address:</strong> {inf.address}</div>}
                      {inf.maintenancePriority && <div><strong>Priority:</strong> {inf.maintenancePriority}</div>}
                      {inf.notes && <div style={{ marginTop: '4px', fontStyle: 'italic', color: '#64748b' }}>"{inf.notes}"</div>}
                    </div>
                  </div>
                </Popup>
              </Marker>
            );
          })}

        {/* 3. Shelters Layer */}
        {layers.shelters &&
          shelters.map((shelter) => {
            const lat = shelter.latitude;
            const lon = shelter.longitude;
            if (lat == null || lon == null || isNaN(lat) || isNaN(lon)) return null;

            const isSelected = selectedId === shelter.id;
            const icon = createShelterIcon(shelter.available !== false, isSelected);

            return (
              <Marker
                key={`shelter-${shelter.id || shelter.name}`}
                position={[lat, lon]}
                icon={icon}
                eventHandlers={{
                  click: () => onSelectMarker && onSelectMarker('SHELTER', shelter),
                }}
              >
                <Popup>
                  <div style={{ minWidth: '220px', padding: '4px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                      <span style={{ fontSize: '0.7rem', fontWeight: 700, color: '#0f766e', textTransform: 'uppercase' }}>
                        Refuge Shelter
                      </span>
                      <span style={{
                        fontSize: '0.68rem',
                        fontWeight: 700,
                        padding: '2px 6px',
                        borderRadius: '4px',
                        backgroundColor: shelter.available !== false ? '#ccfbf1' : '#fee2e2',
                        color: shelter.available !== false ? '#0f766e' : '#b91c1c'
                      }}>
                        {shelter.available !== false ? 'Available' : 'Capacity Full'}
                      </span>
                    </div>
                    <div style={{ fontSize: '0.88rem', fontWeight: 700, color: '#0f172a', marginBottom: '4px' }}>
                      {shelter.name}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#475569', lineHeight: 1.4 }}>
                      <div><strong>Capacity:</strong> {shelter.capacity || 'N/A'} persons</div>
                      <div><strong>Hours:</strong> {shelter.operationalHours || '24/7'}</div>
                      {shelter.contactNumber && <div><strong>Contact:</strong> {shelter.contactNumber}</div>}
                      {shelter.address && <div><strong>Address:</strong> {shelter.address}</div>}
                    </div>
                  </div>
                </Popup>
              </Marker>
            );
          })}

        {/* 4. IoT Sensors Layer */}
        {layers.sensors &&
          sensors.map((sensor) => {
            const lat = sensor.latitude || (sensor.geometry?.coordinates && sensor.geometry.coordinates[1]);
            const lon = sensor.longitude || (sensor.geometry?.coordinates && sensor.geometry.coordinates[0]);
            if (!lat || !lon) return null;

            const props = sensor.properties || sensor;
            return (
              <Marker
                key={`sensor-${props.sensorId || props.id}`}
                position={[lat, lon]}
                icon={createSensorIcon(props.status)}
                eventHandlers={{
                  click: () => onSelectMarker && onSelectMarker('SENSOR', props),
                }}
              >
                <Popup>
                  <div style={{ minWidth: '200px', padding: '4px' }}>
                    <div style={{ fontSize: '0.7rem', fontWeight: 700, color: '#0284c7', textTransform: 'uppercase' }}>
                      IoT Environmental Sensor
                    </div>
                    <div style={{ fontSize: '0.88rem', fontWeight: 700, color: '#0f172a' }}>
                      {props.sensorId} ({props.sensorType})
                    </div>
                    <p style={{ fontSize: '0.75rem', color: '#475569', margin: '4px 0 0 0' }}>
                      Location: {props.locationName || 'City Grid'}<br />
                      Value: <strong>{props.lastValue != null ? props.lastValue : '--'} {props.unit || ''}</strong><br />
                      Status: <strong style={{ color: props.status === 'ONLINE' ? '#15803d' : '#b91c1c' }}>{props.status || 'ONLINE'}</strong>
                    </p>
                  </div>
                </Popup>
              </Marker>
            );
          })}
      </MapContainer>
    </div>
  );
};

export default LiveMap;
