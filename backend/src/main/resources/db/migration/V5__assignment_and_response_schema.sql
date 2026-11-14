-- ===================================================
-- CivicPulse Migration V5: Department Assignment and Response Workflow Schema
-- ===================================================

-- 1. Extend teams table with active column
ALTER TABLE teams ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Extend incidents table with assignment tracking columns
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ;
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS assigned_by VARCHAR(100);
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS assignment_reason TEXT;

-- 3. Create incident_assignment_history table for auditing assignments
CREATE TABLE IF NOT EXISTS incident_assignment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    assigned_by VARCHAR(100) NOT NULL,
    previous_department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    new_department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    previous_team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    new_team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    assignment_reason TEXT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assignment_history_incident_id ON incident_assignment_history(incident_id);
CREATE INDEX IF NOT EXISTS idx_assignment_history_assigned_at ON incident_assignment_history(assigned_at);

-- 4. Create response_actions table
CREATE TABLE IF NOT EXISTS response_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RECOMMENDED', -- RECOMMENDED, APPROVED, IN_PROGRESS, COMPLETED, CANCELLED
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_response_actions_incident_id ON response_actions(incident_id);
CREATE INDEX IF NOT EXISTS idx_response_actions_status ON response_actions(status);

-- 5. Create configurable assignment_rules table
CREATE TABLE IF NOT EXISTS assignment_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_type VARCHAR(100) NOT NULL,
    department_code VARCHAR(50) NOT NULL,
    department_name VARCHAR(150) NOT NULL,
    default_team_code VARCHAR(50),
    description TEXT,
    priority_override VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assignment_rules_incident_type ON assignment_rules(incident_type);
CREATE INDEX IF NOT EXISTS idx_assignment_rules_active ON assignment_rules(active);

