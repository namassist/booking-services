# Clinic Booking Service API

REST API untuk sistem booking klinik online dengan autentikasi JWT, otorisasi berbasis role, dan pencegahan double booking.

## Fitur Utama

- **Autentikasi JWT** dengan access/refresh token dan token rotation
- **Otorisasi Role-based** (ADMIN, STAFF, PATIENT)
- **Pencegahan Double Booking** (3-layer: slot validation + pessimistic locking + database constraint)
- **Rate Limiting** (100 req/menit global, 10 req/menit untuk login)
- **Account Lockout** (kunci akun setelah 5x login gagal)
- **Correlation ID** untuk request tracing
- **Multi-klinik** dengan dokter dan jadwal

## Technology Stack

- Java 17
- Spring Boot 4.0.1
- Spring Security + JWT (jjwt 0.12.6)
- PostgreSQL + Flyway migrations
- Bucket4j + Caffeine untuk rate limiting

---

## Cara Menjalankan Aplikasi

### Prerequisites

- [JDK 17+](https://adoptium.net/)
- [PostgreSQL](https://www.postgresql.org/download/) atau Docker
- [Git](https://git-scm.com/downloads)

### 1. Clone Repository

```bash
git clone https://github.com/namassist/booking-services.git
cd booking-service
```

### 2. Setup Database

**Menggunakan Docker:**

```bash
docker run -d --name booking-postgres \
  -e POSTGRES_DB=clinic \
  -e POSTGRES_USER=uadmin \
  -e POSTGRES_PASSWORD=secretpisan \
  -p 5432:5432 \
  postgres:16
```

**Atau PostgreSQL lokal:**

```sql
CREATE DATABASE clinic;
```

### 3. Konfigurasi Environment Variables

```bash
cp .env.example .env
```

Edit `.env` dengan nilai yang sesuai (lihat [Environment Variables](#environment-variables)).

### 4. Jalankan Aplikasi

**Linux/Mac:**

```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

**VS Code:**

1. Install **Spring Boot Extension Pack**
2. Tambahkan `"envFile": "${workspaceFolder}/.env"` di `.vscode/launch.json`
3. Tekan `F5`

### 5. Verifikasi

- **API URL**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

**Akun Default (dari DataSeeder):**

| Role    | Email               | Password    |
| ------- | ------------------- | ----------- |
| Admin   | admin@example.com   | secretpisan |
| Patient | patient@example.com | secretpisan |

---

## Environment Variables

### Wajib (Required)

| Variable                 | Deskripsi                           | Contoh                                    |
| ------------------------ | ----------------------------------- | ----------------------------------------- |
| `DATABASE_URL`           | PostgreSQL JDBC URL                 | `jdbc:postgresql://localhost:5432/clinic` |
| `DATABASE_USERNAME`      | Username database                   | `uadmin`                                  |
| `DATABASE_PASSWORD`      | Password database                   | `secretpisan`                             |
| `JWT_SECRET`             | Base64-encoded secret (min 256-bit) | `c2VjdXJlLWp3dC1zZWNyZXQta2V5Li4u`        |
| `JWT_ACCESS_EXPIRATION`  | Access token TTL (ms)               | `900000` (15 menit)                       |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms)              | `604800000` (7 hari)                      |
| `RATE_LIMIT_RPM`         | Rate limit global (req/menit)       | `100`                                     |
| `PORT`                   | Port server                         | `8080`                                    |

### Opsional (dengan default)

| Variable                   | Deskripsi                            | Default                                       |
| -------------------------- | ------------------------------------ | --------------------------------------------- |
| `CORS_ALLOWED_ORIGINS`     | Daftar origin yang diizinkan (koma)  | `http://localhost:3000,http://localhost:5173` |
| `RATE_LIMIT_AUTH_RPM`      | Rate limit endpoint auth (req/menit) | `10`                                          |
| `JWT_ISSUER`               | JWT issuer untuk validasi            | `booking-service`                             |
| `JWT_AUDIENCE`             | JWT audience untuk validasi          | `booking-api`                                 |
| `MAX_FAILED_ATTEMPTS`      | Maks percobaan login sebelum lockout | `5`                                           |
| `LOCKOUT_DURATION_MINUTES` | Durasi lockout akun (menit)          | `15`                                          |
| `SHOW_SQL`                 | Tampilkan SQL di log                 | `false`                                       |
| `LOG_LEVEL_SECURITY`       | Log level Spring Security            | `INFO`                                        |
| `LOG_LEVEL_APP`            | Log level aplikasi                   | `INFO`                                        |

---

## Database & Migration

Aplikasi menggunakan **Flyway** untuk database migration. Migration dijalankan otomatis saat aplikasi start.

### Struktur Migration

| File                                    | Deskripsi                             |
| --------------------------------------- | ------------------------------------- |
| `V1__create_users_table.sql`            | Tabel users dengan role enum          |
| `V2__create_clinics_table.sql`          | Tabel clinics                         |
| `V3__create_doctors_table.sql`          | Tabel doctors                         |
| `V4__create_patients_table.sql`         | Tabel patients                        |
| `V5__create_doctor_schedules_table.sql` | Jadwal dokter per hari                |
| `V6__create_bookings_table.sql`         | Tabel bookings + unique partial index |
| `V7__create_refresh_tokens_table.sql`   | Refresh token storage                 |
| `V10__add_patients_user_id_index.sql`   | Index untuk performa                  |
| `V11__create_login_attempts_table.sql`  | Tracking login attempts untuk lockout |

### Manual Migration

```bash
./mvnw flyway:migrate
```

---

## Arsitektur

```
┌─────────────────────────────────────────────────────────────┐
│                         Clients                              │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP + JWT + X-Correlation-ID
┌─────────────────────────▼───────────────────────────────────┐
│              CorrelationIdFilter (request tracing)          │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│     RateLimitingFilter (100 RPM global, 10 RPM auth)        │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│          JwtAuthenticationFilter (token validation)          │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      Controllers                             │
│   AuthController │ BookingController │ DoctorController      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                       Services                               │
│    AuthService (lockout) │ BookingService (concurrency)     │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      Repositories                            │
│          @Lock(PESSIMISTIC_WRITE) untuk booking             │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                     PostgreSQL                               │
│              Unique partial index + constraints              │
└─────────────────────────────────────────────────────────────┘
```

---

## User Roles & Access Control

Sistem menggunakan 3 role dengan hierarki akses:

| Role        | Deskripsi              | Booking Access             |
| ----------- | ---------------------- | -------------------------- |
| **PATIENT** | Pasien (self-register) | Booking untuk diri sendiri |
| **STAFF**   | Staf klinik            | Booking untuk semua pasien |
| **ADMIN**   | Administrator          | Full access                |

### Endpoint Access Matrix

| Endpoint                         | PATIENT | STAFF | ADMIN |
| -------------------------------- | :-----: | :---: | :---: |
| `POST /api/bookings`             |   ✅    |  ✅   |  ✅   |
| `GET /api/bookings/my`           |   ✅    |  ✅   |  ✅   |
| `DELETE /api/bookings/{id}`      |  ✅\*   |  ✅   |  ✅   |
| `GET /api/bookings/doctor/{id}`  |   ❌    |  ✅   |  ✅   |
| `PUT /api/bookings/{id}/confirm` |   ❌    |  ✅   |  ✅   |

> \*PATIENT hanya bisa cancel booking milik sendiri

📖 **Detail lengkap:** [System Documentation](docs/SYSTEM_DOCUMENTATION.md)

---

## Booking Flow

### Lifecycle

```
  [Patient/Staff]          [Staff]              [Anyone]
       │                      │                    │
       ▼                      │                    │
   ┌────────┐                 │                    │
   │PENDING │─────────────────┼────────────────────┤
   └────┬───┘                 │                    │
        │                     ▼                    ▼
        │              ┌───────────┐        ┌───────────┐
        └─────────────►│ CONFIRMED │        │ CANCELLED │
                       └───────────┘        └───────────┘
```

### Status

| Status      | Deskripsi           | Slot?     |
| ----------- | ------------------- | --------- |
| `PENDING`   | Menunggu konfirmasi | Blocked   |
| `CONFIRMED` | Dikonfirmasi staff  | Blocked   |
| `CANCELLED` | Dibatalkan          | Available |

---

## Entity Relationship (Simplified)

```
┌─────────┐     ┌─────────┐     ┌──────────┐
│ clinics │◄────│ doctors │────►│ bookings │
└─────────┘     └────┬────┘     └────┬─────┘
                     │               │
               ┌─────▼─────┐         │
               │ schedules │    ┌────▼────┐
               │ (M-F,time)│    │ patients│
               └───────────┘    └────┬────┘
                                     │
                                ┌────▼────┐
                                │  users  │
                                │ (auth)  │
                                └─────────┘
```

📖 **ERD lengkap dengan SQL:** [System Documentation](docs/SYSTEM_DOCUMENTATION.md#database-implementation)

---

## Security Implementation

### JWT Token Flow

```
Client                    Server                   Database
  │                          │                        │
  │  POST /login             │                        │
  │─────────────────────────►│  Verify password       │
  │                          │───────────────────────►│
  │  accessToken (15min)     │                        │
  │  refreshToken (7d)       │  Store refresh token   │
  │◄─────────────────────────│───────────────────────►│
  │                          │                        │
  │  API + accessToken       │                        │
  │─────────────────────────►│  Validate (no DB)      │
  │  Response                │                        │
  │◄─────────────────────────│                        │
  │                          │                        │
  │  POST /refresh           │                        │
  │─────────────────────────►│  Rotate token          │
  │  New tokens              │───────────────────────►│
  │◄─────────────────────────│                        │
```

### Security Features

| Feature         | Implementasi                    |
| --------------- | ------------------------------- |
| Authentication  | JWT (access 15min + refresh 7d) |
| Token Storage   | Refresh token di database       |
| Rate Limiting   | 100 RPM global, 10 RPM auth     |
| Account Lockout | 5x failed → 15min lock          |
| Password        | BCrypt hashing                  |

📖 **Detail implementasi:** [System Documentation](docs/SYSTEM_DOCUMENTATION.md#security-implementation)

---

## Booking Conditions & Validation

### Kondisi Booking yang Valid

Untuk membuat booking, semua kondisi berikut harus terpenuhi:

| Kondisi              | Deskripsi                                | Error Jika Gagal                                         |
| -------------------- | ---------------------------------------- | -------------------------------------------------------- |
| **Dokter aktif**     | `doctor.isActive = true`                 | "Doctor is not available for booking"                    |
| **Tanggal valid**    | Hari ini atau masa depan                 | "Booking date cannot be in the past"                     |
| **Slot belum lewat** | Jika hari ini, waktu harus di masa depan | "Cannot book a time slot that has already passed"        |
| **Maks 90 hari**     | Tidak lebih dari 90 hari ke depan        | "Booking date cannot be more than 90 days in the future" |
| **Jadwal tersedia**  | Dokter punya jadwal di hari tersebut     | "Doctor is not available on this day"                    |
| **Dalam jam kerja**  | Waktu dalam jadwal dokter                | "Doctor is not available at X. Schedule is Y to Z"       |
| **Slot aligned**     | Waktu harus tepat pada grid jadwal       | "Appointments must start at N-minute intervals"          |
| **Tidak overlap**    | Tidak bentrok dengan booking lain        | "This time slot conflicts with an existing booking"      |

> **Note:** Same-day booking diizinkan selama slot belum lewat.

### Slot Alignment Validation

Sistem **hanya menerima waktu yang tepat pada grid jadwal dokter**, bukan waktu arbitrary.

**Contoh:** Jadwal dokter 09:00-12:00 dengan durasi slot 30 menit.

| Waktu Request | Status     | Alasan                                   |
| ------------- | ---------- | ---------------------------------------- |
| 09:00         | ✅ Valid   | Tepat pada grid                          |
| 09:30         | ✅ Valid   | Tepat pada grid                          |
| 10:00         | ✅ Valid   | Tepat pada grid                          |
| **09:15**     | ❌ Ditolak | Tidak pada grid (antara 09:00 dan 09:30) |
| **09:45**     | ❌ Ditolak | Tidak pada grid                          |
| **10:10**     | ❌ Ditolak | Tidak pada grid                          |

### Overlap Detection

Sistem mendeteksi **semua jenis overlap waktu**, bukan hanya exact match.

```
Overlap Rule: (A.start < B.end) AND (A.end > B.start)
```

**Contoh:** Booking existing 10:00-10:30

| Request Baru | Overlap? | Keterangan                                 |
| ------------ | -------- | ------------------------------------------ |
| 10:00-10:30  | ✅ Ya    | Exact same time                            |
| 09:30-10:00  | ❌ Tidak | Berakhir tepat saat existing mulai         |
| 10:30-11:00  | ❌ Tidak | Mulai tepat saat existing berakhir         |
| 09:45-10:15  | ✅ Ya    | Partial overlap (start before, end during) |
| 10:15-10:45  | ✅ Ya    | Partial overlap (start during, end after)  |

---

## Strategi Anti-Double Booking

Sistem menggunakan **pendekatan 3 lapis (defense-in-depth)**:

### Layer 1: Slot Alignment (Application)

```java
// BookingService.isValidSlotTime()
// Memvalidasi waktu request tepat pada grid jadwal
LocalTime current = schedule.getStartTime();
while (current.plusMinutes(slotDuration).isBefore(endTime)) {
    if (current.equals(requestedTime)) return true;
    current = current.plusMinutes(slotDuration);
}
return false; // Reject unaligned times
```

Mencegah input waktu arbitrary seperti 10:15 yang bisa bypass unique index.

### Layer 2: Overlap Detection with Lock (Application)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Booking b WHERE b.doctor.id = :doctorId " +
       "AND b.slotStartTime < :newEndTime " +
       "AND b.slotEndTime > :newStartTime " +
       "AND b.status NOT IN ('CANCELLED')")
Optional<Booking> findOverlappingBookingWithLock(...);
```

Mendeteksi overlap waktu untuk booking yang sudah ada dengan pessimistic lock.

### Layer 3: Database Constraint (Final Safety Net)

```sql
CREATE UNIQUE INDEX uk_bookings_no_double
ON bookings(doctor_id, booking_date, slot_start_time)
WHERE status NOT IN ('CANCELLED');
```

Partial unique index mencegah duplikasi di level database, menangkap race condition yang lolos dari application layer.

### Cara Kerjanya (Skenario)

#### Skenario 1: User input waktu tidak valid (09:15)

```
User request: booking jam 09:15 (jadwal dokter 30 menit)

[Layer 1] Slot Alignment Check
    ↓ 09:15 tidak ada di grid (09:00, 09:30, 10:00...)
    ↓ ❌ REJECT → Error: "Slots must start at scheduled intervals"

Hasil: Request ditolak di awal, tidak sampai ke database
```

#### Skenario 2: Slot sudah ada booking

```
User A sudah booking jam 09:00-09:30
User B request: booking jam 09:00

[Layer 1] Slot Alignment Check
    ↓ 09:00 valid ✓

[Layer 2] Overlap Detection
    ↓ Query: ada booking 09:00-09:30 yang overlap dengan 09:00-09:30?
    ↓ Ditemukan booking User A
    ↓ ❌ REJECT → Error: "conflicts with existing booking 09:00 to 09:30"

Hasil: Request ditolak, User A tetap punya booking
```

#### Skenario 3: Dua user booking bersamaan (Race Condition)

```
Slot 09:00 masih kosong
User A dan B klik booking 09:00 di waktu yang PERSIS sama

[Layer 1] Keduanya pass ✓

[Layer 2] Overlap Detection
    ↓ User A: Query → tidak ada booking → pass ✓
    ↓ User B: Query → tidak ada booking → pass ✓
    (Lock tidak efektif karena tidak ada row untuk dikunci)

[Layer 3] Database Constraint
    ↓ User A: INSERT → Success ✓
    ↓ User B: INSERT → ❌ UNIQUE CONSTRAINT VIOLATION
    ↓ GlobalExceptionHandler mendeteksi "uk_bookings_no_double"
    ↓ Return 409 Conflict: "This time slot was just booked by another user"

Hasil: User A berhasil, User B gagal dengan pesan error yang user-friendly
```

#### Ringkasan

| Serangan / Skenario                    | Ditangkap Oleh              | Hasil                 |
| -------------------------------------- | --------------------------- | --------------------- |
| Input waktu sembarangan (09:15, 10:17) | Layer 1 - Slot Alignment    | ❌ Ditolak            |
| Booking saat slot sudah terisi         | Layer 2 - Overlap Detection | ❌ Ditolak            |
| Partial overlap (09:45 vs 10:00-10:30) | Layer 2 - Overlap Detection | ❌ Ditolak            |
| Race condition (2 user bersamaan)      | Layer 3 - DB Constraint     | 1 berhasil, 1 ditolak |

---

## API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint               | Auth   | Deskripsi                            |
| ------ | ---------------------- | ------ | ------------------------------------ |
| POST   | `/api/auth/register`   | Public | Registrasi patient baru              |
| POST   | `/api/auth/login`      | Public | Login, return access & refresh token |
| POST   | `/api/auth/refresh`    | Public | Refresh access token                 |
| POST   | `/api/auth/logout`     | Public | Logout (revoke refresh token)        |
| POST   | `/api/auth/logout-all` | Bearer | Logout dari semua device             |

### Doctors (`/api/doctors`)

| Method | Endpoint                                            | Auth   | Deskripsi                            |
| ------ | --------------------------------------------------- | ------ | ------------------------------------ |
| GET    | `/api/doctors`                                      | Public | List semua dokter (paginated)        |
| GET    | `/api/doctors/{id}`                                 | Public | Detail dokter                        |
| GET    | `/api/doctors/{id}/available-slots?date=YYYY-MM-DD` | Public | Slot tersedia untuk tanggal tertentu |
| GET    | `/api/doctors/clinic/{clinicId}`                    | Public | Dokter per klinik                    |
| GET    | `/api/doctors/search?name=X&specialization=Y`       | Public | Cari dokter                          |

### Clinics (`/api/clinics`)

| Method | Endpoint                     | Auth   | Deskripsi                     |
| ------ | ---------------------------- | ------ | ----------------------------- |
| GET    | `/api/clinics`               | Public | List semua klinik (paginated) |
| GET    | `/api/clinics/{id}`          | Public | Detail klinik                 |
| GET    | `/api/clinics/search?name=X` | Public | Cari klinik                   |

### Bookings (`/api/bookings`)

| Method | Endpoint                                    | Auth                        | Deskripsi                    |
| ------ | ------------------------------------------- | --------------------------- | ---------------------------- |
| POST   | `/api/bookings`                             | Patient, Staff, Admin       | Buat booking baru            |
| GET    | `/api/bookings/my`                          | Patient                     | Booking saya (paginated)     |
| GET    | `/api/bookings/doctor/{id}?date=YYYY-MM-DD` | Staff, Admin                | Booking per dokter & tanggal |
| GET    | `/api/bookings/date/{date}`                 | Staff, Admin                | Semua booking per tanggal    |
| DELETE | `/api/bookings/{id}?reason=X`               | Patient (own), Staff, Admin | Batalkan booking             |
| PUT    | `/api/bookings/{id}/confirm`                | Staff, Admin                | Konfirmasi booking           |

Dokumentasi lengkap tersedia di **Swagger UI**: `/swagger-ui.html`

---

## Paginated Response Format

Semua endpoint yang mengembalikan list menggunakan format pagination standar:

```json
{
  "success": true,
  "data": {
    "items": [...],
    "meta": {
      "page": 0,
      "pageSize": 10,
      "itemCount": 5,
      "totalItems": 25,
      "totalPages": 3,
      "hasNext": true,
      "hasPrev": false
    },
    "links": {
      "self": "/api/doctors?page=0&pageSize=10&sort=name,asc",
      "next": "/api/doctors?page=1&pageSize=10&sort=name,asc",
      "prev": null
    }
  },
  "timestamp": "2026-01-22T06:00:00+07:00"
}
```

| Field             | Deskripsi                       |
| ----------------- | ------------------------------- |
| `items`           | Array of data items             |
| `meta.page`       | Current page number (0-indexed) |
| `meta.pageSize`   | Items per page                  |
| `meta.itemCount`  | Items in current page           |
| `meta.totalItems` | Total items across all pages    |
| `meta.totalPages` | Total number of pages           |
| `meta.hasNext`    | Has next page                   |
| `meta.hasPrev`    | Has previous page               |

---

## Running Tests

```bash
./mvnw test
```

---

## License

MIT
