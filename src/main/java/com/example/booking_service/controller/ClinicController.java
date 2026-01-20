package com.example.booking_service.controller;

import com.example.booking_service.dto.ApiResponse;
import com.example.booking_service.dto.ClinicResponse;
import com.example.booking_service.entity.Clinic;
import com.example.booking_service.exception.ResourceNotFoundException;
import com.example.booking_service.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for clinic operations.
 */
@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Clinic management endpoints")
public class ClinicController {

    private final ClinicRepository clinicRepository;

    /**
     * Get all active clinics.
     */
    @GetMapping
    @Operation(summary = "Get All Clinics", description = "Retrieve a list of all active clinics.")
    public ResponseEntity<ApiResponse<List<ClinicResponse>>> getAllClinics() {
        List<ClinicResponse> clinics = clinicRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(clinics));
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
}
