# Clinic Booking Service API

REST API untuk sistem booking klinik online dengan autentikasi JWT, otorisasi berbasis role, dan pencegahan double booking.

## Fitur Utama

- **Autentikasi JWT** dengan access/refresh token dan token rotation
- **Otorisasi Role-based** (ADMIN, STAFF, PATIENT)
- **Pencegahan Double Booking** (2-layer: pessimistic locking + database constraint)
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
git clone https://github.com/your-username/booking-service.git
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

## Strategi Anti-Double Booking

Sistem menggunakan **pendekatan 2 lapis (defense-in-depth)**:

### Layer 1: Database Constraint (Primary)

```sql
CREATE UNIQUE INDEX uk_bookings_no_double
ON bookings(doctor_id, booking_date, slot_start_time)
WHERE status NOT IN ('CANCELLED');
```

Partial unique index ini mencegah duplikasi booking aktif di level database. Jika 2 request concurrent berhasil melewati application layer, database akan menolak yang kedua dengan constraint violation.

### Layer 2: Pessimistic Locking (Application)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Booking b WHERE ...")
Optional<Booking> findExistingBookingWithLock(...);
```

Menggunakan `SELECT FOR UPDATE` untuk mengunci row yang sudah ada, mencegah race condition untuk slot yang sama.

### Cara Kerjanya

| Skenario                          | Layer 1 (DB)          | Layer 2 (Lock)                | Hasil |
| --------------------------------- | --------------------- | ----------------------------- | ----- |
| Slot sudah terisi                 | ✅ Mencegah           | ✅ Mencegah                   | Aman  |
| 2 request concurrent, slot kosong | ✅ Mencegah (1 gagal) | ❌ Tidak bisa lock row kosong | Aman  |
| Lock timeout/failure              | ✅ Tetap mencegah     | -                             | Aman  |

---

## API Endpoints

| Method | Endpoint                    | Role                        | Deskripsi               |
| ------ | --------------------------- | --------------------------- | ----------------------- |
| POST   | `/api/auth/register`        | Public                      | Registrasi patient baru |
| POST   | `/api/auth/login`           | Public                      | Login                   |
| POST   | `/api/auth/refresh`         | Public                      | Refresh access token    |
| POST   | `/api/auth/logout`          | Public                      | Logout (revoke token)   |
| GET    | `/api/doctors`              | Public                      | List semua dokter       |
| GET    | `/api/doctors/{id}/slots`   | Public                      | Slot tersedia           |
| POST   | `/api/bookings`             | Patient, Staff, Admin       | Buat booking            |
| GET    | `/api/bookings/my`          | Patient                     | Booking saya            |
| GET    | `/api/bookings/doctor/{id}` | Staff, Admin                | Booking per dokter      |
| DELETE | `/api/bookings/{id}`        | Patient (own), Staff, Admin | Batalkan booking        |

Dokumentasi lengkap tersedia di **Swagger UI**: `/swagger-ui.html`

---

## Running Tests

```bash
./mvnw test
```

---

## License

MIT
