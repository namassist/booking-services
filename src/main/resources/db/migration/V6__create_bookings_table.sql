-- V6: Create bookings table with anti-double-booking mechanism
CREATE TYPE booking_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW');

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    booking_date DATE NOT NULL,
    slot_start_time TIME NOT NULL,
    slot_end_time TIME NOT NULL,
    status booking_status NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
    CONSTRAINT chk_bookings_time CHECK (slot_start_time < slot_end_time)
);

-- CRITICAL: Unique partial index for anti-double-booking
-- Prevents multiple active bookings for the same doctor, date, and time slot
CREATE UNIQUE INDEX uk_bookings_no_double ON bookings(doctor_id, booking_date, slot_start_time)
    WHERE status NOT IN ('CANCELLED');

-- Index for doctor's bookings lookup
CREATE INDEX idx_bookings_doctor_date ON bookings(doctor_id, booking_date);

-- Index for patient's bookings lookup  
CREATE INDEX idx_bookings_patient_date ON bookings(patient_id, booking_date);

-- Index for date-based queries
CREATE INDEX idx_bookings_date ON bookings(booking_date);

-- Index for status filtering
CREATE INDEX idx_bookings_status ON bookings(status);

-- Composite index for common query: doctor + date + status
CREATE INDEX idx_bookings_doctor_date_status ON bookings(doctor_id, booking_date, status);
