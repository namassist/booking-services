package com.example.booking_service.controller;

import com.example.booking_service.entity.Clinic;
import com.example.booking_service.repository.ClinicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicControllerTest {

    @Mock
    private ClinicRepository clinicRepository;

    @InjectMocks
    private ClinicController clinicController;

    private Clinic clinic;

    @BeforeEach
    void setUp() {
        clinic = Clinic.builder()
                .id(UUID.randomUUID())
                .name("Test Clinic")
                .address("Test Address")
                .phone("021-12345678")
                .email("test@clinic.com")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("getAllClinics - Returns paginated response")
    void getAllClinics_Paginated() {
        // Arrange
        Page<Clinic> page = new PageImpl<>(List.of(clinic));
        when(clinicRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(page);

        // Act
        ResponseEntity<?> response = clinicController.getAllClinics(0, 10, "name", "asc");

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("searchClinics - Returns matching clinics")
    void searchClinics_ReturnsMatches() {
        // Arrange
        when(clinicRepository.findByNameContainingIgnoreCase("Test")).thenReturn(List.of(clinic));

        // Act
        ResponseEntity<?> response = clinicController.searchClinics("Test");

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }
}
