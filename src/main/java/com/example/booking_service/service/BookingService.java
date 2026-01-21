package com.example.booking_service.service;

import com.example.booking_service.dto.AvailableSlotDto;
import com.example.booking_service.dto.BookingResponse;
import com.example.booking_service.dto.CreateBookingRequest;
import com.example.booking_service.entity.*;
import com.example.booking_service.exception.BookingConflictException;
import com.example.booking_service.exception.ResourceNotFoundException;
import com.example.booking_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for booking operations with anti-double-booking logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    /**
     * Create a new booking with double-booking prevention.
     * Uses pessimistic locking to ensure atomicity.
     */
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, UUID userId) {
        // Get patient for this user
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));

        // Get doctor
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));

        if (!doctor.getIsActive()) {
            throw new IllegalArgumentException("Doctor is not available for booking");
        }

        // Validate booking date (must be in the future)
        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Booking date must be in the future");
        }

        // Validate booking date (max 90 days ahead)
        LocalDate maxBookingDate = LocalDate.now().plusDays(90);
        if (request.getBookingDate().isAfter(maxBookingDate)) {
            throw new IllegalArgumentException("Booking date cannot be more than 90 days in the future");
        }

        // Get doctor's schedule for the day
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(request.getBookingDate().getDayOfWeek().name());
        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrue(
                doctor.getId(), dayOfWeek);

        if (schedules.isEmpty()) {
            throw new IllegalArgumentException("Doctor is not available on this day");
        }

        // Find the applicable schedule and STRICTLY validate the time slot alignment
        DoctorSchedule applicableSchedule = null;
        for (DoctorSchedule schedule : schedules) {
            // Check if requested time is EXACTLY on the slot grid (not arbitrary like 10:15)
            if (isValidSlotTime(request.getSlotStartTime(), schedule)) {
                applicableSchedule = schedule;
                break;
            }
        }

        if (applicableSchedule == null) {
            throw new IllegalArgumentException(
                    "Requested time slot is not valid. Slots must start at scheduled intervals (e.g., 09:00, 09:30, 10:00)");
        }

        LocalTime slotEndTime = request.getSlotStartTime().plusMinutes(applicableSchedule.getSlotDurationMinutes());

        // CRITICAL: Check for ANY overlapping booking (not just exact time match)
        // This prevents partial overlaps like booking 10:15 when 10:00 is already booked
        bookingRepository.findOverlappingBookingWithLock(
                doctor.getId(),
                request.getBookingDate(),
                request.getSlotStartTime(),
                slotEndTime
        ).ifPresent(existing -> {
            throw new BookingConflictException(
                    "This time slot conflicts with an existing booking from " + 
                    existing.getSlotStartTime() + " to " + existing.getSlotEndTime());
        });

        // Create the booking
        Booking booking = Booking.builder()
                .doctor(doctor)
                .patient(patient)
                .bookingDate(request.getBookingDate())
                .slotStartTime(request.getSlotStartTime())
                .slotEndTime(slotEndTime)
                .status(BookingStatus.PENDING)
                .notes(request.getNotes())
                .build();

        booking = bookingRepository.save(booking);
        
        log.info("Created booking {} for patient {} with doctor {} on {}",
                booking.getId(), patient.getId(), doctor.getId(), request.getBookingDate());

        return mapToResponse(booking);
    }

    /**
     * Get available time slots for a doctor on a specific date.
     */
    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getAvailableSlots(UUID doctorId, LocalDate date) {
        // Verify doctor exists
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor", "id", doctorId);
        }

        DayOfWeek dayOfWeek = DayOfWeek.valueOf(date.getDayOfWeek().name());
        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrue(
                doctorId, dayOfWeek);

        if (schedules.isEmpty()) {
            return List.of();
        }

        // Get existing bookings for this doctor and date
        List<Booking> existingBookings = bookingRepository.findActiveBookingsByDoctorAndDate(doctorId, date);
        Set<LocalTime> bookedSlots = existingBookings.stream()
                .map(Booking::getSlotStartTime)
                .collect(Collectors.toSet());

        // Generate all possible slots
        List<AvailableSlotDto> slots = new ArrayList<>();
        for (DoctorSchedule schedule : schedules) {
            LocalTime currentTime = schedule.getStartTime();
            while (currentTime.plusMinutes(schedule.getSlotDurationMinutes()).isBefore(schedule.getEndTime().plusSeconds(1))) {
                LocalTime endTime = currentTime.plusMinutes(schedule.getSlotDurationMinutes());
                boolean available = !bookedSlots.contains(currentTime);
                
                // For today, also check if the slot is in the past
                if (date.equals(LocalDate.now()) && currentTime.isBefore(LocalTime.now())) {
                    available = false;
                }

                slots.add(AvailableSlotDto.builder()
                        .doctorId(doctorId)
                        .startTime(currentTime)
                        .endTime(endTime)
                        .available(available)
                        .build());

                currentTime = endTime;
            }
        }

        return slots;
    }

    /**
     * Get bookings for a doctor on a specific date.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByDoctorAndDate(UUID doctorId, LocalDate date) {
        return bookingRepository.findByDoctorIdAndBookingDateOrderBySlotStartTimeAsc(doctorId, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get bookings for a doctor on a specific date (paginated).
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByDoctorAndDate(UUID doctorId, LocalDate date, Pageable pageable) {
        return bookingRepository.findByDoctorIdAndBookingDateOrderBySlotStartTimeAsc(doctorId, date, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get bookings on a specific date (all doctors).
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByDate(LocalDate date) {
        return bookingRepository.findByBookingDateOrderBySlotStartTimeAsc(date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get bookings on a specific date (paginated).
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByDate(LocalDate date, Pageable pageable) {
        return bookingRepository.findByBookingDateOrderBySlotStartTimeAsc(date, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get bookings for a patient.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByPatient(UUID patientId) {
        return bookingRepository.findByPatientIdOrderByBookingDateDescSlotStartTimeDesc(patientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get bookings for a patient (paginated).
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByPatient(UUID patientId, Pageable pageable) {
        return bookingRepository.findByPatientIdOrderByBookingDateDescSlotStartTimeDesc(patientId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get bookings for current user (patient).
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(UUID userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
        
        return getBookingsByPatient(patient.getId());
    }

    /**
     * Get bookings for current user (patient, paginated).
     */
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(UUID userId, Pageable pageable) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
        
        return getBookingsByPatient(patient.getId(), pageable);
    }

    /**
     * Cancel a booking.
     * PATIENT can only cancel their own bookings.
     * STAFF/ADMIN can cancel any booking.
     */
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID userId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Get the user to check their role
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // PATIENT can only cancel their own bookings
        if (user.getRole() == UserRole.PATIENT) {
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
            if (!booking.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("You can only cancel your own bookings");
            }
        }
        // STAFF and ADMIN can cancel any booking (no additional check needed)

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking = bookingRepository.save(booking);

        log.info("Cancelled booking {} with reason: {}", bookingId, reason);

        return mapToResponse(booking);
    }

    /**
     * Confirm a booking (staff/admin only).
     */
    @Transactional
    public BookingResponse confirmBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        log.info("Confirmed booking {}", bookingId);

        return mapToResponse(booking);
    }

    /**
     * Map Booking entity to BookingResponse DTO.
     */
    private BookingResponse mapToResponse(Booking booking) {
        Doctor doctor = booking.getDoctor();
        Patient patient = booking.getPatient();
        Clinic clinic = doctor.getClinic();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingDate(booking.getBookingDate())
                .slotStartTime(booking.getSlotStartTime())
                .slotEndTime(booking.getSlotEndTime())
                .status(booking.getStatus())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                .doctorSpecialization(doctor.getSpecialization())
                .patientId(patient.getId())
                .patientName(patient.getName())
                .patientPhone(patient.getPhone())
                .clinicId(clinic.getId())
                .clinicName(clinic.getName())
                .build();
    }

    /**
     * Validate that the requested slot time is EXACTLY on the schedule's slot grid.
     * This prevents arbitrary times like 10:15 when slots are at 10:00, 10:30, etc.
     * 
     * @param requestedTime The time to validate
     * @param schedule The doctor's schedule
     * @return true if the time is valid (on the grid), false otherwise
     */
    private boolean isValidSlotTime(LocalTime requestedTime, DoctorSchedule schedule) {
        LocalTime current = schedule.getStartTime();
        LocalTime end = schedule.getEndTime();
        int slotDuration = schedule.getSlotDurationMinutes();
        
        // Walk through all valid slot times and check for exact match
        while (current.plusMinutes(slotDuration).isBefore(end.plusSeconds(1))) {
            if (current.equals(requestedTime)) {
                return true; // Found exact match on the grid
            }
            current = current.plusMinutes(slotDuration);
        }
        
        return false; // Requested time is not on the slot grid
    }
}
