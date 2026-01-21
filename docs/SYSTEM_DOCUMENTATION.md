# Clinic Booking Service - System Documentation

## Overview

Sistem booking klinik online multi-cabang yang memungkinkan pasien membuat appointment dengan dokter. Dilengkapi dengan autentikasi JWT, otorisasi berbasis role, dan mekanisme pencegahan double-booking 3 lapis.

---

## User Roles & Access Control

### Role Hierarchy

```
ADMIN (Full Access)
  └── STAFF (Clinic Operations)
        └── PATIENT (Self-Service)
```

### Role Definitions

| Role        | Deskripsi                               | Registrasi            |
| ----------- | --------------------------------------- | --------------------- |
| **ADMIN**   | Administrator sistem dengan akses penuh | Manual (database)     |
| **STAFF**   | Staf klinik untuk operasional harian    | Manual (database)     |
| **PATIENT** | Pasien yang dapat booking sendiri       | Self-register via API |

---

## Access Restrictions by Role

### PATIENT

Pasien memiliki akses terbatas hanya untuk keperluan pribadi:

| Fitur                     | Akses | Catatan                  |
| ------------------------- | ----- | ------------------------ |
| Register akun baru        | ✅    | Public endpoint          |
| Login/Logout              | ✅    | -                        |
| Lihat daftar dokter       | ✅    | Public data              |
| Lihat slot tersedia       | ✅    | Per dokter & tanggal     |
| **Buat booking**          | ✅    | Hanya untuk diri sendiri |
| Lihat booking sendiri     | ✅    | Hanya milik sendiri      |
| Batalkan booking sendiri  | ✅    | Hanya milik sendiri      |
| Konfirmasi booking        | ❌    | Staff/Admin only         |
| Lihat booking pasien lain | ❌    | -                        |

### STAFF

Staf klinik dapat mengelola operasional booking:

| Fitur                           | Akses | Catatan                             |
| ------------------------------- | ----- | ----------------------------------- |
| Semua fitur PATIENT             | ✅    | -                                   |
| **Buat booking** untuk pasien   | ✅    | Atas nama pasien                    |
| Lihat semua booking per dokter  | ✅    | Filter by date                      |
| Lihat semua booking per tanggal | ✅    | Filter by clinic                    |
| **Konfirmasi booking**          | ✅    | Mengubah status PENDING → CONFIRMED |
| Batalkan booking pasien         | ✅    | Dengan reason                       |

### ADMIN

Administrator memiliki akses penuh:

| Fitur                   | Akses | Catatan           |
| ----------------------- | ----- | ----------------- |
| Semua fitur STAFF       | ✅    | -                 |
| Logout semua device     | ✅    | Revoke all tokens |
| Manage users (future)   | ✅    | CRUD users        |
| Manage clinics (future) | ✅    | CRUD clinics      |
| Manage doctors (future) | ✅    | CRUD doctors      |

---

## Endpoint Access Matrix

### Public Endpoints (No Auth Required)

```
POST /api/auth/register     - Registrasi patient baru
POST /api/auth/login        - Login
POST /api/auth/refresh      - Refresh token
POST /api/auth/logout       - Logout (revoke token)
GET  /api/clinics           - List clinics
GET  /api/clinics/{id}      - Clinic detail
GET  /api/doctors           - List doctors
GET  /api/doctors/{id}      - Doctor detail
GET  /api/doctors/{id}/available-slots - Available slots
```

### Protected Endpoints

| Endpoint                         | PATIENT | STAFF | ADMIN |
| -------------------------------- | ------- | ----- | ----- |
| `POST /api/auth/logout-all`      | ❌      | ❌    | ✅    |
| `POST /api/bookings`             | ✅      | ✅    | ✅    |
| `GET /api/bookings/my`           | ✅      | ✅    | ✅    |
| `GET /api/bookings/doctor/{id}`  | ❌      | ✅    | ✅    |
| `GET /api/bookings/date/{date}`  | ❌      | ✅    | ✅    |
| `DELETE /api/bookings/{id}`      | ✅\*    | ✅    | ✅    |
| `PUT /api/bookings/{id}/confirm` | ❌      | ✅    | ✅    |

> \*PATIENT hanya bisa cancel booking milik sendiri

---

## Booking Workflow

### Who Can Create Bookings?

| Role    | Dapat Booking? | Untuk Siapa?           |
| ------- | -------------- | ---------------------- |
| PATIENT | ✅             | Diri sendiri saja      |
| STAFF   | ✅             | Semua pasien (walk-in) |
| ADMIN   | ✅             | Semua pasien           |

