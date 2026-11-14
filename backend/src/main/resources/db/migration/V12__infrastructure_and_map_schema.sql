-- ==============================================================================
-- CivicPulse Migration V12: Infrastructure Monitoring and GIS Map Schema
-- Enhances infrastructure table with inspection date, maintenance priority, notes
-- Seeds diverse infrastructure assets & public shelters
-- Creates spatial & status indexes for high-performance parameterized map queries
-- ==============================================================================

-- 1. Add missing infrastructure monitoring columns
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'infrastructure' AND column_name = 'last_inspected_at') THEN
        ALTER TABLE infrastructure ADD COLUMN last_inspected_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'infrastructure' AND column_name = 'maintenance_priority') THEN
        ALTER TABLE infrastructure ADD COLUMN maintenance_priority VARCHAR(50) DEFAULT 'MEDIUM';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'infrastructure' AND column_name = 'notes') THEN
        ALTER TABLE infrastructure ADD COLUMN notes TEXT;
    END IF;
END $$;

-- 2. Populate defaults for existing infrastructure rows
UPDATE infrastructure
SET last_inspected_at = COALESCE(last_inspected_at, CURRENT_TIMESTAMP - INTERVAL '15 days'),
    maintenance_priority = COALESCE(maintenance_priority, 'MEDIUM'),
    notes = COALESCE(notes, 'Routine municipal asset monitored by City Operations Command Centre.')
WHERE last_inspected_at IS NULL OR maintenance_priority IS NULL OR notes IS NULL;

-- 3. Indexes for rapid bounding-box and status filtering
CREATE INDEX IF NOT EXISTS idx_infrastructure_status ON infrastructure(status);
CREATE INDEX IF NOT EXISTS idx_infrastructure_type ON infrastructure(infrastructure_type);
CREATE INDEX IF NOT EXISTS idx_infrastructure_coords ON infrastructure(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_emergency_resources_coords ON emergency_resources(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_emergency_resources_type ON emergency_resources(resource_type);

-- 4. Seed diverse infrastructure assets if not already present
INSERT INTO infrastructure (code, name, infrastructure_type, latitude, longitude, address, status, importance_level, maintenance_priority, last_inspected_at, notes)
VALUES
    ('INF-HOSP-01', 'Sector 22 Civil Hospital & Emergency Unit', 'HOSPITAL', 30.7320, 76.7710, 'Sector 22-A Medical Complex', 'OPERATIONAL', 'CRITICAL', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '3 days', 'Primary community trauma and emergency triage center.'),
    ('INF-HOSP-02', 'Sector 32 Multi-Specialty Government Medical College', 'HOSPITAL', 30.7090, 76.7730, 'Sector 32-B Hospital Road', 'OPERATIONAL', 'CRITICAL', 'MEDIUM', CURRENT_TIMESTAMP - INTERVAL '7 days', 'Level 1 trauma center with 800-bed critical care capacity.'),
    ('INF-BRG-02', 'Dakshin Marg Elevated Flyover & Junction Bridge', 'BRIDGE', 30.7180, 76.7750, 'Dakshin Marg Sector 33/34', 'OPERATIONAL', 'PRIMARY', 'LOW', CURRENT_TIMESTAMP - INTERVAL '20 days', 'Six-lane arterial flyover with automated load vibration sensors.'),
    ('INF-WTR-02', 'Sector 39 Central Water Works & Filtration Complex', 'WATER_PIPELINE', 30.7250, 76.7350, 'Sector 39 Utility Zone', 'OPERATIONAL', 'CRITICAL', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '5 days', 'Main reservoir delivering 60 MGD potable water with pressure monitoring.'),
    ('INF-PWR-02', 'Industrial Area 66kV Transformer Yard', 'SUBSTATION', 30.7050, 76.8080, 'Plot 14 Phase 2 Industrial Area', 'DEGRADED', 'CRITICAL', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Feed transformer #2 undergoing thermal stress evaluation; running at 85% capacity.'),
    ('INF-SHL-01', 'Sector 19 Disaster Relief Shelter & Community Center', 'SHELTER', 30.7380, 76.7910, 'Sector 19-C Community Complex', 'OPERATIONAL', 'PRIMARY', 'LOW', CURRENT_TIMESTAMP - INTERVAL '10 days', 'Designated municipal refuge shelter with standby generator and emergency supplies.'),
    ('INF-SHL-02', 'Sector 42 Sports Complex Evacuation Center', 'SHELTER', 30.7260, 76.7510, 'Sector 42 Lake Road', 'OPERATIONAL', 'PRIMARY', 'MEDIUM', CURRENT_TIMESTAMP - INTERVAL '12 days', 'High-capacity municipal evacuation zone equipped with water storage and medical bays.'),
    ('INF-ROAD-03', 'Himalaya Marg Underpass Corridor', 'ROAD', 30.7340, 76.7620, 'Himalaya Marg Sector 22/23 Crossing', 'UNDER_MAINTENANCE', 'PRIMARY', 'CRITICAL', CURRENT_TIMESTAMP - INTERVAL '2 days', 'Road resurfacing and storm drain clearing in progress.')
ON CONFLICT (code) DO UPDATE
SET last_inspected_at = EXCLUDED.last_inspected_at,
    maintenance_priority = EXCLUDED.maintenance_priority,
    notes = EXCLUDED.notes;

-- 5. Seed emergency shelters into emergency_resources if needed
INSERT INTO emergency_resources (name, resource_type, latitude, longitude, address, contact_number, available, capacity, operational_hours)
VALUES
    ('North Zone Evacuation Shelter Sector 10', 'EMERGENCY_SHELTER', 30.7580, 76.7900, 'Sector 10-A Leisure Valley Hall', '+91-172-2749911', true, 400, '24/7'),
    ('West Zone Flood Relief Shelter Sector 38', 'EMERGENCY_SHELTER', 30.7450, 76.7450, 'Sector 38 Community Center', '+91-172-2698822', true, 300, '24/7'),
    ('Chandigarh Red Cross Emergency Center', 'EMERGENCY_SHELTER', 30.7390, 76.7760, 'Sector 16 Madhya Marg', '+91-172-2740055', true, 250, '24/7')
ON CONFLICT DO NOTHING;

