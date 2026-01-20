package com.example.booking_service.controller;

import com.example.booking_service.dto.ApiResponse;
import com.example.booking_service.dto.AvailableSlotDto;
import com.example.booking_service.dto.DoctorResponse;
import com.example.booking_service.dto.PagedResponse;
import com.example.booking_service.entity.Doctor;
import com.example.booking_service.exception.ResourceNotFoundException;
import com.example.booking_service.repository.DoctorRepository;
import com.example.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for doctor operations with pagination support.
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Doctor management endpoints")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final BookingService bookingService;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Get all active doctors with pagination.
     */
    @GetMapping
    @Operation(summary = "Get All Doctors", description = "Retrieve a paginated list of all active doctors.")
    public ResponseEntity<ApiResponse<PagedResponse<DoctorResponse>>> getAllDoctors(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<DoctorResponse> doctors = doctorRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(doctors, "/api/doctors")));
    }

    /**
     * Get doctor by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Doctor by ID", description = "Retrieve details of a specific doctor.")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctor(@PathVariable UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(doctor)));
    }

    /**
     * Get doctors by clinic.
     */
    @GetMapping("/clinic/{clinicId}")
    @Operation(summary = "Get Doctors by Clinic", description = "Retrieve all doctors belonging to a specific clinic.")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getDoctorsByClinic(@PathVariable UUID clinicId) {
        List<DoctorResponse> doctors = doctorRepository.findByClinicIdAndIsActiveTrue(clinicId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    /**
     * Get available time slots for a doctor on a specific date.
     */
    @GetMapping("/{id}/available-slots")
    @Operation(summary = "Get Available Slots", description = "Retrieve available appointments slots for a doctor on a specific date.")
    public ResponseEntity<ApiResponse<List<AvailableSlotDto>>> getAvailableSlots(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<AvailableSlotDto> slots = bookingService.getAvailableSlots(id, date);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    /**
     * Search doctors by specialization or name.
     */
    @GetMapping("/search")
    @Operation(summary = "Search Doctors", description = "Search doctors by name or specialization.")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> searchDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String name
    ) {
        List<Doctor> doctors;
        if (specialization != null && !specialization.isBlank()) {
            doctors = doctorRepository.findBySpecializationContainingIgnoreCaseAndIsActiveTrue(specialization);
        } else if (name != null && !name.isBlank()) {
            doctors = doctorRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name);
        } else {
            doctors = doctorRepository.findByIsActiveTrue();
        }
        
        List<DoctorResponse> response = doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .phone(doctor.getPhone())
                .isActive(doctor.getIsActive())
                .clinicId(doctor.getClinic().getId())
                .clinicName(doctor.getClinic().getName())
                .build();
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), validSize, Sort.by(direction, sortBy));
    }
}
