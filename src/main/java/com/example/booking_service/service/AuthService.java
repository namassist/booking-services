package com.example.booking_service.service;

import com.example.booking_service.dto.AuthResponse;
import com.example.booking_service.dto.LoginRequest;
import com.example.booking_service.dto.RefreshTokenRequest;
import com.example.booking_service.dto.RegisterRequest;
import com.example.booking_service.entity.LoginAttempt;
import com.example.booking_service.entity.Patient;
import com.example.booking_service.entity.RefreshToken;
import com.example.booking_service.entity.User;
import com.example.booking_service.entity.UserRole;
import com.example.booking_service.exception.InvalidTokenException;
import com.example.booking_service.repository.LoginAttemptRepository;
import com.example.booking_service.repository.PatientRepository;
import com.example.booking_service.repository.RefreshTokenRepository;
import com.example.booking_service.repository.UserRepository;
import com.example.booking_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;

/**
 * Service for authentication operations with account lockout protection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    /**
     * Register a new patient user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(UserRole.PATIENT)
                .isActive(true)
                .build();
        
        user = userRepository.save(user);

        // Create corresponding patient record
        Patient patient = Patient.builder()
                .user(user)
                .name(request.getName())
                .phone(request.getPhone())
                .build();
        
        patientRepository.save(patient);

        // Log with user ID instead of email (PII protection)
        log.info("Registered new user with id: {}", user.getId());
        
        return generateAuthResponse(user);
    }

    /**
     * Authenticate a user and return tokens.
     * Implements account lockout after too many failed attempts.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user first to check lockout status
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        
        if (user != null) {
            // Check if account is locked
            if (!user.isAccountNonLocked()) {
                log.warn("Login attempt for locked account: {}", user.getId());
                throw new LockedException("Account is locked. Please try again later.");
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Successful login - record attempt and clear any lockout
            if (user != null) {
                recordLoginAttempt(user, true, null);
                if (user.getLockedUntil() != null) {
                    user.setLockedUntil(null);
                    userRepository.save(user);
                }
            }

            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            log.info("User logged in with id: {}", user.getId());
            
            return generateAuthResponse(user);

        } catch (BadCredentialsException e) {
            // Failed login - record attempt and potentially lock account
            if (user != null) {
                recordLoginAttempt(user, false, null);
                checkAndLockAccount(user);
            }
            throw e;
        }
    }

    /**
     * Record a login attempt.
     */
    private void recordLoginAttempt(User user, boolean success, String ipAddress) {
        LoginAttempt attempt = LoginAttempt.builder()
                .user(user)
                .success(success)
                .ipAddress(ipAddress)
                .build();
        loginAttemptRepository.save(attempt);
    }

    /**
     * Check failed attempts and lock account if threshold exceeded.
     */
    private void checkAndLockAccount(User user) {
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(lockoutDurationMinutes);
        long failedAttempts = loginAttemptRepository.countFailedAttemptsSince(user.getId(), since);
        
        if (failedAttempts >= maxFailedAttempts) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(lockoutDurationMinutes));
            userRepository.save(user);
            log.warn("Account locked due to {} failed attempts: user id {}", failedAttempts, user.getId());
        }
    }

    /**
     * Refresh access token using refresh token.
     * Implements token rotation - old refresh token is invalidated.
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        
        RefreshToken refreshToken = refreshTokenRepository.findValidToken(tokenHash, OffsetDateTime.now())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));

        // Revoke the old token (token rotation)
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        
        // Log with user ID instead of email (PII protection)
        log.info("Token refreshed for user id: {}", user.getId());
        
        return generateAuthResponse(user);
    }

    /**
     * Logout by revoking the refresh token.
     */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                    // Log with user ID instead of email (PII protection)
                    log.info("User logged out with id: {}", token.getUser().getId());
                });
    }

    /**
     * Logout from all devices by revoking all refresh tokens.
     */
    @Transactional
    public void logoutAll(User user) {
        refreshTokenRepository.revokeAllUserTokens(user.getId(), OffsetDateTime.now());
        // Log with user ID instead of email (PII protection)
        log.info("All tokens revoked for user id: {}", user.getId());
    }

    /**
     * Generate authentication response with access and refresh tokens.
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        // Save refresh token hash
        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getRefreshTokenExpirationMs() / 1000)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    /**
     * Save refresh token hash to database.
     */
    private void saveRefreshToken(User user, String token) {
        String tokenHash = hashToken(token);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationMs() / 1000))
                .revoked(false)
                .build();
        
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Hash a token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
