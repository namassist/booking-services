package com.example.booking_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for creating a booking.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Doctor ID is required")
    @Schema(description = "Doctor UUID", example = "64446175-d541-4cc3-852f-2f3b4b4e7c2a")
    private UUID doctorId;

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date cannot be in the past")
    @Schema(description = "Booking date (YYYY-MM-DD)", example = "2026-01-23", type = "string", format = "date")
    private LocalDate bookingDate;

    @NotNull(message = "Slot start time is required")
    @Schema(description = "Slot start time (HH:mm)", example = "09:00", type = "string", format = "time")
    private LocalTime slotStartTime;

    @Schema(description = "Optional notes for the booking", example = "First visit")
    private String notes;
}
