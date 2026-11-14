-- CivicPulse Smart City Command Centre - V1 Database Foundation Schema
-- Structured City Data Schema (PostgreSQL + PostGIS)

-- 1. Enable UUID Extension (built-in gen_random_uuid in PG 13+, uuid-ossp fallback)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. PostGIS Extension (conditional on environment availability)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS postgis;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'PostGIS extension not installed in current postgres instance; proceeding with coordinate columns';
END $$;

-- ===================================================
-- TABLE: roles
-- ===================================================
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: departments
-- ===================================================
CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    contact_email VARCHAR(150),
    contact_phone VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: teams
-- ===================================================
CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    contact_number VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: users
-- ===================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    role_id UUID NOT NULL REFERENCES roles(id),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: locations
-- ===================================================
CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    location_type VARCHAR(50) NOT NULL, -- WARD, SECTOR, JUNCTION, ZONE, DISTRICT
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    ward_number VARCHAR(50),
    district VARCHAR(100),
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: priority_rules
-- ===================================================
CREATE TABLE IF NOT EXISTS priority_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id VARCHAR(50) NOT NULL UNIQUE,
    incident_type VARCHAR(100) NOT NULL,
    tier VARCHAR(20) NOT NULL,       -- TIER_1, TIER_2, TIER_3, TIER_4
    priority VARCHAR(20) NOT NULL,   -- CRITICAL, HIGH, MEDIUM, LOW
    sla_minutes INT NOT NULL,
    default_department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    conditions_json TEXT,
    recommended_actions_json TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: incidents
-- ===================================================
CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_number VARCHAR(50) NOT NULL UNIQUE,
    incident_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    source VARCHAR(50) NOT NULL, -- CITIZEN, IOT, OPERATOR, EXTERNAL_API
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address VARCHAR(255),
    location_id UUID REFERENCES locations(id) ON DELETE SET NULL,
    reported_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detection_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    severity_indicator VARCHAR(50),
    population_exposure VARCHAR(50),
    infrastructure_importance VARCHAR(50),
    assigned_tier VARCHAR(20) NOT NULL, -- TIER_1, TIER_2, TIER_3, TIER_4
    priority VARCHAR(20) NOT NULL,      -- CRITICAL, HIGH, MEDIUM, LOW
    sla_minutes INT NOT NULL,
    sla_deadline TIMESTAMPTZ NOT NULL,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    assigned_team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    current_status VARCHAR(50) NOT NULL DEFAULT 'DETECTED',
    escalation_state VARCHAR(50) NOT NULL DEFAULT 'NONE',
    resolution_time TIMESTAMPTZ,
    resolution_notes TEXT,
    evidence_url VARCHAR(500),
    reason_notes TEXT,
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: incident_status_history
-- ===================================================
CREATE TABLE IF NOT EXISTS incident_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    changed_by_username VARCHAR(100) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: sla_records
-- ===================================================
CREATE TABLE IF NOT EXISTS sla_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL UNIQUE REFERENCES incidents(id) ON DELETE CASCADE,
    target_sla_minutes INT NOT NULL,
    deadline_at TIMESTAMPTZ NOT NULL,
    current_sla_status VARCHAR(50) NOT NULL DEFAULT 'ON_TRACK', -- ON_TRACK, AT_RISK, BREACHED, RESOLVED
    remaining_seconds BIGINT,
    breached BOOLEAN NOT NULL DEFAULT FALSE,
    breached_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: complaints
-- ===================================================
CREATE TABLE IF NOT EXISTS complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_number VARCHAR(50) NOT NULL UNIQUE,
    citizen_id UUID REFERENCES users(id) ON DELETE SET NULL,
    citizen_name VARCHAR(150) NOT NULL,
    citizen_phone VARCHAR(50),
    category VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    address VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    incident_id UUID REFERENCES incidents(id) ON DELETE SET NULL,
    evidence_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: infrastructure
