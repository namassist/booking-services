package com.example.booking_service.controller;

import com.example.booking_service.dto.*;
import com.example.booking_service.entity.User;
import com.example.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller for booking operations.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;

    /**
     * Create a new booking.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'STAFF', 'ADMIN')")
    @Operation(summary = "Create Booking", description = "Create a new appointment booking.")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal User user
    ) {
        BookingResponse response = bookingService.createBooking(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking created successfully", response));
    }

    /**
     * Get current user's bookings (patient only).
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get My Bookings", description = "Retrieve bookings for the currently authenticated patient.")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal User user
    ) {
        List<BookingResponse> bookings = bookingService.getMyBookings(user.getId());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get bookings by doctor and date.
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Get Bookings by Doctor", description = "Retrieve bookings for a specific doctor and date.")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByDoctor(
            @PathVariable UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<BookingResponse> bookings = bookingService.getBookingsByDoctorAndDate(doctorId, date);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Get bookings by date (all doctors).
     */
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Get Bookings by Date", description = "Retrieve all bookings for a specific date across all doctors.")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<BookingResponse> bookings = bookingService.getBookingsByDate(date);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Cancel a booking.
     */
    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'STAFF', 'ADMIN')")
    @Operation(summary = "Cancel Booking", description = "Cancel an existing booking.")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User user
    ) {
        BookingResponse response = bookingService.cancelBooking(bookingId, user.getId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", response));
    }

    /**
     * Confirm a booking (staff/admin only).
     */
    @PutMapping("/{bookingId}/confirm")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Confirm Booking", description = "Confirm a pending booking.")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable UUID bookingId) {
        BookingResponse response = bookingService.confirmBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed", response));
    }
}
