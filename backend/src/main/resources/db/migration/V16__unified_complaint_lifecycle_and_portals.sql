-- ==============================================================================
-- CivicPulse Migration V16: Unified Complaint Lifecycle, Portals, Attachments & SLA
-- ==============================================================================

-- 1. Complaint sequence for human-readable record numbers (e.g. CP-2026-000001)
CREATE SEQUENCE IF NOT EXISTS complaint_record_seq START WITH 1 INCREMENT BY 1;

-- 2. Ensure all required municipal departments exist
INSERT INTO departments (code, name, description, active)
VALUES
    ('HEALTH', 'Public Health Department', 'Municipal sanitation health, epidemic control, and medical hygiene', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 3. Enhance complaints table with department, assignee, subcategory, SLA, and resolution fields
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS subcategory VARCHAR(100);
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS department_id UUID REFERENCES departments(id) ON DELETE SET NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS assigned_to_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS priority VARCHAR(20) DEFAULT 'MEDIUM';
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS sla_minutes INT DEFAULT 1440;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS sla_deadline TIMESTAMPTZ;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS sla_status VARCHAR(50) DEFAULT 'ON_TRACK';
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS resolution_notes TEXT;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS resolution_evidence_url VARCHAR(500);
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;

-- Backfill default SLA deadline for existing complaints without deadline (started_at + 24 hours)
UPDATE complaints
SET sla_deadline = created_at + INTERVAL '24 hours'
WHERE sla_deadline IS NULL;

-- Backfill department_id for existing complaints based on category
UPDATE complaints
SET department_id = (SELECT id FROM departments WHERE code = 'WATER' LIMIT 1)
WHERE department_id IS NULL AND category IN ('WATER_SUPPLY', 'WATER', 'Water Supply');

UPDATE complaints
SET department_id = (SELECT id FROM departments WHERE code = 'SANITATION' LIMIT 1)
WHERE department_id IS NULL AND category IN ('SOLID_WASTE_MANAGEMENT', 'SANITATION', 'Sanitation');

UPDATE complaints
SET department_id = (SELECT id FROM departments WHERE code = 'ELECTRICAL' LIMIT 1)
WHERE department_id IS NULL AND category IN ('STREET_LIGHTING', 'ELECTRICAL', 'Electricity');

UPDATE complaints
SET department_id = (SELECT id FROM departments WHERE code = 'PWD' LIMIT 1)
WHERE department_id IS NULL AND category IN ('ROADS_AND_TRAFFIC', 'SEWAGE_AND_DRAINAGE', 'Infrastructure');

-- Default fallback department
UPDATE complaints
SET department_id = (SELECT id FROM departments WHERE code = 'PWD' LIMIT 1)
WHERE department_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_complaints_department_id ON complaints(department_id);
CREATE INDEX IF NOT EXISTS idx_complaints_assigned_to ON complaints(assigned_to_user_id);
CREATE INDEX IF NOT EXISTS idx_complaints_priority ON complaints(priority);
CREATE INDEX IF NOT EXISTS idx_complaints_sla_status ON complaints(sla_status);
CREATE INDEX IF NOT EXISTS idx_complaints_sla_deadline ON complaints(sla_deadline);

-- 4. Create complaint_attachments table for secure image storage
CREATE TABLE IF NOT EXISTS complaint_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    attachment_type VARCHAR(50) NOT NULL DEFAULT 'CITIZEN_EVIDENCE', -- CITIZEN_EVIDENCE, RESOLUTION_PROOF
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comp_att_complaint_id ON complaint_attachments(complaint_id);
CREATE INDEX IF NOT EXISTS idx_comp_att_uploaded_by ON complaint_attachments(uploaded_by);

-- 5. Create complaint_receipts table for persistent digital acknowledgement receipts
CREATE TABLE IF NOT EXISTS complaint_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE UNIQUE,
    receipt_number VARCHAR(100) NOT NULL UNIQUE,
    complaint_record_number VARCHAR(50) NOT NULL,
    citizen_name VARCHAR(150) NOT NULL,
    citizen_phone VARCHAR(50),
    title VARCHAR(200),
    category VARCHAR(100),
    department_name VARCHAR(150),
    location_address VARCHAR(255),
    priority VARCHAR(20),
    status VARCHAR(50),
    sla_deadline TIMESTAMPTZ,
    expected_resolution_hours INT,
    acknowledgement_message TEXT,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_comp_receipt_complaint ON complaint_receipts(complaint_id);
CREATE INDEX IF NOT EXISTS idx_comp_receipt_number ON complaint_receipts(receipt_number);

-- 6. Create complaint_status_history table for auditing lifecycle transitions
CREATE TABLE IF NOT EXISTS complaint_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    changed_by_name VARCHAR(150),
    changed_by_role VARCHAR(50),
    change_reason VARCHAR(255),
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comp_status_hist_complaint ON complaint_status_history(complaint_id);
CREATE INDEX IF NOT EXISTS idx_comp_status_hist_created ON complaint_status_history(created_at);

-- 7. Enhance notifications table with complaint link columns
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS complaint_id UUID REFERENCES complaints(id) ON DELETE CASCADE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS complaint_number VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_notifications_complaint_id ON notifications(complaint_id);
CREATE INDEX IF NOT EXISTS idx_notifications_complaint_num ON notifications(complaint_number);

