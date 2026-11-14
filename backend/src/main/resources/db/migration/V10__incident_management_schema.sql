-- ===================================================
-- CivicPulse Migration V10: Incident Management Schema & JDBC Persistence
-- Adds: title, category, status, location_name, reported_by, assigned_to
-- Configures defaults and indexes for parameterized SQL querying
-- ===================================================

DO $$
BEGIN
    -- 1. Add title column if not present
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'incidents' AND column_name = 'title') THEN
        ALTER TABLE incidents ADD COLUMN title VARCHAR(200);
    END IF;

    -- 2. Add category column if not present
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'incidents' AND column_name = 'category') THEN
        ALTER TABLE incidents ADD COLUMN category VARCHAR(100);
    END IF;

    -- 3. Add status column if not present
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'incidents' AND column_name = 'status') THEN
        ALTER TABLE incidents ADD COLUMN status VARCHAR(50) DEFAULT 'OPEN';
    END IF;

    -- 4. Add location_name column if not present
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'incidents' AND column_name = 'location_name') THEN
        ALTER TABLE incidents ADD COLUMN location_name VARCHAR(255);
    END IF;

    -- 5. Add reported_by foreign key referencing users(id) if not present
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'incidents' AND column_name = 'reported_by') THEN
        ALTER TABLE incidents ADD COLUMN reported_by UUID REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    -- 6. Add assigned_to foreign key referencing users(id) if not present
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'incidents' AND column_name = 'assigned_to') THEN
        ALTER TABLE incidents ADD COLUMN assigned_to UUID REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Drop NOT NULL on legacy columns to enable clean, direct inserts with new schema
ALTER TABLE incidents ALTER COLUMN incident_type DROP NOT NULL;
ALTER TABLE incidents ALTER COLUMN source SET DEFAULT 'CITIZEN';
ALTER TABLE incidents ALTER COLUMN current_status SET DEFAULT 'OPEN';

-- Populate default incident_number if not provided
ALTER TABLE incidents ALTER COLUMN incident_number SET DEFAULT ('INC-' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || '-' || SUBSTRING(gen_random_uuid()::text, 1, 8));

-- Synchronize data for all existing rows
UPDATE incidents
SET title = COALESCE(title, incident_type, 'Smart City Incident'),
    category = COALESCE(category, incident_type, 'General'),
    status = COALESCE(status, CASE
        WHEN current_status IN ('DETECTED', 'OPEN') THEN 'OPEN'
        WHEN current_status IN ('ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'PRIORITIZED') THEN 'IN_PROGRESS'
        WHEN current_status IN ('RESOLVED', 'VERIFIED') THEN 'RESOLVED'
        WHEN current_status IN ('CLOSED', 'REJECTED', 'CANCELLED') THEN 'CLOSED'
        ELSE 'OPEN'
    END),
    location_name = COALESCE(location_name, address, 'Smart City Command Centre'),
    reported_by = COALESCE(reported_by, created_by_user_id),
    severity = COALESCE(severity, severity_indicator, priority, 'MEDIUM')
WHERE title IS NULL OR category IS NULL OR status IS NULL OR location_name IS NULL OR reported_by IS NULL OR severity IS NULL;

-- Indexes for performant filtering, search, and user references
CREATE INDEX IF NOT EXISTS idx_incidents_title_lower ON incidents (LOWER(title));
CREATE INDEX IF NOT EXISTS idx_incidents_category ON incidents (category);
CREATE INDEX IF NOT EXISTS idx_incidents_severity ON incidents (severity);
CREATE INDEX IF NOT EXISTS idx_incidents_status_col ON incidents (status);
CREATE INDEX IF NOT EXISTS idx_incidents_reported_by ON incidents (reported_by);
CREATE INDEX IF NOT EXISTS idx_incidents_assigned_to ON incidents (assigned_to);
CREATE INDEX IF NOT EXISTS idx_incidents_location_name_lower ON incidents (LOWER(location_name));
