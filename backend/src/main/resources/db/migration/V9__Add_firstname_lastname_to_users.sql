-- V9__Add_firstname_lastname_to_users.sql
-- Add first_name and last_name columns to users table

ALTER TABLE users 
ADD COLUMN first_name VARCHAR(100) NOT NULL DEFAULT '';

ALTER TABLE users 
ADD COLUMN last_name VARCHAR(100) NOT NULL DEFAULT '';

-- Update existing system user with placeholder names
UPDATE users 
SET first_name = 'System', last_name = 'User' 
WHERE username = 'system' OR keycloak_id = 'system-keycloak-id';
