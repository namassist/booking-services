-- V5: Create doctor_schedules table
CREATE TYPE day_of_week AS ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY');

CREATE TABLE doctor_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL,
    day_of_week day_of_week NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 30,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT chk_schedules_time CHECK (start_time < end_time),
    CONSTRAINT chk_schedules_duration CHECK (slot_duration_minutes > 0 AND slot_duration_minutes <= 240)
);

-- Composite index for schedule lookup by doctor and day
CREATE INDEX idx_schedules_doctor_day ON doctor_schedules(doctor_id, day_of_week);

-- Prevent duplicate schedules for the same doctor, day, and time range
CREATE UNIQUE INDEX uk_schedules_doctor_day_time ON doctor_schedules(doctor_id, day_of_week, start_time, end_time) WHERE is_active = TRUE;
