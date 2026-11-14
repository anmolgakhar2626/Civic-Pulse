-- ==============================================================================
-- Migration V7: GIS Spatial Enhancements, Indexes, Distance Functions & Seed Data
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;

-- Spatial Distance Helper Function (PostgreSQL Earthdistance)
CREATE OR REPLACE FUNCTION calculate_distance_meters(lat1 DOUBLE PRECISION, lon1 DOUBLE PRECISION, lat2 DOUBLE PRECISION, lon2 DOUBLE PRECISION)
RETURNS DOUBLE PRECISION AS $$
BEGIN
    RETURN earth_distance(ll_to_earth(lat1, lon1), ll_to_earth(lat2, lon2));
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Table for GIS Sensors in PostgreSQL
CREATE TABLE IF NOT EXISTS sensors_gis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sensor_id VARCHAR(100) NOT NULL UNIQUE,
    sensor_type VARCHAR(100) NOT NULL,
    location_name VARCHAR(255),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ONLINE',
    last_value DOUBLE PRECISION,
    unit VARCHAR(50),
    last_seen_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- GiST Spatial Indexes for Rapid Earthdistance Queries
CREATE INDEX IF NOT EXISTS idx_incidents_earth ON incidents USING gist (ll_to_earth(latitude, longitude));
CREATE INDEX IF NOT EXISTS idx_infrastructure_earth ON infrastructure USING gist (ll_to_earth(latitude, longitude));
CREATE INDEX IF NOT EXISTS idx_emergency_resources_earth ON emergency_resources USING gist (ll_to_earth(latitude, longitude));
CREATE INDEX IF NOT EXISTS idx_traffic_signals_earth ON traffic_signals USING gist (ll_to_earth(latitude, longitude));
CREATE INDEX IF NOT EXISTS idx_waste_bins_earth ON waste_bins USING gist (ll_to_earth(latitude, longitude));
CREATE INDEX IF NOT EXISTS idx_locations_earth ON locations USING gist (ll_to_earth(latitude, longitude));
CREATE INDEX IF NOT EXISTS idx_sensors_gis_earth ON sensors_gis USING gist (ll_to_earth(latitude, longitude));

-- ==============================================================================
-- SEED DATA: Emergency Resources
-- ==============================================================================
INSERT INTO emergency_resources (name, resource_type, latitude, longitude, address, contact_number, available, capacity, operational_hours)
VALUES
    ('PGIMER Super Specialty Hospital', 'HOSPITAL', 30.7673, 76.7770, 'Sector 12, Chandigarh', '+91-172-2747585', true, 1900, '24/7'),
    ('General Multi-Specialty Hospital', 'HOSPITAL', 30.7486, 76.7865, 'Sector 16, Chandigarh', '+91-172-2766000', true, 500, '24/7'),
    ('Central Police Headquarters & Station', 'POLICE_STATION', 30.7410, 76.7850, 'Sector 17, Chandigarh', '+91-172-2741100', true, 80, '24/7'),
    ('Sector 17 Central Fire & Rescue Station', 'FIRE_STATION', 30.7425, 76.7820, 'Sector 17-C, Chandigarh', '+91-172-2702333', true, 12, '24/7'),
    ('Disaster Emergency Shelter Sector 22', 'EMERGENCY_SHELTER', 30.7350, 76.7720, 'Sector 22-B Community Centre', '+91-172-2701234', true, 350, '24/7'),
    ('South Zone Police Station Sector 34', 'POLICE_STATION', 30.7190, 76.7650, 'Sector 34 Sub-City Centre', '+91-172-2601999', true, 50, '24/7'),
    ('Industrial Area Fire Station Phase 1', 'FIRE_STATION', 30.7080, 76.8020, 'Phase 1, Industrial Area', '+91-172-2651101', true, 8, '24/7')
ON CONFLICT DO NOTHING;

-- ==============================================================================
-- SEED DATA: Infrastructure
-- ==============================================================================
INSERT INTO infrastructure (code, name, infrastructure_type, latitude, longitude, address, status, importance_level)
VALUES
    ('INF-DAM-01', 'Sukhna Water Gate & Flood Retention Reservoir', 'DRAINAGE', 30.7420, 76.8180, 'Sukhna Lake Regulator End', 'OPERATIONAL', 'CRITICAL'),
    ('INF-SUB-01', 'Sector 17 Central Grid Power Substation', 'SUBSTATION', 30.7395, 76.7840, 'Sector 17 Utility Zone', 'OPERATIONAL', 'CRITICAL'),
    ('INF-PIPE-01', 'Main Water Supply Pipeline Trunk Sector 26', 'WATER_PIPELINE', 30.7310, 76.7950, 'Sector 26 Grain Market Corridor', 'OPERATIONAL', 'PRIMARY'),
    ('INF-FLY-01', 'Madhya Marg Overpass & Express Corridor', 'ROAD', 30.7470, 76.7900, 'Madhya Marg Sector 18/19', 'OPERATIONAL', 'PRIMARY'),
    ('INF-UND-01', 'Sector 19 Railway Underpass & Storm Drain', 'BRIDGE', 30.7300, 76.7880, 'Railway Track Border Sector 19', 'OPERATIONAL', 'PRIMARY')
