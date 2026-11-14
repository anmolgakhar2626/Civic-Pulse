-- ===================================================
-- CivicPulse - V6 Security & Roles Schema
-- Seed Default Roles and Create User-Roles Mapping
-- ===================================================

-- 1. Seed 5 Default Municipal Roles
INSERT INTO roles (id, name, description, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111101', 'ADMIN', 'System Administrator with full access to city configuration, users, and operations', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111102', 'COMMAND_OPERATOR', 'City Command Centre Operator managing real-time incidents, priority, SLA, and dispatch', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111103', 'DEPARTMENT_OFFICER', 'Department-specific nodal officer managing department actions and acknowledgments', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111104', 'FIELD_WORKER', 'Field technical and response crew member executing on-site tasks and updating resolution', CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111105', 'CITIZEN', 'Citizen user of municipal reporting and service tracking services', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 2. Create user_roles mapping table for explicit role association
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
