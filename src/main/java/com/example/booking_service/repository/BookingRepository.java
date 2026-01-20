package com.example.booking_service.repository;

import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Booking entity with pessimistic locking for anti-double-booking.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Find existing booking with pessimistic lock to prevent double booking.
     * This is the CRITICAL method for concurrency control.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.doctor.id = :doctorId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.slotStartTime = :slotStartTime " +
           "AND b.status NOT IN ('CANCELLED')")
    Optional<Booking> findExistingBookingWithLock(
            @Param("doctorId") UUID doctorId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("slotStartTime") LocalTime slotStartTime
    );

    /**
     * Check if a slot is already booked (without lock, for read-only checks).
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.doctor.id = :doctorId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.slotStartTime = :slotStartTime " +
           "AND b.status NOT IN ('CANCELLED')")
    boolean isSlotBooked(
            @Param("doctorId") UUID doctorId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("slotStartTime") LocalTime slotStartTime
    );

    /**
     * Find all bookings for a doctor on a specific date.
     */
    List<Booking> findByDoctorIdAndBookingDateOrderBySlotStartTimeAsc(UUID doctorId, LocalDate bookingDate);

    /**
     * Find all bookings for a doctor on a specific date with specific statuses.
     */
    @Query("SELECT b FROM Booking b WHERE b.doctor.id = :doctorId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.status IN :statuses " +
           "ORDER BY b.slotStartTime ASC")
    List<Booking> findByDoctorIdAndDateAndStatuses(
            @Param("doctorId") UUID doctorId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("statuses") List<BookingStatus> statuses
    );

    /**
     * Find all bookings for a patient (non-paginated).
     */
    List<Booking> findByPatientIdOrderByBookingDateDescSlotStartTimeDesc(UUID patientId);

    /**
     * Find all bookings for a patient (paginated).
     */
    Page<Booking> findByPatientIdOrderByBookingDateDescSlotStartTimeDesc(UUID patientId, Pageable pageable);

    /**
     * Find all bookings on a specific date (non-paginated).
     */
    List<Booking> findByBookingDateOrderBySlotStartTimeAsc(LocalDate bookingDate);

    /**
     * Find all bookings on a specific date (paginated).
     */
    Page<Booking> findByBookingDateOrderBySlotStartTimeAsc(LocalDate bookingDate, Pageable pageable);

    /**
     * Find bookings for a doctor on a date (paginated).
     */
    Page<Booking> findByDoctorIdAndBookingDateOrderBySlotStartTimeAsc(UUID doctorId, LocalDate bookingDate, Pageable pageable);

    /**
     * Find bookings for a patient within a date range.
     */
    @Query("SELECT b FROM Booking b WHERE b.patient.id = :patientId " +
           "AND b.bookingDate BETWEEN :startDate AND :endDate " +
           "ORDER BY b.bookingDate ASC, b.slotStartTime ASC")
    List<Booking> findByPatientIdAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find all existing bookings for a doctor on a date (for slot availability calculation).
     */
    @Query("SELECT b FROM Booking b WHERE b.doctor.id = :doctorId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.status NOT IN ('CANCELLED') " +
           "ORDER BY b.slotStartTime ASC")
    List<Booking> findActiveBookingsByDoctorAndDate(
            @Param("doctorId") UUID doctorId,
            @Param("bookingDate") LocalDate bookingDate
    );
}
