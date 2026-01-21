package com.example.booking_service.service;

import com.example.booking_service.dto.AuthResponse;
import com.example.booking_service.dto.LoginRequest;
import com.example.booking_service.dto.RegisterRequest;
import com.example.booking_service.entity.*;
import com.example.booking_service.repository.LoginAttemptRepository;
import com.example.booking_service.repository.PatientRepository;
import com.example.booking_service.repository.RefreshTokenRepository;
import com.example.booking_service.repository.UserRepository;
import com.example.booking_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private LoginAttemptRepository loginAttemptRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .role(UserRole.PATIENT)
                .isActive(true)
                .build();

        // Set properties via reflection
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutDurationMinutes", 15);
    }

    @Test
    @DisplayName("login - Success returns tokens")
    void login_Success_ReturnsTokens() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken()).thenReturn("refreshToken");
        when(loginAttemptRepository.countFailedAttemptsSince(any(), any())).thenReturn(0L);

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login - Wrong password throws unauthorized")
    void login_WrongPassword_ThrowsUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(loginAttemptRepository.countFailedAttemptsSince(any(), any())).thenReturn(0L);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    @DisplayName("login - Locked account throws LockedException")
    void login_AccountLocked_ThrowsLocked() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@example.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new LockedException("Account is locked"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("login - Successful login clears lockout")
    void login_Success_ClearsLockout() {
        // Arrange
        user.setLockedUntil(OffsetDateTime.now().minusMinutes(1)); // Expired lockout

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken()).thenReturn("refreshToken");
        when(loginAttemptRepository.countFailedAttemptsSince(any(), any())).thenReturn(0L);

        // Act
        authService.login(request);

        // Assert
        verify(userRepository).save(argThat(u -> u.getLockedUntil() == null));
    }

    @Test
    @DisplayName("register - Success creates user and patient")
    void register_Success_CreatesUserAndPatient() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setName("New User");
        request.setPhone("081234567890");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken()).thenReturn("refreshToken");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("register - Email exists throws conflict")
    void register_EmailExists_ThrowsConflict() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password");
        request.setName("User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("refresh - Success returns new tokens")
    void refresh_Success_ReturnsNewTokens() {
        // Arrange
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hashedToken")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        com.example.booking_service.dto.RefreshTokenRequest request = 
                new com.example.booking_service.dto.RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");

        when(refreshTokenRepository.findValidToken(any(), any()))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(user)).thenReturn("newAccessToken");
        when(jwtService.generateRefreshToken()).thenReturn("newRefreshToken");

        // Act
        AuthResponse response = authService.refresh(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
    }
}
