-- V3: Create doctors table
CREATE TABLE doctors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    specialization VARCHAR(255),
    license_number VARCHAR(100),
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doctors_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE CASCADE
);

-- Index for clinic-based doctor listing
CREATE INDEX idx_doctors_clinic_id ON doctors(clinic_id);

-- Index for active doctors
CREATE INDEX idx_doctors_is_active ON doctors(is_active);

-- Composite index for clinic + active status
CREATE INDEX idx_doctors_clinic_active ON doctors(clinic_id, is_active);
