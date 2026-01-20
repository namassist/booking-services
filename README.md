# Clinic Booking Service API

A secure, scalable REST API for online clinic booking with JWT authentication, role-based authorization, and double booking prevention.

## Features

- **JWT Authentication** with access/refresh tokens and token rotation
- **Role-based Authorization** (ADMIN, STAFF, PATIENT)
- **Double Booking Prevention** using pessimistic locking
- **Rate Limiting** (100 requests/minute per IP)
- **Multi-clinic Support** with doctors and schedules

## Technology Stack

- Java 17
- Spring Boot 4.0.1
- Spring Security with JWT
- PostgreSQL + Flyway migrations
- Bucket4j for rate limiting

## Step-by-Step Guide for New Users

Follow these steps to get the project running on your local machine.

### Prerequisites

Ensure you have the following installed:

- [Java Development Kit (JDK) 17+](https://adoptium.net/)
- [PostgreSQL](https://www.postgresql.org/download/) (or Docker)
- [Git](https://git-scm.com/downloads)

---

### 1. Clone the Repository

Open your terminal and run:

```bash
git clone https://github.com/your-username/booking-service.git
cd booking-service
```

### 2. Set Up the Database

You need a running PostgreSQL database. You can start one using Docker:

```bash
docker run -d --name booking-postgres \
  -e POSTGRES_DB=clinic \
  -e POSTGRES_USER=uadmin \
  -e POSTGRES_PASSWORD=secretpisan \
  -p 5432:5432 \
  postgres:16
```

_Or if you have PostgreSQL installed locally, create a database named `clinic`._

### 3. Configure Environment Variables

The application enforces **strict configuration**. You must provide environment variables.

1.  Copy the example environment file:
    ```bash
    cp .env.example .env
    ```
2.  Open `.env` and fill in the values (or use the defaults provided below for local dev):

    ```properties
    DATABASE_URL=jdbc:postgresql://localhost:5432/clinic
    DATABASE_USERNAME=uadmin
    DATABASE_PASSWORD=secretpisan
    SHOW_SQL=true
    JWT_SECRET=c2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1kZXZlbG9wbWVudC1vbmx5LWNoYW5nZS1pbi1wcm9kdWN0aW9u
    JWT_ACCESS_EXPIRATION=900000
    JWT_REFRESH_EXPIRATION=604800000
    RATE_LIMIT_RPM=100
    PORT=8080
    LOG_LEVEL_SECURITY=INFO
    LOG_LEVEL_APP=DEBUG
    ```

### 4. Run the Application

#### Option A: Using Command Line (Linux/Mac)

You need to export the variables before running because Spring Boot doesn't read `.env` by default.

```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

#### Option B: Using VS Code (Recommended)

1.  Open the project in VS Code.
2.  If you have the **Spring Boot Extension Pack** installed, it should detect the project.
3.  Create/Update `.vscode/launch.json` to include `"envFile": "${workspaceFolder}/.env"` in the configuration.
4.  Press `F5` to start debugging.

### 5. Verify & Test (Seeded Data)

When the application starts for the **first time**, a `DataSeeder` will populate the database with dummy data.

**API URL**: `http://localhost:8080`

#### Default Credentials

Use these accounts to log in and test:

| Role        | Email                 | Password   | Notes                        |
| ----------- | --------------------- | ---------- | ---------------------------- |
| **Admin**   | `admin@example.com`   | `password` | Has full access              |
| **Patient** | `patient@example.com` | `password` | Linked to patient "John Doe" |

#### Default Doctors

| Name            | Specialization       | Schedule                  |
| --------------- | -------------------- | ------------------------- |
| **Dr. Strange** | General Practitioner | Mon & Wed (09:00 - 12:00) |
| **Dr. Doom**    | Dermatologist        | Tue & Thu (13:00 - 16:00) |

---

## Environment Variables Reference

| Variable                 | Description                   |
| ------------------------ | ----------------------------- |
| `DATABASE_URL`           | PostgreSQL JDBC URL           |
| `DATABASE_USERNAME`      | Database username             |
| `DATABASE_PASSWORD`      | Database password             |
| `JWT_SECRET`             | Base64-encoded 256-bit secret |
| `JWT_ACCESS_EXPIRATION`  | Access token TTL (ms)         |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms)        |
| `RATE_LIMIT_RPM`         | Requests per minute limit     |
| `PORT`                   | Server port                   |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Clients                              │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/JWT
┌─────────────────────────▼───────────────────────────────────┐
│                    Rate Limiting Filter                      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  JWT Authentication Filter                   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      Controllers                             │
│   AuthController │ BookingController │ DoctorController      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                       Services                               │
│         AuthService │ BookingService (concurrency)          │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      Repositories                            │
│          with @Lock(PESSIMISTIC_WRITE)                      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                     PostgreSQL                               │
│              with unique partial indexes                     │
└─────────────────────────────────────────────────────────────┘
```

## Anti-Double Booking Strategy

The system uses a **two-layer protection** approach:

### 1. Database Constraint (Primary)

```sql
CREATE UNIQUE INDEX uk_bookings_no_double
ON bookings(doctor_id, booking_date, slot_start_time)
WHERE status NOT IN ('CANCELLED');
```

This partial unique index prevents duplicate active bookings at the database level.

### 2. Pessimistic Locking (Application)

The repository uses `SELECT FOR UPDATE` to acquire an exclusive lock before inserting, serializing concurrent booking attempts.

## Running Tests

```bash
# Ensure test environment variables are set or rely on h2 in test profile
./mvnw test
```

## License

MIT
