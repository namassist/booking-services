-- V10: Add index on patients.user_id for faster lookup
-- This index improves performance for findByUserId queries used in booking operations

CREATE INDEX idx_patients_user_id ON patients(user_id);
