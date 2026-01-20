package com.example.booking_service.controller;

import com.example.booking_service.dto.*;
import com.example.booking_service.entity.User;
import com.example.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for booking operations with pagination support.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;

    private static final int MAX_PAGE_SIZE = 100;

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
     * Get current user's bookings (patient only) with pagination.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get My Bookings", description = "Retrieve bookings for the currently authenticated patient with pagination.")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "bookingDate") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<BookingResponse> bookings = bookingService.getMyBookings(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(bookings, "/api/bookings/my")));
    }

    /**
     * Get bookings by doctor and date with pagination.
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Get Bookings by Doctor", description = "Retrieve bookings for a specific doctor and date with pagination.")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getBookingsByDoctor(
            @PathVariable UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "slotStartTime") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<BookingResponse> bookings = bookingService.getBookingsByDoctorAndDate(doctorId, date, pageable);
        String basePath = String.format("/api/bookings/doctor/%s?date=%s", doctorId, date);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(bookings, basePath)));
    }

    /**
     * Get bookings by date (all doctors) with pagination.
     */
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Get Bookings by Date", description = "Retrieve all bookings for a specific date across all doctors with pagination.")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getBookingsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "slotStartTime") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<BookingResponse> bookings = bookingService.getBookingsByDate(date, pageable);
        String basePath = String.format("/api/bookings/date/%s", date);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(bookings, basePath)));
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

    /**
     * Create pageable with size limit and sorting.
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), validSize, Sort.by(direction, sortBy));
    }
}
