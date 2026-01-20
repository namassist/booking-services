package com.example.booking_service.repository;

import com.example.booking_service.entity.DayOfWeek;
import com.example.booking_service.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for DoctorSchedule entity.
 */
@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {

    /**
     * Find all schedules for a doctor.
     */
    List<DoctorSchedule> findByDoctorIdAndIsActiveTrue(UUID doctorId);

    /**
     * Find schedule for a doctor on a specific day.
     */
    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndIsActiveTrue(UUID doctorId, DayOfWeek dayOfWeek);

    /**
     * Check if a schedule exists for doctor on a specific day.
     */
    boolean existsByDoctorIdAndDayOfWeekAndIsActiveTrue(UUID doctorId, DayOfWeek dayOfWeek);
}
