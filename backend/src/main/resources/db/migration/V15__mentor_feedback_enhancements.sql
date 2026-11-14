-- ===================================================
-- CivicPulse Migration V15: Mentor Feedback Enhancements
-- 1. Mobile Number OTP Authentication Schema
-- 2. Verified Citizen Badge & Reputation Schema
-- 3. Government Grievance Portal Integration Schema
-- ===================================================

-- 1. Mobile Number & Reputation Fields in Users Table
ALTER TABLE users ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_mobile_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS genuine_complaint_count INT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified_citizen BOOLEAN DEFAULT FALSE;

-- Ensure demo users have mobile verification and demo phone numbers
UPDATE users
SET is_mobile_verified = TRUE,
    mobile_number = COALESCE(phone, '+919876543210')
WHERE username IN ('admin', 'operator', 'officer_traffic', 'field_worker', 'citizen');

CREATE INDEX IF NOT EXISTS idx_users_mobile_number ON users (mobile_number);
CREATE INDEX IF NOT EXISTS idx_users_verified_citizen ON users (is_verified_citizen);

-- 2. Mobile OTPs Table (No plain-text OTPs, salted hash, expiry & attempt limits)
CREATE TABLE IF NOT EXISTS mobile_otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mobile_number VARCHAR(30) NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    otp_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL DEFAULT 'REGISTRATION',
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    resend_after TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mobile_otps_mobile_purpose ON mobile_otps (mobile_number, purpose);
CREATE INDEX IF NOT EXISTS idx_mobile_otps_created_at ON mobile_otps (created_at);

-- 3. Complaint Verification Enhancements in Complaints Table
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS title VARCHAR(200);
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS complaint_verification_status VARCHAR(50) DEFAULT 'UNVERIFIED';
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS verified_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS verification_notes TEXT;

-- Synchronize title from description or category if missing
UPDATE complaints
SET title = COALESCE(title, SUBSTRING(description, 1, 60), category, 'Citizen Grievance')
WHERE title IS NULL;

CREATE INDEX IF NOT EXISTS idx_complaints_verification_status ON complaints (complaint_verification_status);
CREATE INDEX IF NOT EXISTS idx_complaints_citizen_id ON complaints (citizen_id);

-- 4. Complaint Verification History (Auditable Trail)
CREATE TABLE IF NOT EXISTS complaint_verification_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    citizen_id UUID REFERENCES users(id) ON DELETE SET NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    decision_reason TEXT,
    verified_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    verified_by_name VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_verif_hist_complaint_id ON complaint_verification_history (complaint_id);
CREATE INDEX IF NOT EXISTS idx_verif_hist_citizen_id ON complaint_verification_history (citizen_id);

-- 5. Government Grievance Portals Configuration
CREATE TABLE IF NOT EXISTS government_portals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portal_code VARCHAR(50) NOT NULL UNIQUE,
    portal_name VARCHAR(150) NOT NULL,
    endpoint_url VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_sandbox BOOLEAN NOT NULL DEFAULT TRUE,
    auth_type VARCHAR(50) NOT NULL DEFAULT 'API_KEY',
    config_metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial sandbox portals
INSERT INTO government_portals (id, portal_code, portal_name, endpoint_url, is_active, is_sandbox, auth_type, config_metadata)
VALUES
    ('22222222-2222-2222-2222-222222222201', 'CPGRAMS_SANDBOX', 'Central Public Grievance Redress & Monitoring System (Sandbox)', 'https://sandbox.cpgrams.gov.in/api/v1', TRUE, TRUE, 'API_KEY', '{"jurisdiction":"NATIONAL","sla_days":15}'),
    ('22222222-2222-2222-2222-222222222202', 'STATE_PGRS_SANDBOX', 'State Municipal Public Grievance Redressal (Sandbox)', 'https://sandbox.pgrs.state.gov.in/api/v2', TRUE, TRUE, 'OAUTH2', '{"jurisdiction":"STATE","sla_days":7}')
ON CONFLICT (portal_code) DO NOTHING;

-- 6. External Grievances (Linked to Complaints with Idempotency Key)
CREATE TABLE IF NOT EXISTS external_grievances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    portal_id UUID NOT NULL REFERENCES government_portals(id) ON DELETE RESTRICT,
    portal_code VARCHAR(50) NOT NULL,
    acknowledgement_number VARCHAR(100) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    external_status VARCHAR(50) NOT NULL DEFAULT 'ACKNOWLEDGED',
    status_description TEXT,
    officer_in_charge VARCHAR(150),
    target_resolution_date TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ext_grv_complaint_id ON external_grievances (complaint_id);
CREATE INDEX IF NOT EXISTS idx_ext_grv_ack_number ON external_grievances (acknowledgement_number);
CREATE INDEX IF NOT EXISTS idx_ext_grv_idempotency_key ON external_grievances (idempotency_key);

-- 7. External Grievance Status Progress History
CREATE TABLE IF NOT EXISTS grievance_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_grievance_id UUID NOT NULL REFERENCES external_grievances(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    remarks TEXT,
    action_taken_by VARCHAR(150),
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_payload TEXT
);

CREATE INDEX IF NOT EXISTS idx_grv_hist_external_grv ON grievance_status_history (external_grievance_id);

-- 8. Grievance Sync Logs (Audit for submissions, polls, timeouts, retries)
CREATE TABLE IF NOT EXISTS grievance_sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_grievance_id UUID REFERENCES external_grievances(id) ON DELETE CASCADE,
    portal_code VARCHAR(50) NOT NULL,
    sync_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100),
    request_payload TEXT,
    response_payload TEXT,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    sync_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sync_logs_grv_id ON grievance_sync_logs (external_grievance_id);
CREATE INDEX IF NOT EXISTS idx_sync_logs_timestamp ON grievance_sync_logs (sync_timestamp);

-- 9. Acknowledgement Documents
CREATE TABLE IF NOT EXISTS acknowledgement_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_grievance_id UUID NOT NULL REFERENCES external_grievances(id) ON DELETE CASCADE,
    document_reference VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL DEFAULT 'ACKNOWLEDGEMENT_RECEIPT',
    document_title VARCHAR(200) NOT NULL,
    document_url VARCHAR(500),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    file_size_bytes BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ack_docs_grv_id ON acknowledgement_documents (external_grievance_id);
