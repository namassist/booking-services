package com.example.booking_service.config;

import com.example.booking_service.entity.*;
import com.example.booking_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedClinicsAndDoctors();
        seedUsersAndPatients();
        log.info("Data seeding completed.");
    }

    private void seedClinicsAndDoctors() {
        if (clinicRepository.count() > 0) {
            log.info("Clinics already exist. Skipping seeding.");
            return;
        }

        log.info("Seeding clinics and doctors...");

        // Create Clinic
        Clinic clinic = Clinic.builder()
                .name("Sehat Sentosa Clinic")
                .address("Jl. Jendral Sudirman No. 123, Jakarta")
                .phone("021-12345678")
                .email("info@sehatsentosa.com")
                .isActive(true)
                .build();
        clinicRepository.save(clinic);

        // Create Doctors
        Doctor docStrange = Doctor.builder()
                .clinic(clinic)
                .name("Dr. Strange")
                .specialization("General Practitioner")
                .licenseNumber("STR-12345")
                .phone("081234567890")
                .isActive(true)
                .build();

        Doctor docDoom = Doctor.builder()
                .clinic(clinic)
                .name("Dr. Doom")
                .specialization("Dermatologist")
                .licenseNumber("STR-67890")
                .phone("081298765432")
                .isActive(true)
                .build();

        doctorRepository.saveAll(Arrays.asList(docStrange, docDoom));

        // Create Schedules for Dr. Strange (Mon & Wed)
        DoctorSchedule strangeMon = DoctorSchedule.builder()
                .doctor(docStrange)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .isActive(true)
                .build();

        DoctorSchedule strangeWed = DoctorSchedule.builder()
                .doctor(docStrange)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .isActive(true)
                .build();

        // Create Schedules for Dr. Doom (Tue & Thu)
        DoctorSchedule doomTue = DoctorSchedule.builder()
                .doctor(docDoom)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(16, 0))
                .slotDurationMinutes(30)
                .isActive(true)
                .build();

        DoctorSchedule doomThu = DoctorSchedule.builder()
                .doctor(docDoom)
                .dayOfWeek(DayOfWeek.THURSDAY)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(16, 0))
                .slotDurationMinutes(30)
                .isActive(true)
                .build();

        doctorScheduleRepository.saveAll(Arrays.asList(strangeMon, strangeWed, doomTue, doomThu));
    }

    private void seedUsersAndPatients() {
        if (userRepository.count() > 0) {
            log.info("Users already exist. Skipping seeding.");
            return;
        }

        log.info("Seeding users and patients...");

        // Create Admin
        User admin = User.builder()
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("secretpisan"))
                .name("Admin User")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
        userRepository.save(admin);

        // Create Patient User
        User patientUser = User.builder()
                .email("patient@example.com")
                .passwordHash(passwordEncoder.encode("secretpisan"))
                .name("John Doe")
                .role(UserRole.PATIENT)
                .isActive(true)
                .build();
        userRepository.save(patientUser);

        // Create Patient Profile
        Patient patientToSave = Patient.builder()
                .user(patientUser)
                .name("John Doe")
                .phone("085678901234")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .address("Jl. Kebon Jeruk No. 10")
                .emergencyContact("Jane Doe (Wife) - 085677778888")
                .build();
        patientRepository.save(patientToSave);
    }
}
