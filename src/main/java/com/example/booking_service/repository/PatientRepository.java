package com.example.booking_service.repository;

import com.example.booking_service.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Patient entity.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Find a patient by their user ID.
     */
    Optional<Patient> findByUserId(UUID userId);

    /**
     * Find a patient by phone number.
     */
    Optional<Patient> findByPhone(String phone);

    /**
     * Check if a patient exists for a user.
     */
    boolean existsByUserId(UUID userId);
}
