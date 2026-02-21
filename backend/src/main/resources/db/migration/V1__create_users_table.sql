-- V1__create_users_table.sql
-- Create users table for Lazycord authentication

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    avatar_url VARCHAR(500),
    points INTEGER NOT NULL DEFAULT 0,
    xp INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    rank VARCHAR(50) NOT NULL DEFAULT 'Newbie',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_users_keycloak_id ON users(keycloak_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_points ON users(points DESC);
CREATE INDEX IF NOT EXISTS idx_users_level ON users(level DESC);

-- Add comments for documentation
COMMENT ON TABLE users IS 'Application users synced with Keycloak';
COMMENT ON COLUMN users.keycloak_id IS 'Keycloak user ID for external identity provider sync';
COMMENT ON COLUMN users.points IS 'Gamification points earned by the user';
COMMENT ON COLUMN users.xp IS 'Experience points for level progression';
COMMENT ON COLUMN users.rank IS 'User rank based on level (Newbie, Regular, Veteran, Expert, Master, Grandmaster, Legend)';
