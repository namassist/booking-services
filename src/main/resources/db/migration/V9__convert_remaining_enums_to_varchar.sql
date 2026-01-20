-- V9: Convert remaining enum types to VARCHAR for Hibernate compatibility

-- 1. Doctor Schedules (day_of_week)
-- Drop index that depends on day_of_week
DROP INDEX IF EXISTS idx_schedules_doctor_day;
DROP INDEX IF EXISTS uk_schedules_doctor_day_time;

ALTER TABLE doctor_schedules ALTER COLUMN day_of_week TYPE VARCHAR(20) USING day_of_week::text;
DROP TYPE IF EXISTS day_of_week CASCADE;

-- Re-create indexes
CREATE INDEX idx_schedules_doctor_day ON doctor_schedules(doctor_id, day_of_week);
CREATE UNIQUE INDEX uk_schedules_doctor_day_time ON doctor_schedules(doctor_id, day_of_week, start_time, end_time) WHERE is_active = TRUE;


-- 2. Bookings (booking_status)
-- Drop indexes that depend on status
DROP INDEX IF EXISTS uk_bookings_no_double;
DROP INDEX IF EXISTS idx_bookings_status;
DROP INDEX IF EXISTS idx_bookings_doctor_date_status;

ALTER TABLE bookings ALTER COLUMN status DROP DEFAULT;
ALTER TABLE bookings ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE bookings ALTER COLUMN status SET DEFAULT 'PENDING';
DROP TYPE IF EXISTS booking_status CASCADE;

-- Re-create indexes
-- Note: 'CANCELLED' is now a string literal
CREATE UNIQUE INDEX uk_bookings_no_double ON bookings(doctor_id, booking_date, slot_start_time)
    WHERE status NOT IN ('CANCELLED');

CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_doctor_date_status ON bookings(doctor_id, booking_date, status);
