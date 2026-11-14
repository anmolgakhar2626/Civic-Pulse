-- ===================================================
-- CivicPulse Migration V4: SLA & Escalation Engine Schema Enhancements
-- ===================================================

-- 1. Extend sla_records table with required SLA fields
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS priority_tier VARCHAR(20);
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS target_minutes INT;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS acknowledged_at TIMESTAMPTZ;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ON_TRACK';
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS remaining_minutes BIGINT;
ALTER TABLE sla_records ADD COLUMN IF NOT EXISTS escalation_level VARCHAR(50) DEFAULT 'NONE';

-- 2. Populate values from existing columns if rows exist
UPDATE sla_records SET target_minutes = target_sla_minutes WHERE target_minutes IS NULL AND target_sla_minutes IS NOT NULL;
UPDATE sla_records SET status = current_sla_status WHERE status IS NULL OR status = 'ON_TRACK';
UPDATE sla_records SET started_at = created_at WHERE started_at IS NULL;

-- 3. Required performance indexes for SLA & Escalation monitoring
CREATE INDEX IF NOT EXISTS idx_sla_records_incident_id ON sla_records(incident_id);
CREATE INDEX IF NOT EXISTS idx_sla_records_status_new ON sla_records(status);
CREATE INDEX IF NOT EXISTS idx_sla_records_deadline_at ON sla_records(deadline_at);
CREATE INDEX IF NOT EXISTS idx_sla_records_escalation_level ON sla_records(escalation_level);
