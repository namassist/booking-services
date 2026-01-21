package com.example.booking_service.service;

import com.example.booking_service.dto.BookingResponse;
import com.example.booking_service.dto.CreateBookingRequest;
import com.example.booking_service.entity.*;
import com.example.booking_service.exception.BookingConflictException;
import com.example.booking_service.exception.ResourceNotFoundException;
import com.example.booking_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorScheduleRepository scheduleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private UUID userId;
    private UUID doctorId;
    private UUID patientId;
    private UUID bookingId;
    private Doctor doctor;
    private Patient patient;
    private User user;
    private Clinic clinic;
    private DoctorSchedule schedule;
    private Booking booking;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        clinic = Clinic.builder()
                .id(UUID.randomUUID())
                .name("Test Clinic")
                .build();

        doctor = Doctor.builder()
                .id(doctorId)
                .name("Dr. Test")
                .clinic(clinic)
                .isActive(true)
                .build();

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .role(UserRole.PATIENT)
                .build();

        patient = Patient.builder()
                .id(patientId)
                .user(user)
                .name("Test Patient")
                .build();

        schedule = DoctorSchedule.builder()
                .id(UUID.randomUUID())
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .isActive(true)
                .build();

        booking = Booking.builder()
                .id(bookingId)
                .doctor(doctor)
                .patient(patient)
                .bookingDate(LocalDate.now().plusDays(7))
                .slotStartTime(LocalTime.of(9, 0))
                .slotEndTime(LocalTime.of(9, 30))
                .status(BookingStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("createBooking - Success when slot is available")
    void createBooking_Success() {
        // Arrange
        LocalDate nextMonday = getNextMonday();
        CreateBookingRequest request = new CreateBookingRequest();
        request.setDoctorId(doctorId);
        request.setBookingDate(nextMonday);
        request.setSlotStartTime(LocalTime.of(9, 0));

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrue(eq(doctorId), any()))
                .thenReturn(List.of(schedule));
        when(bookingRepository.findExistingBookingWithLock(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        // Act
        BookingResponse response = bookingService.createBooking(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - Throws conflict when slot already booked")
    void createBooking_SlotAlreadyBooked_ThrowsConflict() {
        // Arrange
        LocalDate nextMonday = getNextMonday();
        CreateBookingRequest request = new CreateBookingRequest();
        request.setDoctorId(doctorId);
        request.setBookingDate(nextMonday);
        request.setSlotStartTime(LocalTime.of(9, 0));

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrue(eq(doctorId), any()))
                .thenReturn(List.of(schedule));
        when(bookingRepository.findExistingBookingWithLock(any(), any(), any()))
                .thenReturn(Optional.of(booking)); // Slot already booked

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, userId))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("createBooking - Throws exception when booking date exceeds 90 days")
    void createBooking_MaxDateExceeded_ThrowsBadRequest() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setDoctorId(doctorId);
        request.setBookingDate(LocalDate.now().plusDays(100)); // Over 90 days
        request.setSlotStartTime(LocalTime.of(9, 0));

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("90 days");
    }

    @Test
    @DisplayName("createBooking - Throws not found when doctor doesn't exist")
    void createBooking_DoctorNotFound_ThrowsNotFound() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setDoctorId(doctorId);
        request.setBookingDate(LocalDate.now().plusDays(7));
        request.setSlotStartTime(LocalTime.of(9, 0));

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createBooking - Throws exception when no schedule for day")
    void createBooking_NoSchedule_ThrowsBadRequest() {
        // Arrange
        LocalDate nextMonday = getNextMonday();
        CreateBookingRequest request = new CreateBookingRequest();
        request.setDoctorId(doctorId);
        request.setBookingDate(nextMonday);
        request.setSlotStartTime(LocalTime.of(9, 0));

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrue(eq(doctorId), any()))
                .thenReturn(List.of()); // No schedule

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("cancelBooking - Patient can cancel own booking")
    void cancelBooking_AsPatient_OwnBooking_Success() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // Act
        BookingResponse response = bookingService.cancelBooking(bookingId, userId, "Test reason");

        // Assert
        assertThat(response).isNotNull();
        verify(bookingRepository).save(argThat(b -> b.getStatus() == BookingStatus.CANCELLED));
    }

    @Test
    @DisplayName("cancelBooking - Patient cannot cancel other's booking")
    void cancelBooking_AsPatient_OtherBooking_ThrowsForbidden() {
        // Arrange
        Patient otherPatient = Patient.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();
        Booking otherBooking = Booking.builder()
                .id(bookingId)
                .doctor(doctor)
                .patient(otherPatient)
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(otherBooking));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

        // Act & Assert
        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, userId, "Test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only cancel your own");
    }

    @Test
    @DisplayName("cancelBooking - Staff can cancel any booking")
    void cancelBooking_AsStaff_AnyBooking_Success() {
        // Arrange
        User staffUser = User.builder()
                .id(UUID.randomUUID())
                .email("staff@example.com")
                .role(UserRole.STAFF)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(staffUser.getId())).thenReturn(Optional.of(staffUser));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // Act
        BookingResponse response = bookingService.cancelBooking(bookingId, staffUser.getId(), "Admin cancel");

        // Assert
        assertThat(response).isNotNull();
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("confirmBooking - Success for pending booking")
    void confirmBooking_Success() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BookingResponse response = bookingService.confirmBooking(bookingId);

        // Assert
        assertThat(response).isNotNull();
        verify(bookingRepository).save(argThat(b -> b.getStatus() == BookingStatus.CONFIRMED));
    }

    @Test
    @DisplayName("getMyBookings - Returns paginated results")
    void getMyBookings_Paginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> bookingPage = new PageImpl<>(List.of(booking), pageable, 1);

        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(bookingRepository.findByPatientIdOrderByBookingDateDescSlotStartTimeDesc(patientId, pageable))
                .thenReturn(bookingPage);

        // Act
        Page<BookingResponse> result = bookingService.getMyBookings(userId, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAvailableSlots - Returns filtered slots excluding booked")
    void getAvailableSlots_ReturnsFilteredSlots() {
        // Arrange
        LocalDate nextMonday = getNextMonday();
        when(doctorRepository.existsById(doctorId)).thenReturn(true);
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrue(eq(doctorId), any()))
                .thenReturn(List.of(schedule));
        when(bookingRepository.findActiveBookingsByDoctorAndDate(doctorId, nextMonday))
                .thenReturn(List.of(booking)); // 9:00 slot is booked

        // Act
        var slots = bookingService.getAvailableSlots(doctorId, nextMonday);

        // Assert
        assertThat(slots).isNotEmpty();
        // 9:00 slot should be marked as not available
        var slot9am = slots.stream()
                .filter(s -> s.getStartTime().equals(LocalTime.of(9, 0)))
                .findFirst();
        assertThat(slot9am).isPresent();
        assertThat(slot9am.get().isAvailable()).isFalse();
    }

    private LocalDate getNextMonday() {
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (java.time.DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        if (daysUntilMonday == 0) daysUntilMonday = 7;
        return today.plusDays(daysUntilMonday);
    }
}