ON CONFLICT (code) DO NOTHING;

-- ==============================================================================
-- SEED DATA: Traffic Signals
-- ==============================================================================
INSERT INTO traffic_signals (signal_code, junction_name, junction_type, latitude, longitude, operational_status, current_cycle_seconds)
VALUES
    ('SIG-101', 'Matka Chowk Major Junction', 'MAJOR', 30.7455, 76.7845, 'ACTIVE', 120),
    ('SIG-102', 'Sector 17/18 Commercial Crossing', 'MAJOR', 30.7380, 76.7870, 'ACTIVE', 90),
    ('SIG-103', 'Tribune Chowk High-Traffic Junction', 'MAJOR', 30.7125, 76.7890, 'ACTIVE', 120),
    ('SIG-104', 'Transport Chowk Freight Corridor', 'MAJOR', 30.7240, 76.8050, 'ACTIVE', 90),
    ('SIG-105', 'Kisan Bhawan Chowk', 'MINOR', 30.7280, 76.7640, 'ACTIVE', 60)
ON CONFLICT (signal_code) DO NOTHING;

-- ==============================================================================
-- SEED DATA: Waste Bins
-- ==============================================================================
INSERT INTO waste_bins (bin_code, location_name, latitude, longitude, capacity_liters, current_fill_percentage, sensor_status)
VALUES
    ('BIN-SEC17-01', 'Sector 17 Plaza Central Promenade', 30.7405, 76.7832, 1100, 42.5, 'ACTIVE'),
    ('BIN-SEC22-01', 'Sector 22 Market Shastri Market Gate', 30.7360, 76.7745, 1100, 78.0, 'ACTIVE'),
    ('BIN-ISBT-01', 'ISBT Sector 43 Intercity Bus Terminal', 30.7160, 76.7480, 2400, 55.0, 'ACTIVE'),
    ('BIN-SEC35-01', 'Sector 35 Food Street Inner Circle', 30.7210, 76.7630, 1100, 89.5, 'ACTIVE'),
    ('BIN-ROSE-01', 'Zakir Hussain Rose Garden South Gate', 30.7510, 76.7800, 1100, 25.0, 'ACTIVE')
ON CONFLICT (bin_code) DO NOTHING;

-- ==============================================================================
-- SEED DATA: Locations / Public Facilities
-- ==============================================================================
INSERT INTO locations (code, name, location_type, latitude, longitude)
VALUES
    ('FAC-HQ-01', 'Municipal Corporation Head Office', 'PUBLIC_FACILITY', 30.7415, 76.7805),
    ('FAC-LIB-01', 'City Central Library Sector 34', 'PUBLIC_FACILITY', 30.7200, 76.7660),
    ('FAC-THT-01', 'Tagore Theatre Cultural Complex', 'PUBLIC_FACILITY', 30.7375, 76.7925),
    ('FAC-PLZ-01', 'Sector 17 Central Urban Plaza', 'PUBLIC_FACILITY', 30.7400, 76.7820)
ON CONFLICT (code) DO NOTHING;

-- ==============================================================================
-- SEED DATA: GIS Sensors
-- ==============================================================================
INSERT INTO sensors_gis (sensor_id, sensor_type, location_name, latitude, longitude, status, last_value, unit)
VALUES
    ('AQI-SEC17-01', 'AIR_QUALITY', 'Sector 17 Commercial Centre', 30.7410, 76.7850, 'ONLINE', 142.0, 'AQI'),
    ('PM25-IND-01', 'PM25', 'Industrial Area Phase 1', 30.7080, 76.8020, 'ONLINE', 85.0, 'µg/m³'),
    ('PM10-IND-01', 'PM10', 'Industrial Area Phase 1', 30.7080, 76.8020, 'ONLINE', 128.0, 'µg/m³'),
    ('TEMP-WARD5-01', 'TEMPERATURE', 'Ward 5 Municipal Office', 30.7046, 76.7179, 'ONLINE', 24.5, '°C'),
    ('HUM-WARD5-01', 'HUMIDITY', 'Ward 5 Municipal Office', 30.7046, 76.7179, 'ONLINE', 68.0, '%'),
    ('FLOOD-SUKHNA-01', 'WATER_LEVEL', 'Sukhna Lake Regulator Spillway', 30.7420, 76.8180, 'ONLINE', 3.8, 'm'),
    ('TRAF-MATKA-01', 'TRAFFIC', 'Matka Chowk Sensor Loop', 30.7455, 76.7845, 'ONLINE', 74.0, 'veh/min'),
    ('WASTE-SEC22-01', 'WASTE_LEVEL', 'Sector 22 Market Smart Bin', 30.7360, 76.7745, 'ONLINE', 78.0, '%'),
    ('ELEC-SUB17-01', 'ELECTRICAL', 'Sector 17 Substation Feed 3', 30.7395, 76.7840, 'ONLINE', 230.0, 'V'),
    ('SMOKE-ISBT-01', 'SMOKE', 'ISBT Sector 43 Hub', 30.7160, 76.7480, 'ONLINE', 0.02, 'ppm')
ON CONFLICT (sensor_id) DO NOTHING;
