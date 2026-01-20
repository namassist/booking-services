package com.example.booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HATEOAS-style links for pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageLinks {
    
    private String self;
    private String next;
    private String prev;
}