-- 6. Seed core municipal departments
INSERT INTO departments (code, name, description, active)
VALUES
    ('EMERGENCY', 'Emergency Services', 'Immediate city emergency, fire, and rescue services', TRUE),
    ('PWD', 'Public Works', 'Roadways, stormwater drains, manholes, and public infrastructure', TRUE),
    ('DISASTER_MGMT', 'Disaster Management', 'Urban flood response, cyclone, and emergency disaster relief', TRUE),
    ('TRAFFIC', 'Traffic Department', 'Traffic signals, mobility controllers, and transit routing', TRUE),
    ('WATER', 'Water Department', 'Municipal water pipelines, purification, and distribution', TRUE),
    ('ENVIRONMENT', 'Environment Department', 'Air quality monitoring, hazardous smog, and emissions', TRUE),
    ('SANITATION', 'Sanitation Department', 'Solid waste management, bin collection, and street cleanliness', TRUE),
    ('ELECTRICAL', 'Electrical/Municipal Department', 'Power transformers, street lighting, and grid infrastructure', TRUE),
    ('PARKS', 'Parks Department', 'Urban greenery, tree pruning, parks, and botanical maintenance', TRUE),
    ('MAINTENANCE', 'Municipal Maintenance', 'Civic assets, graffiti cleanup, and minor municipal upkeep', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 7. Seed default active response teams for each department
INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'EMERG_RAPID', 'Emergency Rapid Response Team', '112', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'EMERGENCY'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'PWD_ROAD', 'Road & Infrastructure Repair Team', '1800-PWD-FIX', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'PWD'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'DISASTER_UNIT', 'Disaster Relief & Flood Unit', '1070', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'DISASTER_MGMT'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'TRAFFIC_OPS', 'Traffic Operations Response Team', '1095', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'TRAFFIC'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'WATER_PIPE', 'Water Supply & Pipeline Team', '1916', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'WATER'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'ENV_HAZARD', 'Environmental Hazard Team', '1800-ENV-CLEAN', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'ENVIRONMENT'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'SANIT_SQUAD', 'Waste & Sanitation Squad', '1800-SW-CLEAN', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'SANITATION'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'ELECT_UNIT', 'Streetlight & Electrical Unit', '1912', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'ELECTRICAL'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'PARKS_TREE', 'Parks & Arboriculture Unit', '1800-PARK-FIX', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'PARKS'
ON CONFLICT (code) DO NOTHING;

INSERT INTO teams (department_id, code, name, contact_number, status, active)
SELECT d.id, 'MAINT_CREW', 'Graffiti & Urban Maintenance Crew', '1800-MUNI-FIX', 'AVAILABLE', TRUE
FROM departments d WHERE d.code = 'MAINTENANCE'
ON CONFLICT (code) DO NOTHING;

-- 8. Seed the 18 initial configurable assignment rules
INSERT INTO assignment_rules (incident_type, department_code, department_name, default_team_code, description, active)
VALUES
    ('MAJOR_ROAD_ACCIDENT', 'EMERGENCY', 'Emergency Services', 'EMERG_RAPID', 'Major collision or vehicular emergency', TRUE),
    ('ELECTRICAL_HAZARD', 'EMERGENCY', 'Emergency Services', 'EMERG_RAPID', 'Live exposed wires or transformer sparks', TRUE),
    ('OPEN_MANHOLE', 'PWD', 'Public Works', 'PWD_ROAD', 'Uncovered or broken roadway manhole', TRUE),
    ('FLASH_FLOOD', 'DISASTER_MGMT', 'Disaster Management', 'DISASTER_UNIT', 'Sudden deep flood or submerged underpass', TRUE),
    ('TRAFFIC_SIGNAL_FAILURE', 'TRAFFIC', 'Traffic Department', 'TRAFFIC_OPS', 'Non-functional traffic signal at junction', TRUE),
    ('PIPELINE_BURST', 'WATER', 'Water Department', 'WATER_PIPE', 'Main drinking water pipeline rupture', TRUE),
    ('WATER_CONTAMINATION', 'WATER', 'Water Department', 'WATER_PIPE', 'Contaminated water supply or chemical taint', TRUE),
    ('ROAD_CAVE_IN', 'PWD', 'Public Works', 'PWD_ROAD', 'Severe road collapse or sinkhole', TRUE),
    ('GAS_LEAK', 'EMERGENCY', 'Emergency Services', 'EMERG_RAPID', 'Hazardous gas leakage or explosion risk', TRUE),
    ('AIR_QUALITY_HAZARD', 'ENVIRONMENT', 'Environment Department', 'ENV_HAZARD', 'Hazardous smog cluster or extreme pollution', TRUE),
    ('WASTE_OVERFLOW', 'SANITATION', 'Sanitation Department', 'SANIT_SQUAD', 'Public bin overflow exceeding capacity', TRUE),
    ('ILLEGAL_DUMPING', 'SANITATION', 'Sanitation Department', 'SANIT_SQUAD', 'Unauthorized roadside garbage dump', TRUE),
    ('STREETLIGHT_FAILURE', 'ELECTRICAL', 'Electrical/Municipal Department', 'ELECT_UNIT', 'Dark streetlight cluster on public street', TRUE),
    ('BLOCKED_STORM_DRAIN', 'PWD', 'Public Works', 'PWD_ROAD', 'Clogged stormwater drain causing stagnation', TRUE),
    ('ROAD_CRACK', 'PWD', 'Public Works', 'PWD_ROAD', 'Minor road surface fissure or defect', TRUE),
    ('SIDEWALK_DAMAGE', 'PWD', 'Public Works', 'PWD_ROAD', 'Damaged pedestrian footpath or walkway', TRUE),
    ('TREE_PRUNING', 'PARKS', 'Parks Department', 'PARKS_TREE', 'Overgrown or hazardous tree branch trimming', TRUE),
    ('GRAFFITI', 'MAINTENANCE', 'Municipal Maintenance', 'MAINT_CREW', 'Public property defacement or wall tagging', TRUE)
ON CONFLICT DO NOTHING;
