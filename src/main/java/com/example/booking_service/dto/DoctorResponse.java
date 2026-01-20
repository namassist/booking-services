package com.example.booking_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for doctor data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorResponse {

    private UUID id;
    private String name;
    private String specialization;
    private String phone;
    private boolean isActive;
    private UUID clinicId;
    private String clinicName;
}
