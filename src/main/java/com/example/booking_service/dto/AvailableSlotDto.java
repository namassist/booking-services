package com.example.booking_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for available time slots.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailableSlotDto {

    private UUID doctorId;
    
    @Schema(description = "Slot start time", example = "09:00:00", type = "string", format = "time")
    private LocalTime startTime;
    
    @Schema(description = "Slot end time", example = "09:30:00", type = "string", format = "time")
    private LocalTime endTime;
    
    private boolean available;
}
