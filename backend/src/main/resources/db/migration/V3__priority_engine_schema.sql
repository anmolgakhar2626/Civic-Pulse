-- ===================================================
-- CivicPulse Migration V3: Priority Rule Engine Schema & Default Rules
-- ===================================================

-- 1. Extend incidents table with priority evaluation attributes
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS priority_tier VARCHAR(20);
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS priority_label VARCHAR(50);
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS priority_reason TEXT;
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS severity VARCHAR(50);
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS public_exposure VARCHAR(50);
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS safety_impact VARCHAR(100);
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS priority_evaluated_at TIMESTAMPTZ;
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS priority_rule_id VARCHAR(100);

-- 2. Extend priority_rules table
ALTER TABLE priority_rules ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE priority_rules ADD COLUMN IF NOT EXISTS reason_template TEXT;

-- 3. Seed Default System Priority Rules (Safe upsert using rule_id)
INSERT INTO priority_rules (rule_id, name, incident_type, tier, priority, sla_minutes, conditions_json, reason_template, enabled)
VALUES
-- TIER 1: CRITICAL (<= 30 mins)
('LIVE_ELECTRICAL_WIRE', 'Live Exposed Electrical Wire', 'ELECTRICAL_HAZARD', 'CRITICAL', 'CRITICAL', 30,
 '{"exposedLiveWire": true}', '["Live exposed electrical wire hazard", "Immediate electrocution and public safety hazard", "Critical emergency response required"]', TRUE),

('MAJOR_ROAD_ACCIDENT', 'Major Road Collision', 'TRAFFIC_COLLISION', 'CRITICAL', 'CRITICAL', 30,
 '{"severity": "EXTREME"}', '["Major vehicular collision on municipal roadway", "High public exposure and potential casualty risk", "Immediate emergency dispatch required"]', TRUE),

('TRANSFORMER_FIRE_RISK', 'Transformer Spark and Fire Hazard', 'ELECTRICAL_HAZARD', 'CRITICAL', 'CRITICAL', 30,
 '{"transformerSpark": true}', '["High-voltage transformer spark / electrical fire risk", "Critical danger to surrounding public infrastructure"]', TRUE),

('OPEN_MANHOLE_BUSY_ROAD', 'Open Roadway Manhole on High Traffic Corridor', 'OPEN_MANHOLE', 'CRITICAL', 'CRITICAL', 30,
 '{"busyRoad": true}', '["Open uncovered manhole on high-traffic road", "Severe danger to moving vehicles and pedestrians"]', TRUE),

('SUBMERGED_UNDERPASS', 'Flash Flood / Submerged Underpass', 'FLASH_FLOOD', 'CRITICAL', 'CRITICAL', 30,
 '{"submergedUnderpass": true}', '["Sudden deep flash flood submerging roadway underpass", "Life-threatening vehicular entrapment hazard"]', TRUE),

('WASTE_OBSTRUCTION_EMERGENCY', 'Waste Obstruction Blocking Emergency Route', 'WASTE_OVERFLOW', 'CRITICAL', 'CRITICAL', 30,
 '{"blockingEmergencyAccess": true}', '["Waste obstruction blocking critical emergency vehicle access", "Immediate clearance required for emergency response"]', TRUE),

-- TIER 2: HIGH (2–4 hours)
('TRAFFIC_SIGNAL_MAJOR_JUNCTION', 'Traffic Signal Failure at Major Junction', 'TRAFFIC_SIGNAL_FAILURE', 'HIGH', 'HIGH', 240,
 '{"majorJunction": true}', '["Traffic signal non-functional at major arterial junction", "Significant arterial gridlock and intersection hazard", "Priority police dispatch required"]', TRUE),

('MAIN_PIPELINE_BURST', 'Main Water Pipeline Burst', 'WATER_PIPE_BURST', 'HIGH', 'HIGH', 240,
 '{"severity": "HIGH"}', '["Primary municipal water transmission pipeline rupture", "Widespread water pressure loss and localized flooding risk"]', TRUE),

('DRINKING_WATER_CONTAMINATION', 'Drinking Water Contamination Hazard', 'WATER_CONTAMINATION', 'HIGH', 'HIGH', 240,
 '{}', '["Detected contamination in drinking water supply", "Public health risk requiring immediate containment"]', TRUE),

('ROAD_CAVE_IN', 'Roadway Cave-In / Sinkhole', 'ROAD_DAMAGE', 'HIGH', 'HIGH', 240,
 '{"roadCaveIn": true}', '["Structural roadway cave-in / sinkhole detected", "Severe lane obstruction and structural failure risk"]', TRUE),

('SEVERE_POTHOLE', 'Severe Tire-Burst Road Pothole', 'ROAD_DAMAGE', 'HIGH', 'HIGH', 240,
 '{"severity": "HIGH"}', '["Deep pothole posing direct tire-burst or motorcycle rollover hazard", "Arterial roadway safety hazard"]', TRUE),

