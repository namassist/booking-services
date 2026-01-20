package com.example.booking_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for JWT settings.
 * This makes the custom properties known to Spring Boot.
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    
    private String secret = "your-256-bit-secret-key-here-for-development-only-change-in-production";
    private long accessTokenExpirationMs = 900000;
    private long refreshTokenExpirationMs = 604800000;
}
