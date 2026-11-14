-- ===================================================
-- CivicPulse Migration V2: Incident Management Updates
-- 1. Make priority / tier / SLA fields nullable until Priority Engine is implemented
-- 2. Enhance incident_status_history with remarks and changed_at
-- 3. Add database indexes for commonly queried incident fields
-- ===================================================

-- 1. Make priority, tier, and SLA columns nullable
ALTER TABLE incidents ALTER COLUMN assigned_tier DROP NOT NULL;
ALTER TABLE incidents ALTER COLUMN priority DROP NOT NULL;
ALTER TABLE incidents ALTER COLUMN sla_minutes DROP NOT NULL;
ALTER TABLE incidents ALTER COLUMN sla_deadline DROP NOT NULL;

-- 2. Adjust incident_status_history columns
ALTER TABLE incident_status_history ALTER COLUMN changed_by_username DROP NOT NULL;
ALTER TABLE incident_status_history ADD COLUMN IF NOT EXISTS changed_by VARCHAR(100);
ALTER TABLE incident_status_history ADD COLUMN IF NOT EXISTS remarks TEXT;
ALTER TABLE incident_status_history ADD COLUMN IF NOT EXISTS changed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

-- Populate default values from existing columns if any rows exist
UPDATE incident_status_history SET remarks = notes WHERE remarks IS NULL AND notes IS NOT NULL;
UPDATE incident_status_history SET changed_by = changed_by_username WHERE changed_by IS NULL AND changed_by_username IS NOT NULL;
UPDATE incident_status_history SET changed_at = created_at WHERE changed_at IS NULL;

-- 3. Commonly queried incident fields indexes
CREATE INDEX IF NOT EXISTS idx_incidents_type ON incidents(incident_type);
CREATE INDEX IF NOT EXISTS idx_incidents_source ON incidents(source);
CREATE INDEX IF NOT EXISTS idx_incidents_created_at ON incidents(created_at);
CREATE INDEX IF NOT EXISTS idx_incidents_assigned_team_id ON incidents(assigned_team_id);
CREATE INDEX IF NOT EXISTS idx_incident_history_changed_at ON incident_status_history(changed_at);
