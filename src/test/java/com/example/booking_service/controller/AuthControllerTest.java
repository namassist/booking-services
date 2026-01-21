package com.example.booking_service.controller;

import com.example.booking_service.dto.AuthResponse;
import com.example.booking_service.dto.LoginRequest;
import com.example.booking_service.dto.RegisterRequest;
import com.example.booking_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        authResponse = AuthResponse.builder()
                .accessToken("testAccessToken")
                .refreshToken("testRefreshToken")
                .tokenType("Bearer")
                .build();
    }

    @Test
    @DisplayName("login - Success returns tokens")
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(authService.login(any())).thenReturn(authResponse);

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("login - Bad credentials throws exception")
    void login_BadCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("register - Success returns tokens")
    void register_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setName("New User");
        request.setPhone("081234567890");

        when(authService.register(any())).thenReturn(authResponse);

        // Act
        ResponseEntity<?> response = authController.register(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }
}
