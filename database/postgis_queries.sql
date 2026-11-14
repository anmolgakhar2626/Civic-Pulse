-- ==============================================================================
-- CivicPulse Smart City Command Centre — PostGIS Spatial Reference Queries
-- SRID: EPSG:4326 (WGS 84 Latitude/Longitude)
-- ==============================================================================

-- 1. Enable PostGIS Extensions (executed during initial migration)
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- 2. Create Spatial Index for High-Performance Geofencing
CREATE INDEX IF NOT EXISTS idx_locations_geom ON locations USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_sensors_gis_geom ON sensors_gis USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_infrastructure_geom ON infrastructure USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_traffic_signals_geom ON traffic_signals USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_waste_bins_geom ON waste_bins USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_emergency_resources_geom ON emergency_resources USING GIST (geom);

-- 3. Query: Find all complaints within a 2-kilometer radius of a given coordinate
-- Example: Indiranagar / City Centre (30.7333 N, 76.7794 E)
SELECT 
    c.id,
    c.complaint_number,
    c.title,
    c.category,
    c.status,
    c.priority,
    ROUND(ST_Distance(
        ST_SetSRID(ST_MakePoint(c.longitude, c.latitude), 4326)::geography,
        ST_SetSRID(ST_MakePoint(76.7794, 30.7333), 4326)::geography
    )::numeric, 1) AS distance_meters
FROM complaints c
WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL
  AND ST_DWithin(
        ST_SetSRID(ST_MakePoint(c.longitude, c.latitude), 4326)::geography,
        ST_SetSRID(ST_MakePoint(76.7794, 30.7333), 4326)::geography,
        2000 -- 2000 meters radius
  )
ORDER BY distance_meters ASC;

-- 4. Query: Nearest Emergency Resource (Ambulance/Fire) to Incident Location
SELECT 
    er.id,
    er.resource_type,
    er.call_sign,
    er.status,
    ROUND(ST_Distance(
        er.geom::geography,
        ST_SetSRID(ST_MakePoint(76.7794, 30.7333), 4326)::geography
    )::numeric, 1) AS distance_meters
FROM emergency_resources er
WHERE er.status = 'AVAILABLE'
ORDER BY er.geom <-> ST_SetSRID(ST_MakePoint(76.7794, 30.7333), 4326)
LIMIT 3;

-- 5. Query: Export Active Infrastructure Outages as GeoJSON FeatureCollection
SELECT json_build_object(
    'type', 'FeatureCollection',
    'features', json_agg(
        json_build_object(
            'type', 'Feature',
            'geometry', ST_AsGeoJSON(geom)::json,
            'properties', json_build_object(
                'id', id,
                'name', name,
                'type', infrastructure_type,
                'status', operational_status,
                'department', department_code
            )
        )
    )
) AS geojson_output
FROM infrastructure
WHERE operational_status != 'OPERATIONAL';

-- 6. Query: Detect Incidents Near Environmental Sensor Anomaly Clusters
SELECT 
    i.id AS incident_id,
    i.title,
    s.sensor_id,
    s.sensor_type,
    ROUND(ST_Distance(
        i.geom::geography,
        s.geom::geography
    )::numeric, 1) AS distance_meters
FROM incidents i
JOIN sensors_gis s ON ST_DWithin(i.geom::geography, s.geom::geography, 500)
WHERE i.status NOT IN ('RESOLVED', 'CLOSED');