### Booking Lifecycle

```
                    ┌─────────────────────────────────────────┐
                    │         Patient/Staff Creates           │
                    │              Booking                     │
                    └─────────────┬───────────────────────────┘
                                  │
                                  ▼
                    ┌─────────────────────────────────────────┐
                    │           Status: PENDING               │
                    │    (Menunggu konfirmasi staff)          │
                    └─────────────┬───────────────────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
              ▼                   ▼                   ▼
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
    │  Staff Confirms │ │ Patient Cancels │ │  Staff Cancels  │
    │                 │ │                 │ │                 │
    └────────┬────────┘ └────────┬────────┘ └────────┬────────┘
             │                   │                   │
             ▼                   ▼                   ▼
    ┌─────────────────┐ ┌─────────────────────────────────────┐
    │   CONFIRMED     │ │            CANCELLED                │
    │  (Ready untuk   │ │    (Slot kembali tersedia)          │
    │   appointment)  │ └─────────────────────────────────────┘
    └─────────────────┘
```

### Booking Status

| Status      | Deskripsi             | Slot Available? |
| ----------- | --------------------- | --------------- |
| `PENDING`   | Menunggu konfirmasi   | ❌ Blocked      |
| `CONFIRMED` | Dikonfirmasi staff    | ❌ Blocked      |
| `CANCELLED` | Dibatalkan            | ✅ Available    |
| `COMPLETED` | Selesai (future)      | ✅ Historical   |
| `NO_SHOW`   | Tidak datang (future) | ✅ Historical   |

---

## Database Implementation

### Entity Relationship Diagram

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    users     │     │   clinics    │     │   doctors    │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id (PK)      │     │ id (PK)      │     │ id (PK)      │
│ email        │     │ name         │     │ name         │
│ password     │     │ address      │     │ specialization│
│ role (ENUM)  │     │ phone        │◄────│ clinic_id(FK)│
│ name         │     │ is_active    │     │ user_id (FK) │───┐
│ phone        │     └──────────────┘     │ is_active    │   │
│ is_active    │                          └──────────────┘   │
└──────┬───────┘                                  │          │
       │           ┌──────────────────────────────┘          │
       │           │                                         │
       │     ┌─────▼────────┐     ┌─────────────────┐       │
       │     │doctor_schedules│   │    bookings     │       │
       │     ├──────────────┤    ├─────────────────┤       │
       │     │ id (PK)      │    │ id (PK)         │       │
       │     │ doctor_id(FK)│    │ doctor_id (FK)  │───────┘
       │     │ day_of_week  │    │ patient_id (FK) │◄──┐
       │     │ start_time   │    │ booking_date    │   │
       │     │ end_time     │    │ slot_start_time │   │
       │     │ slot_duration│    │ slot_end_time   │   │
       │     │ is_active    │    │ status (ENUM)   │   │
       │     └──────────────┘    │ notes           │   │
       │                         └─────────────────┘   │
       │                                               │
       │     ┌──────────────┐                          │
       └────►│   patients   │──────────────────────────┘
             ├──────────────┤
             │ id (PK)      │
             │ user_id (FK) │
             │ name         │
             │ phone        │
             │ date_of_birth│
             └──────────────┘
