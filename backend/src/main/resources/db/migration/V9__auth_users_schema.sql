-- ===================================================
-- CivicPulse - V9 Auth Users Schema
-- Explicit role, enabled status, and query indexes
-- ===================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'users'
    ) THEN
        CREATE TABLE users (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            full_name VARCHAR(150) NOT NULL,
            email VARCHAR(150) NOT NULL UNIQUE,
            username VARCHAR(100) UNIQUE,
            password_hash VARCHAR(255) NOT NULL,
            role VARCHAR(50) NOT NULL DEFAULT 'CITIZEN',
            enabled BOOLEAN NOT NULL DEFAULT TRUE,
            phone VARCHAR(50),
            department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
            team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
            role_id UUID REFERENCES roles(id) ON DELETE SET NULL,
            active BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
    ELSE
        -- Ensure columns exist
        ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'CITIZEN';
        ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE;

        -- Allow role_id to be nullable for direct role column usage
        ALTER TABLE users ALTER COLUMN role_id DROP NOT NULL;

        -- Populate role from existing role_id mapping if available
        UPDATE users u
        SET role = COALESCE(r.name, 'CITIZEN')
        FROM roles r
        WHERE u.role_id = r.id AND (u.role IS NULL OR u.role = 'CITIZEN');

        -- Synchronize active and enabled flags
        UPDATE users
        SET enabled = COALESCE(active, TRUE)
        WHERE enabled IS NULL;

        UPDATE users
        SET active = enabled
        WHERE active IS NULL;
    END IF;
END $$;

-- Indexes for performant user lookups and authentication
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_username_lower ON users (LOWER(username));
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users (enabled);

