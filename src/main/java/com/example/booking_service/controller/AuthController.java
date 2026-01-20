package com.example.booking_service.controller;

import com.example.booking_service.dto.*;
import com.example.booking_service.entity.User;
import com.example.booking_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new patient account.
     */
    @PostMapping("/register")
    @Operation(summary = "Register new patient", description = "Register a new patient account with email, password, and personal details.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    /**
     * Login with email and password.
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate using email and password to receive access and refresh tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refresh access token using refresh token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Get a new access token using a valid refresh token.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    /**
     * Logout by revoking refresh token.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate the provided refresh token.")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * Logout from all devices.
     */
    @PostMapping("/logout-all")
    @Operation(summary = "Logout All Devices", description = "Invalidate all refresh tokens for the current user.")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal User user) {
        authService.logoutAll(user);
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices", null));
    }
}