```

### Key Tables

#### 1. `users` - User Authentication

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'ADMIN', 'STAFF', 'PATIENT'
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### 2. `doctors` - Doctor Profiles

```sql
CREATE TABLE doctors (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    clinic_id UUID REFERENCES clinics(id),
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100),
    is_active BOOLEAN DEFAULT true
);
```

#### 3. `doctor_schedules` - Working Hours

```sql
CREATE TABLE doctor_schedules (
    id UUID PRIMARY KEY,
    doctor_id UUID REFERENCES doctors(id),
    day_of_week VARCHAR(10) NOT NULL, -- 'MONDAY', 'TUESDAY', etc.
    start_time TIME NOT NULL,          -- e.g., '09:00'
    end_time TIME NOT NULL,            -- e.g., '17:00'
    slot_duration_minutes INT DEFAULT 30,
    is_active BOOLEAN DEFAULT true
);
```

#### 4. `bookings` - Appointment Records

```sql
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    doctor_id UUID REFERENCES doctors(id) NOT NULL,
    patient_id UUID REFERENCES patients(id) NOT NULL,
    booking_date DATE NOT NULL,
    slot_start_time TIME NOT NULL,
    slot_end_time TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    notes TEXT,
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- CRITICAL: Unique partial index untuk prevent double booking
CREATE UNIQUE INDEX uk_bookings_no_double
ON bookings(doctor_id, booking_date, slot_start_time)
WHERE status NOT IN ('CANCELLED');
```

### Anti-Double Booking Implementation

Sistem menggunakan 3 lapis pertahanan:

```
┌─────────────────────────────────────────────────────────────────┐
│                     Layer 1: Slot Alignment                     │
│                                                                 │
│  Validasi waktu request TEPAT pada grid jadwal dokter           │
│  ✓ 09:00, 09:30, 10:00 (30-min grid)                           │
│  ✗ 09:15, 09:45, 10:10 (arbitrary times)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│               Layer 2: Pessimistic Lock + Overlap Check         │
│                                                                 │
│  @Lock(PESSIMISTIC_WRITE)                                       │
│  SELECT * FROM bookings                                         │
│  WHERE slot_start < :newEnd AND slot_end > :newStart            │
│  FOR UPDATE                                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│            Layer 3: Database Unique Partial Index               │
│                                                                 │
│  CREATE UNIQUE INDEX uk_bookings_no_double                      │
│  ON bookings(doctor_id, booking_date, slot_start_time)          │
│  WHERE status NOT IN ('CANCELLED');                             │
│                                                                 │
│  Final safety net untuk race conditions                         │
└─────────────────────────────────────────────────────────────────┘
```

### Query Examples

#### Get Available Slots

```sql
-- 1. Get doctor schedule for the day
SELECT start_time, end_time, slot_duration_minutes
FROM doctor_schedules
WHERE doctor_id = :doctorId
  AND day_of_week = :dayOfWeek
  AND is_active = true;

-- 2. Get existing bookings
SELECT slot_start_time, slot_end_time
FROM bookings
WHERE doctor_id = :doctorId
  AND booking_date = :date
  AND status NOT IN ('CANCELLED');

-- 3. Application calculates available slots by removing booked times
```

#### Create Booking with Lock

```sql
-- Check for overlapping booking (with lock)
SELECT * FROM bookings
WHERE doctor_id = :doctorId
  AND booking_date = :date
  AND slot_start_time < :newEndTime
  AND slot_end_time > :newStartTime
  AND status NOT IN ('CANCELLED')
FOR UPDATE;

-- If no overlap, insert
INSERT INTO bookings (id, doctor_id, patient_id, booking_date,
                      slot_start_time, slot_end_time, status)
VALUES (:id, :doctorId, :patientId, :date, :startTime, :endTime, 'PENDING');
```

---

## Security Implementation

### JWT Token Flow

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  Client  │     │    Server    │     │   Database   │
└────┬─────┘     └──────┬───────┘     └──────┬───────┘
     │                  │                    │
     │  POST /login     │                    │
     │─────────────────►│                    │
     │                  │  Verify password   │
     │                  │───────────────────►│
     │                  │◄───────────────────│
     │                  │                    │
     │  accessToken +   │  Store refresh     │
     │  refreshToken    │  token in DB       │
     │◄─────────────────│───────────────────►│
     │                  │                    │
     │  API Request     │                    │
     │  + accessToken   │                    │
     │─────────────────►│                    │
     │                  │  Validate JWT      │
     │  Response        │  (no DB call)      │
     │◄─────────────────│                    │
     │                  │                    │
     │  POST /refresh   │                    │
     │  + refreshToken  │                    │
     │─────────────────►│                    │
     │                  │  Validate + rotate │
     │  New tokens      │───────────────────►│
     │◄─────────────────│◄───────────────────│
```

### Access Control Annotations

```java
// Controller level security
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public ResponseEntity<?> getBookingsByDoctor(...) { }

@PreAuthorize("hasAnyRole('PATIENT', 'STAFF', 'ADMIN')")
public ResponseEntity<?> createBooking(...) { }

// Service level ownership check (for PATIENT)
if (user.getRole() == Role.PATIENT) {
    if (!booking.getPatient().getUser().getId().equals(userId)) {
        throw new AccessDeniedException("Cannot access other's booking");
    }
}
```

---

## Summary

| Aspek                         | Implementasi                             |
| ----------------------------- | ---------------------------------------- |
| **Authentication**            | JWT dengan access/refresh token rotation |
| **Authorization**             | Role-based (ADMIN, STAFF, PATIENT)       |
| **Booking Access**            | PATIENT (self), STAFF/ADMIN (all)        |
| **Double Booking Prevention** | 3-layer defense                          |
| **Database**                  | PostgreSQL dengan partial unique index   |
| **Rate Limiting**             | 100 RPM global, 10 RPM auth              |
