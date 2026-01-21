package com.example.booking_service.dto;

import com.example.booking_service.entity.BookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for booking data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {

    private UUID id;
    
    @Schema(description = "Booking date", example = "2026-01-23", type = "string", format = "date")
    private LocalDate bookingDate;
    
    @Schema(description = "Slot start time", example = "09:00:00", type = "string", format = "time")
    private LocalTime slotStartTime;
    
    @Schema(description = "Slot end time", example = "09:30:00", type = "string", format = "time")
    private LocalTime slotEndTime;
    
    private BookingStatus status;
    private String notes;
    private OffsetDateTime createdAt;

    // Doctor info
    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialization;

    // Patient info (only for staff/admin)
    private UUID patientId;
    private String patientName;
    private String patientPhone;

    // Clinic info
    private UUID clinicId;
    private String clinicName;
}