-- ===================================================
CREATE TABLE IF NOT EXISTS infrastructure (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    infrastructure_type VARCHAR(100) NOT NULL, -- ROAD, BRIDGE, SUBSTATION, WATER_PIPELINE, DRAINAGE, SHELTER
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'OPERATIONAL',
    importance_level VARCHAR(50) NOT NULL DEFAULT 'PRIMARY', -- CRITICAL, PRIMARY, SECONDARY
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: traffic_signals
-- ===================================================
CREATE TABLE IF NOT EXISTS traffic_signals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    signal_code VARCHAR(50) NOT NULL UNIQUE,
    junction_name VARCHAR(150) NOT NULL,
    junction_type VARCHAR(50) NOT NULL, -- MAJOR, MINOR, RESIDENTIAL
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    operational_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FAILING, MAINTENANCE, OFFLINE
    current_cycle_seconds INT DEFAULT 90,
    last_inspected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: waste_bins
-- ===================================================
CREATE TABLE IF NOT EXISTS waste_bins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bin_code VARCHAR(50) NOT NULL UNIQUE,
    location_name VARCHAR(150) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    capacity_liters INT NOT NULL DEFAULT 1100,
    current_fill_percentage DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    last_emptied_at TIMESTAMPTZ,
    battery_percentage DOUBLE PRECISION DEFAULT 100.0,
    sensor_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FAULTY, OFFLINE
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: emergency_resources
-- ===================================================
CREATE TABLE IF NOT EXISTS emergency_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    resource_type VARCHAR(100) NOT NULL, -- HOSPITAL, POLICE_STATION, FIRE_STATION, EMERGENCY_SHELTER
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address VARCHAR(255),
    contact_number VARCHAR(50),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    capacity INT DEFAULT 0,
    operational_hours VARCHAR(100) DEFAULT '24/7',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: notifications
-- ===================================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL, -- CRITICAL_ALERT, INCIDENT_ASSIGNMENT, SLA_WARNING, STATUS_UPDATE
    channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP', -- IN_APP, SMS, EMAIL, WEBSOCKET
    status VARCHAR(50) NOT NULL DEFAULT 'UNREAD',  -- UNREAD, READ, DISMISSED
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- TABLE: audit_logs
-- ===================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    details_json TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- PostGIS Support: Add Geometry Columns & GiST Indexes if PostGIS installed
-- ===================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis') THEN
        ALTER TABLE locations ADD COLUMN IF NOT EXISTS location_geom geometry(Point, 4326);
        ALTER TABLE incidents ADD COLUMN IF NOT EXISTS location_geom geometry(Point, 4326);
        ALTER TABLE complaints ADD COLUMN IF NOT EXISTS location_geom geometry(Point, 4326);
        ALTER TABLE infrastructure ADD COLUMN IF NOT EXISTS location_geom geometry(Point, 4326);
        ALTER TABLE traffic_signals ADD COLUMN IF NOT EXISTS location_geom geometry(Point, 4326);
        ALTER TABLE waste_bins ADD COLUMN IF NOT EXISTS location_geom geometry(Point, 4326);
        ALTER TABLE emergency_resources ADD COLUMN IF NOT EXISTS location_geom geometry(Point, 4326);

        CREATE INDEX IF NOT EXISTS idx_locations_geom ON locations USING GIST (location_geom);
        CREATE INDEX IF NOT EXISTS idx_incidents_geom ON incidents USING GIST (location_geom);
        CREATE INDEX IF NOT EXISTS idx_complaints_geom ON complaints USING GIST (location_geom);
        CREATE INDEX IF NOT EXISTS idx_infrastructure_geom ON infrastructure USING GIST (location_geom);
        CREATE INDEX IF NOT EXISTS idx_traffic_signals_geom ON traffic_signals USING GIST (location_geom);
        CREATE INDEX IF NOT EXISTS idx_waste_bins_geom ON waste_bins USING GIST (location_geom);
        CREATE INDEX IF NOT EXISTS idx_emergency_resources_geom ON emergency_resources USING GIST (location_geom);
    END IF;
END $$;

-- ===================================================
-- B-Tree & Functional Indexes for Relational Queries
-- ===================================================
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_users_department_id ON users(department_id);

CREATE INDEX IF NOT EXISTS idx_teams_department_id ON teams(department_id);

CREATE INDEX IF NOT EXISTS idx_incidents_status ON incidents(current_status);
CREATE INDEX IF NOT EXISTS idx_incidents_priority ON incidents(priority);
CREATE INDEX IF NOT EXISTS idx_incidents_tier ON incidents(assigned_tier);
CREATE INDEX IF NOT EXISTS idx_incidents_department_id ON incidents(department_id);
CREATE INDEX IF NOT EXISTS idx_incidents_reported_time ON incidents(reported_time);
CREATE INDEX IF NOT EXISTS idx_incidents_coordinates ON incidents(latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_incident_history_incident_id ON incident_status_history(incident_id);

CREATE INDEX IF NOT EXISTS idx_sla_records_status ON sla_records(current_sla_status);
CREATE INDEX IF NOT EXISTS idx_sla_records_deadline ON sla_records(deadline_at);

CREATE INDEX IF NOT EXISTS idx_complaints_citizen_id ON complaints(citizen_id);
CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints(status);
CREATE INDEX IF NOT EXISTS idx_complaints_incident_id ON complaints(incident_id);

CREATE INDEX IF NOT EXISTS idx_infrastructure_type ON infrastructure(infrastructure_type);
CREATE INDEX IF NOT EXISTS idx_locations_type ON locations(location_type);
CREATE INDEX IF NOT EXISTS idx_traffic_signals_status ON traffic_signals(operational_status);
CREATE INDEX IF NOT EXISTS idx_waste_bins_fill ON waste_bins(current_fill_percentage);
CREATE INDEX IF NOT EXISTS idx_emergency_resources_type ON emergency_resources(resource_type);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_status ON notifications(recipient_user_id, status);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
