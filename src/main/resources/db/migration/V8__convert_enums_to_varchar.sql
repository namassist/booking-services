-- V8: Convert enum types to VARCHAR for Hibernate compatibility
-- This allows JPA @Enumerated(EnumType.STRING) to work properly

-- First, drop the default value that depends on the enum type
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;

-- Convert user_role enum to VARCHAR
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20) USING role::text;

-- Set a new default value
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'PATIENT';

-- Drop the enum type
DROP TYPE IF EXISTS user_role CASCADE;