('GAS_LEAKAGE', 'Hazardous Gas Pipeline Leak', 'GAS_LEAK', 'HIGH', 'HIGH', 240,
 '{}', '["Flammable or toxic gas leakage detected", "Combustion risk requiring utility containment team"]', TRUE),

('PM25_HAZARDOUS_SMOG', 'Severe Smog Cluster (PM2.5 > 300)', 'AIR_POLLUTION', 'HIGH', 'HIGH', 240,
 '{"minPm25": 300}', '["PM2.5 particulate level exceeds 300 µg/m³ (Hazardous)", "Severe health risk across municipal district"]', TRUE),

-- TIER 3: MEDIUM (12–24 hours)
('WASTE_BIN_OVERFLOW_NORMAL', 'Smart Waste Bin >90% Capacity', 'WASTE_OVERFLOW', 'MEDIUM', 'MEDIUM', 1440,
 '{"minFillPercentage": 90, "blockingEmergencyAccess": false}', '["Smart waste bin fill exceeds 90% in residential sector", "Scheduled route collection required"]', TRUE),

('TRAFFIC_SIGNAL_MINOR_JUNCTION', 'Traffic Signal Failure at Minor Junction', 'TRAFFIC_SIGNAL_FAILURE', 'MEDIUM', 'MEDIUM', 1440,
 '{"majorJunction": false}', '["Traffic signal non-functional at minor local intersection", "Low-to-moderate traffic density"]', TRUE),

('DEAD_STREETLIGHTS', 'Streetlight Cluster Outage', 'STREETLIGHT_OUTAGE', 'MEDIUM', 'MEDIUM', 1440,
 '{}', '["Public lighting luminaire failure", "Night-time visibility impairment in municipal ward"]', TRUE),

('CLOGGED_STORM_DRAIN', 'Clogged Roadside Storm Drain', 'DRAINAGE_ISSUE', 'MEDIUM', 'MEDIUM', 1440,
 '{}', '["Roadside storm drain clogged with silt and debris", "Runoff accumulation risk during rainfall"]', TRUE),

('ILLEGAL_DUMPING', 'Illegal Roadside Dumping', 'ILLEGAL_DUMPING', 'MEDIUM', 'MEDIUM', 1440,
 '{}', '["Unauthorized debris dumped along public right-of-way", "Sanitation crew cleanup scheduled"]', TRUE),

-- TIER 4: LOW (48–72 hours)
('MINOR_ROAD_CRACKS', 'Minor Road Surface Cracks', 'ROAD_MAINTENANCE', 'LOW', 'LOW', 2880,
 '{}', '["Superficial pavement fissures", "Scheduled for routine road resurfacing cycle"]', TRUE),

('SIDEWALK_DISREPAIR', 'Sidewalk Paving Disrepair', 'SIDEWALK_MAINTENANCE', 'LOW', 'LOW', 2880,
 '{}', '["Loose pedestrian paving slabs", "Scheduled for neighborhood maintenance crew"]', TRUE),

('FADED_LANE_MARKINGS', 'Faded Roadway Lane Markings', 'ROAD_MARKINGS', 'LOW', 'LOW', 2880,
 '{}', '["Faded thermal traffic lane markings", "Scheduled for cyclic night-time repainting"]', TRUE),

('MISSING_STREET_SIGN', 'Missing Street Name or Traffic Sign', 'STREET_SIGNAGE', 'LOW', 'LOW', 2880,
 '{}', '["Damaged or missing civic street sign", "Fabrication and installation scheduled"]', TRUE),

('TREE_PRUNING', 'Municipal Tree Pruning Required', 'TREE_PRUNING', 'LOW', 'LOW', 2880,
 '{}', '["Overgrown municipal tree branches", "Routine horticultural maintenance"]', TRUE),

('PARK_MAINTENANCE', 'Public Park Grounds Maintenance', 'PARK_MAINTENANCE', 'LOW', 'LOW', 2880,
 '{}', '["Park furniture or landscaping maintenance", "Scheduled park division ticket"]', TRUE),

('GRAFFITI_REMOVAL', 'Civic Property Graffiti Removal', 'VANDALISM', 'LOW', 'LOW', 2880,
 '{}', '["Unauthorized graffiti on civic property", "Scheduled for pressure wash cleaning"]', TRUE)

ON CONFLICT (rule_id) DO UPDATE SET
    name = EXCLUDED.name,
    incident_type = EXCLUDED.incident_type,
    tier = EXCLUDED.tier,
    priority = EXCLUDED.priority,
    sla_minutes = EXCLUDED.sla_minutes,
    conditions_json = EXCLUDED.conditions_json,
    reason_template = EXCLUDED.reason_template,
    enabled = EXCLUDED.enabled;

-- Index for rule lookups
CREATE INDEX IF NOT EXISTS idx_priority_rules_type_enabled ON priority_rules(incident_type, enabled);
