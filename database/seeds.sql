-- ==============================================================================
-- CivicPulse Smart City Command Centre — Safe Municipal Seed Data
-- Standard reference seeds for demonstration, evaluation, and test environments.
-- Default Password for all seed demo accounts: Admin@CivicPulse2026
-- BCrypt Hash: $2a$10$ccj1G6O55Qr.mk0zua./4u4qRvsN./ppk0XaCMVeFl8SDrmk6EWxa
-- ==============================================================================

-- 1. Municipal Roles
INSERT INTO roles (id, name, description, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111101', 'ADMIN', 'System Administrator with full governance access', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111102', 'COMMAND_OPERATOR', 'City Command Centre Operator managing real-time telemetry and dispatch', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111103', 'DEPARTMENT_OFFICER', 'Department-specific nodal officer managing grievance queues', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111104', 'FIELD_WORKER', 'Field response crew member executing on-site tasks and resolution proof', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111105', 'CITIZEN', 'Citizen user with grievance intake and tracking privileges', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 2. Municipal Departments
INSERT INTO departments (id, code, name, description, active)
VALUES
    ('5b43942a-6f73-4e4c-927d-b845f8821604', 'TRAFFIC', 'Traffic Department', 'City traffic operations, signal controllers, and mobility flow', TRUE),
    ('a2b3c4d5-e6f7-4890-abcd-ef1234567890', 'PWD', 'Public Works', 'Roads, bridges, stormwater drainage, and public physical infrastructure', TRUE),
    ('b3c4d5e6-f7a8-4901-bcde-f12345678901', 'WATER', 'Water Department', 'Municipal water distribution, pipeline maintenance, and pressure monitoring', TRUE),
    ('c4d5e6f7-a8b9-4012-cdef-123456789012', 'ELECTRICAL', 'Electrical/Municipal Department', 'Street lighting, municipal power grids, and high-voltage safety', TRUE),
    ('d5e6f7a8-b9c0-4123-def0-234567890123', 'SANITATION', 'Sanitation Department', 'Solid waste management, bin collection, and neighborhood cleanliness', TRUE),
    ('e6f7a8b9-c0d1-4234-ef01-345678901234', 'HEALTH', 'Public Health Department', 'Hygiene inspections, public medical alerts, and health safety', TRUE),
    ('f7a8b9c0-d1e2-4345-f012-456789012345', 'ENVIRONMENT', 'Environment Department', 'Air quality monitoring, green cover, and environmental noise control', TRUE),
    ('08b9c0d1-e2f3-4456-0123-567890123456', 'EMERGENCY', 'Emergency Services', 'Unified municipal first responders, fire, and disaster dispatch', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 3. Department Teams
INSERT INTO teams (id, department_id, code, name, active)
VALUES
    ('88593f71-979a-4da8-b042-20047c050a20', '5b43942a-6f73-4e4c-927d-b845f8821604', 'TRAFFIC_OPS', 'Traffic Operations Response Team', TRUE),
    ('99593f71-979a-4da8-b042-20047c050a21', 'a2b3c4d5-e6f7-4890-abcd-ef1234567890', 'PWD_REPAIR', 'PWD Rapid Road Repair Unit', TRUE),
    ('aa593f71-979a-4da8-b042-20047c050a22', 'b3c4d5e6-f7a8-4901-bcde-f12345678901', 'WATER_EMERGENCY', 'Water Emergency Response Unit', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 4. Default Demonstration Users
-- Password: Admin@CivicPulse2026
INSERT INTO users (id, username, email, password_hash, full_name, phone, mobile_number, role, department_id, team_id, verified_citizen, genuine_complaint_count, mobile_verified, active, enabled)
VALUES
    ('11111111-2222-3333-4444-555555555501', 'admin', 'admin@civicpulse.gov', '$2a$10$ccj1G6O55Qr.mk0zua./4u4qRvsN./ppk0XaCMVeFl8SDrmk6EWxa', 'System Administrator', '+919876543210', '+919876543210', 'ADMIN', NULL, NULL, FALSE, 0, TRUE, TRUE, TRUE),
    ('11111111-2222-3333-4444-555555555502', 'operator', 'operator@civicpulse.gov', '$2a$10$ccj1G6O55Qr.mk0zua./4u4qRvsN./ppk0XaCMVeFl8SDrmk6EWxa', 'Command Centre Operator', '+919876543211', '+919876543211', 'COMMAND_OPERATOR', NULL, NULL, FALSE, 0, TRUE, TRUE, TRUE),
    ('9f316620-cac4-4928-b8b3-91283e6d9a52', 'officer_traffic', 'traffic@civicpulse.gov', '$2a$10$ccj1G6O55Qr.mk0zua./4u4qRvsN./ppk0XaCMVeFl8SDrmk6EWxa', 'Traffic Department Officer', '+919876543212', '+919876543212', 'DEPARTMENT_OFFICER', '5b43942a-6f73-4e4c-927d-b845f8821604', NULL, FALSE, 0, TRUE, TRUE, TRUE),
    ('0ef1937f-53ee-4029-8c0b-2cb8c547db58', 'field_worker', 'field@civicpulse.gov', '$2a$10$ccj1G6O55Qr.mk0zua./4u4qRvsN./ppk0XaCMVeFl8SDrmk6EWxa', 'Field Operations Crew', '+919876543213', '+919876543213', 'FIELD_WORKER', '5b43942a-6f73-4e4c-927d-b845f8821604', '88593f71-979a-4da8-b042-20047c050a20', FALSE, 0, TRUE, TRUE, TRUE),
    ('11111111-2222-3333-4444-555555555505', 'citizen', 'citizen@smartcity.gov', '$2a$10$ccj1G6O55Qr.mk0zua./4u4qRvsN./ppk0XaCMVeFl8SDrmk6EWxa', 'Citizen Resident', '+919876543210', '+919876543210', 'CITIZEN', NULL, NULL, TRUE, 5, TRUE, TRUE, TRUE)
ON CONFLICT (username) DO NOTHING;

-- 5. User-Role Association
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = u.role
ON CONFLICT DO NOTHING;

-- 6. Standard SLA Policies
INSERT INTO sla_policies (id, category, severity, response_time_minutes, resolution_time_minutes, enabled, created_at)
VALUES
    (gen_random_uuid(), 'Flood', 'CRITICAL', 15, 60, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Fire', 'CRITICAL', 10, 45, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Infrastructure', 'HIGH', 30, 240, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Water Supply', 'HIGH', 30, 240, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Traffic', 'HIGH', 20, 180, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Street Lighting', 'MEDIUM', 60, 1440, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Solid Waste', 'MEDIUM', 120, 1440, TRUE, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'General', 'LOW', 240, 2880, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
