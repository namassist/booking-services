package com.example.booking_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for rate limiting.
 */
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Data
public class RateLimitProperties {
    
    private int requestsPerMinute = 100;
}
