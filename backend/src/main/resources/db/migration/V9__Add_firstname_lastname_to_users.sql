-- V9__Add_firstname_lastname_to_users.sql
-- Add first_name and last_name columns to users table (if not already added by V8)

-- Only add columns if they don't exist (V8 may have already added them)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'users' AND column_name = 'first_name') THEN
        ALTER TABLE users ADD COLUMN first_name VARCHAR(100) NOT NULL DEFAULT '';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'users' AND column_name = 'last_name') THEN
        ALTER TABLE users ADD COLUMN last_name VARCHAR(100) NOT NULL DEFAULT '';
    END IF;
END $$;

-- Update existing system user with placeholder names
UPDATE users 
SET first_name = 'System', last_name = 'User' 
WHERE username = 'system' OR keycloak_id = 'system-keycloak-id';
