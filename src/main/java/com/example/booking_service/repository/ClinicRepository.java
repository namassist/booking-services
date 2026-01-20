package com.example.booking_service.repository;

import com.example.booking_service.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Clinic entity.
 */
@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    /**
     * Find all active clinics.
     */
    List<Clinic> findByIsActiveTrue();

    /**
     * Find clinics by name containing (case-insensitive search).
     */
    List<Clinic> findByNameContainingIgnoreCase(String name);
}
