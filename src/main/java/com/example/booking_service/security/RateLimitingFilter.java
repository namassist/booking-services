package com.example.booking_service.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiting filter using Bucket4j with Caffeine cache.
 * Limits requests per IP address with separate limits for auth endpoints.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    // Use Caffeine cache with automatic eviction to prevent memory leak
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .build();

    // Separate cache for auth endpoints with stricter limits
    private final Cache<String, Bucket> authBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        
        // Use stricter rate limit for auth endpoints (login/register)
        boolean isAuthEndpoint = requestUri.startsWith("/api/auth/login") 
                || requestUri.startsWith("/api/auth/register");
        
        Bucket bucket;
        if (isAuthEndpoint) {
            bucket = authBuckets.get(clientIp, this::createAuthBucket);
        } else {
            bucket = buckets.get(clientIp, this::createBucket);
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestUri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            
            String message = isAuthEndpoint 
                    ? "Too many login attempts. Please try again later."
                    : "Too many requests. Please try again later.";
            response.getWriter().write(
                    "{\"success\":false,\"error\":\"" + message + "\",\"code\":\"RATE_LIMIT_EXCEEDED\"}"
            );
        }
    }

    /**
     * Create a bucket for general rate limiting.
     */
    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Create a stricter bucket for auth endpoints (brute-force protection).
     */
    private Bucket createAuthBucket(String key) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(authRequestsPerMinute)
                .refillIntervally(authRequestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Extract client IP address from request.
     * Handles X-Forwarded-For header for proxied requests.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
