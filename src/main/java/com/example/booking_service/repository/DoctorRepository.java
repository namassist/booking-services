package com.example.booking_service.repository;

import com.example.booking_service.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Doctor entity.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    /**
     * Find all active doctors.
     */
    List<Doctor> findByIsActiveTrue();

    /**
     * Find all active doctors (paginated).
     */
    Page<Doctor> findByIsActiveTrue(Pageable pageable);

    /**
     * Find all doctors for a specific clinic.
     */
    List<Doctor> findByClinicId(UUID clinicId);

    /**
     * Find active doctors for a specific clinic.
     */
    List<Doctor> findByClinicIdAndIsActiveTrue(UUID clinicId);

    /**
     * Find doctors by specialization.
     */
    List<Doctor> findBySpecializationContainingIgnoreCaseAndIsActiveTrue(String specialization);

    /**
     * Find doctors by name.
     */
    List<Doctor> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);

    /**
     * Find doctors with schedules (fetch join to avoid N+1).
     */
    @Query("SELECT DISTINCT d FROM Doctor d LEFT JOIN FETCH d.schedules WHERE d.isActive = true")
    List<Doctor> findAllActiveDoctorsWithSchedules();

    /**
     * Find doctor with schedules by ID.
     */
    @Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.schedules WHERE d.id = :id")
    Doctor findByIdWithSchedules(@Param("id") UUID id);
}
