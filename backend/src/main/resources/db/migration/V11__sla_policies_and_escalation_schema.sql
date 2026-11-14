-- ===================================================
-- CivicPulse Migration V11: SLA Policies, Tracking, and Escalation Schema
-- ===================================================

-- 1. Create sla_policies table
CREATE TABLE IF NOT EXISTS sla_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    response_time_minutes INT NOT NULL,
    resolution_time_minutes INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sla_policy_category_severity UNIQUE (category, severity)
);

CREATE INDEX IF NOT EXISTS idx_sla_policies_cat_sev ON sla_policies(category, severity);
CREATE INDEX IF NOT EXISTS idx_sla_policies_enabled ON sla_policies(enabled);

-- 2. Enhance sla_records table with Prompt 4 tracking columns
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS policy_id UUID REFERENCES sla_policies(id) ON DELETE SET NULL;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS response_deadline TIMESTAMPTZ;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS resolution_deadline TIMESTAMPTZ;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS first_responded_at TIMESTAMPTZ;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS last_escalated_at TIMESTAMPTZ;

-- Backfill resolution_deadline and first_responded_at from existing columns if empty
UPDATE sla_records 
SET resolution_deadline = deadline_at 
WHERE resolution_deadline IS NULL AND deadline_at IS NOT NULL;

UPDATE sla_records 
SET first_responded_at = acknowledged_at 
WHERE first_responded_at IS NULL AND acknowledged_at IS NOT NULL;

-- Backfill response_deadline if null (e.g. started_at + 15 min or 20% of target)
UPDATE sla_records 
SET response_deadline = started_at + INTERVAL '15 minutes' 
WHERE response_deadline IS NULL AND started_at IS NOT NULL;

-- 3. Create sla_escalation_history table for auditing automatic escalations
CREATE TABLE IF NOT EXISTS sla_escalation_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    sla_record_id UUID REFERENCES sla_records(id) ON DELETE CASCADE,
    previous_escalation_level VARCHAR(50),
    new_escalation_level VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    reason TEXT,
    escalated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sla_escalation_incident ON sla_escalation_history(incident_id);
CREATE INDEX IF NOT EXISTS idx_sla_escalation_time ON sla_escalation_history(escalated_at);
CREATE INDEX IF NOT EXISTS idx_sla_escalation_record ON sla_escalation_history(sla_record_id);

-- 4. Seed Standard Default SLA Policies across categories & severities
INSERT INTO sla_policies (category, severity, response_time_minutes, resolution_time_minutes, enabled)
VALUES
    -- Flood
    ('Flood', 'CRITICAL', 10, 30, true),
    ('Flood', 'HIGH', 20, 120, true),
    ('Flood', 'MEDIUM', 45, 360, true),
    ('Flood', 'LOW', 90, 720, true),

    -- Fire
    ('Fire', 'CRITICAL', 5, 20, true),
    ('Fire', 'HIGH', 15, 60, true),
    ('Fire', 'MEDIUM', 30, 180, true),
    ('Fire', 'LOW', 60, 360, true),

    -- Traffic
    ('Traffic', 'CRITICAL', 10, 45, true),
    ('Traffic', 'HIGH', 20, 90, true),
    ('Traffic', 'MEDIUM', 45, 240, true),
    ('Traffic', 'LOW', 90, 480, true),

    -- Pollution
    ('Pollution', 'CRITICAL', 15, 60, true),
    ('Pollution', 'HIGH', 30, 180, true),
    ('Pollution', 'MEDIUM', 60, 720, true),
    ('Pollution', 'LOW', 120, 1440, true),

    -- Infrastructure
    ('Infrastructure', 'CRITICAL', 15, 60, true),
    ('Infrastructure', 'HIGH', 30, 240, true),
    ('Infrastructure', 'MEDIUM', 60, 720, true),
    ('Infrastructure', 'LOW', 120, 1440, true),

    -- Default fallback policy
    ('DEFAULT', 'CRITICAL', 10, 30, true),
    ('DEFAULT', 'HIGH', 30, 120, true),
    ('DEFAULT', 'MEDIUM', 60, 720, true),
    ('DEFAULT', 'LOW', 120, 1440, true)
ON CONFLICT (category, severity) DO NOTHING;

