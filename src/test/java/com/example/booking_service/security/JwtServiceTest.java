package com.example.booking_service.security;

import com.example.booking_service.entity.User;
import com.example.booking_service.entity.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;
    private String secretKey = "testSecretKeyForJwtTokenGenerationMustBeLongEnough256Bits";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "booking-service-test");
        ReflectionTestUtils.setField(jwtService, "audience", "booking-api-test");

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.ADMIN)
                .build();
    }

    @Test
    @DisplayName("generateToken - Contains correct claims")
    void generateToken_ContainsCorrectClaims() {
        // Act
        String token = jwtService.generateAccessToken(user);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("validateToken - Valid token returns true")
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtService.generateAccessToken(user);

        // Act
        boolean isValid = jwtService.isTokenValid(token, user);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateToken - Wrong user returns false")
    void validateToken_WrongUser_ReturnsFalse() {
        // Arrange
        String token = jwtService.generateAccessToken(user);
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .build();

        // Act
        boolean isValid = jwtService.isTokenValid(token, otherUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken - Expired token throws exception")
    void validateToken_ExpiredToken_ThrowsException() {
        // Arrange - create a token that's already expired
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);
        String expiredToken = jwtService.generateAccessToken(user);
        
        // Reset expiration
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("generateRefreshToken - Returns random string")
    void generateRefreshToken_ReturnsRandomString() {
        // Act
        String token1 = jwtService.generateRefreshToken();
        String token2 = jwtService.generateRefreshToken();

        // Assert
        assertThat(token1).isNotNull().isNotEmpty();
        assertThat(token2).isNotNull().isNotEmpty();
        assertThat(token1).isNotEqualTo(token2);
    }
}
