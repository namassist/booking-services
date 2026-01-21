package com.example.booking_service.controller;

import com.example.booking_service.dto.AvailableSlotDto;
import com.example.booking_service.entity.Clinic;
import com.example.booking_service.entity.Doctor;
import com.example.booking_service.repository.DoctorRepository;
import com.example.booking_service.service.BookingService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorControllerTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private DoctorController doctorController;

    private Doctor doctor;
    private Clinic clinic;
    private UUID doctorId;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        clinic = Clinic.builder()
                .id(UUID.randomUUID())
                .name("Test Clinic")
                .build();
        doctor = Doctor.builder()
                .id(doctorId)
                .name("Dr. Test")
                .specialization("General")
                .phone("081234567890")
                .isActive(true)
                .clinic(clinic)
                .build();
    }

    @Test
    @DisplayName("getAllDoctors - Returns paginated response")
    void getAllDoctors_Paginated() {
        // Arrange
        Page<Doctor> page = new PageImpl<>(List.of(doctor));
        when(doctorRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(page);

        // Act
        ResponseEntity<?> response = doctorController.getAllDoctors(0, 10, "name", "asc");

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getDoctor - Returns doctor when found")
    void getDoctor_Found() {
        // Arrange
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        // Act
        ResponseEntity<?> response = doctorController.getDoctor(doctorId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getAvailableSlots - Returns slots array")
    void getAvailableSlots_Success() {
        // Arrange
        List<AvailableSlotDto> slots = List.of(
                AvailableSlotDto.builder()
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(9, 30))
                        .available(true)
                        .build()
        );
        when(bookingService.getAvailableSlots(eq(doctorId), any(LocalDate.class))).thenReturn(slots);

        // Act
        ResponseEntity<?> response = doctorController.getAvailableSlots(
                doctorId, LocalDate.now().plusDays(7));

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }
}
