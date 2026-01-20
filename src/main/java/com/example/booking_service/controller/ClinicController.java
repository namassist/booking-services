package com.example.booking_service.controller;

import com.example.booking_service.dto.ApiResponse;
import com.example.booking_service.dto.ClinicResponse;
import com.example.booking_service.dto.PagedResponse;
import com.example.booking_service.entity.Clinic;
import com.example.booking_service.exception.ResourceNotFoundException;
import com.example.booking_service.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for clinic operations with pagination support.
 */
@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Clinic management endpoints")
public class ClinicController {

    private final ClinicRepository clinicRepository;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Get all active clinics with pagination.
     */
    @GetMapping
    @Operation(summary = "Get All Clinics", description = "Retrieve a paginated list of all active clinics.")
    public ResponseEntity<ApiResponse<PagedResponse<ClinicResponse>>> getAllClinics(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<ClinicResponse> clinics = clinicRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(clinics, "/api/clinics")));
    }

    /**
     * Get clinic by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Clinic by ID", description = "Retrieve details of a specific clinic.")
    public ResponseEntity<ApiResponse<ClinicResponse>> getClinic(@PathVariable UUID id) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", "id", id));
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(clinic)));
    }

    /**
     * Search clinics by name.
     */
    @GetMapping("/search")
    @Operation(summary = "Search Clinics", description = "Search clinics by name (case insensitive).")
    public ResponseEntity<ApiResponse<List<ClinicResponse>>> searchClinics(@RequestParam String name) {
        List<ClinicResponse> clinics = clinicRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(clinics));
    }

    private ClinicResponse mapToResponse(Clinic clinic) {
        return ClinicResponse.builder()
                .id(clinic.getId())
                .name(clinic.getName())
                .address(clinic.getAddress())
                .phone(clinic.getPhone())
                .email(clinic.getEmail())
                .isActive(clinic.getIsActive())
                .build();
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), validSize, Sort.by(direction, sortBy));
    }
}
