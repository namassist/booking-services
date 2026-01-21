package com.example.booking_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("doFilter - Generates new ID when no header present")
    void doFilter_NoHeader_GeneratesNewId() throws Exception {
        // Arrange
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setHeader(eq("X-Correlation-ID"), argThat(id -> 
                id != null && !id.isEmpty() && id.matches("[a-f0-9-]+")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilter - Uses existing ID from header")
    void doFilter_WithHeader_UsesExistingId() throws Exception {
        // Arrange
        String existingId = "existing-correlation-id-123";
        when(request.getHeader("X-Correlation-ID")).thenReturn(existingId);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setHeader("X-Correlation-ID", existingId);
        verify(filterChain).doFilter(request, response);
    }
}
