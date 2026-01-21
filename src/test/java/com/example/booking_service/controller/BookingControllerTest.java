package com.example.booking_service.controller;

import com.example.booking_service.dto.BookingResponse;
import com.example.booking_service.dto.CreateBookingRequest;
import com.example.booking_service.entity.BookingStatus;
import com.example.booking_service.entity.User;
import com.example.booking_service.entity.UserRole;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private User patientUser;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        patientUser = User.builder()
                .id(UUID.randomUUID())
                .email("patient@test.com")
                .role(UserRole.PATIENT)
                .build();

        bookingResponse = BookingResponse.builder()
                .id(UUID.randomUUID())
                .doctorName("Dr. Test")
                .patientName("Test Patient")
                .bookingDate(LocalDate.now().plusDays(7))
                .slotStartTime(LocalTime.of(9, 0))
                .slotEndTime(LocalTime.of(9, 30))
                .status(BookingStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("createBooking - Returns booking response")
    void createBooking_Returns200() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setDoctorId(UUID.randomUUID());
        request.setBookingDate(LocalDate.now().plusDays(7));
        request.setSlotStartTime(LocalTime.of(9, 0));

        when(bookingService.createBooking(any(), any())).thenReturn(bookingResponse);

        // Act
        ResponseEntity<?> response = bookingController.createBooking(request, patientUser);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getMyBookings - Returns paginated response")
    void getMyBookings_HasPagination() {
        // Arrange
        Page<BookingResponse> page = new PageImpl<>(List.of(bookingResponse));
        when(bookingService.getMyBookings(any(UUID.class), any(Pageable.class))).thenReturn(page);

        // Act
        ResponseEntity<?> response = bookingController.getMyBookings(
                patientUser, 0, 10, "createdAt", "desc");

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getBookingsByDate - Returns paginated response")
    void getBookingsByDate_Paginated() {
        // Arrange
        Page<BookingResponse> page = new PageImpl<>(List.of(bookingResponse));
        when(bookingService.getBookingsByDate(any(LocalDate.class), any(Pageable.class))).thenReturn(page);

        // Act
        ResponseEntity<?> response = bookingController.getBookingsByDate(
                LocalDate.now(), 0, 10, "createdAt", "desc");

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }
}
