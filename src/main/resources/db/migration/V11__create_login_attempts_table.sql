-- V11: Create login_attempts table for account lockout functionality

CREATE TABLE login_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    attempt_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    CONSTRAINT fk_login_attempts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for quick lookup of recent failed attempts
CREATE INDEX idx_login_attempts_user_time ON login_attempts(user_id, attempt_time DESC);

-- Add locked_until column to users table for account lockout
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;
