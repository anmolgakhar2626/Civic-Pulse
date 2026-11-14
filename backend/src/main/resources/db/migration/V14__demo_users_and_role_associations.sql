-- ===================================================
-- CivicPulse - V14 Demo Users and Role Associations
-- Ensure idempotent demo roles, users, credentials,
-- department and team mappings for multi-role operations.
-- ===================================================

-- 1. Ensure municipal roles exist
INSERT INTO roles (id, name, description, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111101', 'ADMIN', 'System Administrator with full access to city configuration, users, and operations', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111102', 'COMMAND_OPERATOR', 'City Command Centre Operator managing real-time incidents, priority, SLA, and dispatch', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111103', 'DEPARTMENT_OFFICER', 'Department-specific nodal officer managing department actions and acknowledgments', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111104', 'FIELD_WORKER', 'Field technical and response crew member executing on-site tasks and updating resolution', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111105', 'CITIZEN', 'Citizen user of municipal reporting and service tracking services', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 2. Link Traffic Department & Team to officer_traffic and field_worker
UPDATE users
SET department_id = (SELECT id FROM departments WHERE code = 'TRAFFIC' LIMIT 1)
WHERE username = 'officer_traffic' AND (department_id IS NULL OR department_id != (SELECT id FROM departments WHERE code = 'TRAFFIC' LIMIT 1));

UPDATE users
SET department_id = (SELECT id FROM departments WHERE code = 'TRAFFIC' LIMIT 1),
    team_id = (SELECT id FROM teams WHERE code = 'TRAFFIC_OPS' LIMIT 1)
WHERE username = 'field_worker' AND (department_id IS NULL OR team_id IS NULL);

-- 3. Ensure role_id and user_roles mappings for all 5 demo users
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = u.role
WHERE u.username IN ('admin', 'operator', 'officer_traffic', 'field_worker', 'citizen')
ON CONFLICT DO NOTHING;

-- 4. Ensure demo users are active and enabled
UPDATE users
SET enabled = TRUE, active = TRUE
WHERE username IN ('admin', 'operator', 'officer_traffic', 'field_worker', 'citizen');
