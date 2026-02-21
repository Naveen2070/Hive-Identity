# Hive-Identity - Authentication & User Management 🔐

> A highly secure, centralized identity provider and user management microservice built with **Kotlin** and **Spring
Boot 3**.

Hive-Identity acts as the strict gatekeeper for the EventHive ecosystem. It is responsible for registering users,
securely hashing passwords, issuing stateless JWTs, managing Role-Based Access Control (RBAC), and acting as the single
source of truth for user data across all internal microservices.

---

### 🔗 Associated Repositories

* 👉 **[EventHive UI (Frontend)](https://github.com/Naveen2070/EventHive-UI)**
* 👉 **[Hive-Event (Core API)](https://github.com/Naveen2070/EventHive)**

---

## 🚀 Key Features

* **🔒 Advanced Authentication:** Issues signed, stateless JSON Web Tokens (JWT) for secure authorization across the
  microservice cluster. Includes secure refresh token rotation and logout blacklisting.
* **🛡️ Industrial-Grade Password Hashing:** Utilizes **Argon2** (the winner of the Password Hashing Competition) to
  defend against GPU-cracking and side-channel attacks.
* **👤 Complete User Lifecycle:** Secure registration, profile management, account deactivation/deletion, and a robust
  Forgot/Reset Password flow using secure, time-limited tokens.
* **🤝 Internal S2S Gatekeeper:** Exposes a strict `/api/internal/` network path protected by a custom Filter enforcing *
  *HMAC-SHA256 timestamped signatures** to prevent replay attacks from internal services.
* **🐇 Async Notifications:** Publishes password reset and welcome emails to the `hive.notifications` RabbitMQ exchange
  to keep API responses blazing fast.
* **👑 Admin RBAC:** Dedicated endpoints for Super Admins to manage system roles, ban/unban users, and perform hard
  deletions.

---

## 🛠️ Tech Stack

* **Language:** Kotlin (JDK 21)
* **Framework:** Spring Boot 3+
* **Database:** PostgreSQL
* **Security:** Spring Security 6, JWT (JJWT), Argon2
* **Message Broker:** RabbitMQ
* **Migration:** Liquibase
* **Build Tool:** Gradle (Kotlin DSL)

---

## 🏗️ Architecture

The project follows a **Feature-Based (Package-by-Feature)** architecture to keep business capabilities strictly
isolated and maintainable. Instead of grouping files by technical layers, code is encapsulated by the features they
belong to:

```text
src/main/kotlin/com/thehiveproject/identity_service
├── admin               # Admin feature (Role assignments, user bans, hard deletes)
├── auth                # Authentication feature (Login, Register, Tokens)
│   ├── controller      # Auth routing
│   ├── dto             # Auth request/response payloads
│   ├── security        # JWT Filters, UserDetailsService, AuthenticationManager
│   └── service         # Auth business logic
├── common              # Shared cross-cutting concerns (DTOs, Exceptions, Utils like S2SAuth)
├── config              # Global Spring configurations (RabbitMQ, Swagger, SecurityConfig)
├── internal.controller # Protected endpoints strictly for Machine-to-Machine (S2S) communication
├── notification        # RabbitMQ Producers (Triggering async email events)
└── user                # User management feature (Profile operations)
    ├── controller      # Profile routing (/me endpoints)
    ├── entity          # User & Role JPA Entities
    ├── repository      # Database interfaces
    └── service         # User CRUD logic
```

---

## 🔒 Security Architecture: Zero-Trust S2S

To ensure absolute zero-trust even within the internal Docker network, Hive-Identity **rejects standard JWTs** on the
`/api/internal/**` path.

Instead, consuming services (like `Hive-Event`) must generate a cryptographically secure hash combining:

1. Their unique Service ID.
2. The exact UNIX timestamp (with a strict 60-second expiration window to prevent replay attacks).
3. A heavily guarded, 256-bit Shared Secret.

If the generated HMAC signature does not mathematically match the Identity Service's expected calculation, the request
is instantly dropped with a `403 Forbidden`.

---

## ⚙️ Getting Started (How to Run)

The application is fully containerized using a multi-stage Dockerfile that builds a lightweight Alpine Linux image
running JDK 21 under a secure, non-root user.

### Prerequisites

* **Java 21** (for manual runs)
* **Docker & Docker Compose**
* **Git**

### 1. Clone the Repository

```bash
git clone [https://github.com/Naveen2070/Hive-Identity.git](https://github.com/Naveen2070/Hive-Identity.git)
cd Hive-Identity

```

### 2. Environment Variables (`.env`)

Before running the application, create a `.env` file in the root directory. The `docker-compose.yml` expects these
variables to configure the `prod` profile:

```ini
# Database Config
DB_USERNAME=admin
DB_PASSWORD=password

# JWT Security
JWT_SECRET=your_super_secret_jwt_key_here
JWT_EXPIRATION_MS=86400000

# RabbitMQ Config
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# S2S Internal Security
INTERNAL_SHARED_SECRET=your_super_secret_shared_key

```

### 3. Run via Docker Compose (Recommended)

This method spins up a PostgreSQL database, a RabbitMQ broker, and the Identity application simultaneously.

```bash
# Build and start the containers in detached mode
docker-compose up --build -d

```

* The Auth API will be available at `http://localhost:8081`.
* The PostgreSQL database is exposed on port `5433` (to prevent conflicts with the Core API's DB).
* The RabbitMQ Management UI is available at `http://localhost:15672` (Login: guest / guest).

To view the logs:

```bash
docker-compose logs -f identity-app

```

### 4. Run Manually (Local Development)

If you prefer to run the Spring Boot application directly on your host machine for debugging:

**Step A: Spin up the infrastructure (Database & RabbitMQ)**

```bash
# Notice we map the DB to 5433 to avoid port collisions
docker run --name hive-identity-db -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=password -e POSTGRES_DB=identity_db -p 5433:5432 -d postgres:17.7-alpine

# Spin up RabbitMQ
docker run --name hive-rabbitmq -p 5672:5672 -p 15672:15672 -d rabbitmq:3-management-alpine

```

**Step B: Run the application via Gradle**
Ensure your local `application-dev.properties` points to `localhost:5433` for Postgres and `localhost:5672` for
RabbitMQ. Then run:

```bash
./gradlew bootRun

```

*Liquibase will automatically migrate the schema on startup.*

---

## 🔌 API Endpoints

### 🔐 Authentication (Public / Auth)

| Method | Endpoint                    | Description                            | Access |
|--------|-----------------------------|----------------------------------------|--------|
| `POST` | `/api/auth/register`        | Register a new user                    | Public |
| `POST` | `/api/auth/login`           | Login and receive Access/Refresh JWTs  | Public |
| `POST` | `/api/auth/refresh`         | Refresh access token                   | Public |
| `POST` | `/api/auth/logout`          | Blacklist token & revoke refresh token | Auth   |
| `POST` | `/api/auth/forgot-password` | Request password reset email           | Public |
| `POST` | `/api/auth/reset-password`  | Reset password via token               | Public |

### 👤 User Management (Profile)

| Method   | Endpoint                     | Description                             | Access |
|----------|------------------------------|-----------------------------------------|--------|
| `GET`    | `/api/users/me`              | Get current authenticated user profile  | Auth   |
| `PATCH`  | `/api/users/me`              | Update current profile details          | Auth   |
| `POST`   | `/api/users/change-password` | Change password (requires old password) | Auth   |
| `DELETE` | `/api/users/deactivate/me`   | Deactivate account                      | Auth   |
| `DELETE` | `/api/users/me`              | Soft delete account                     | Auth   |

### 👑 Admin / Role Management

| Method   | Endpoint                       | Description                                | Access        |
|----------|--------------------------------|--------------------------------------------|---------------|
| `GET`    | `/api/admin/users`             | List/Search all users (Paginated)          | `SUPER_ADMIN` |
| `GET`    | `/api/admin/users/{id}`        | Get detailed user profile by ID            | `SUPER_ADMIN` |
| `POST`   | `/api/admin/users`             | Create internal user (Assign roles manual) | `SUPER_ADMIN` |
| `PATCH`  | `/api/admin/users/{id}/status` | Ban/Unban user (Revokes tokens)            | `SUPER_ADMIN` |
| `DELETE` | `/api/admin/users/{id}/hard`   | Hard delete user from DB (Irreversible)    | `SUPER_ADMIN` |

### 🤖 Internal Machine-to-Machine (S2S)

*(Strictly requires `X-Internal-Service-ID`, `X-Service-Timestamp`, and `X-Service-Token` headers)*

| Method | Endpoint                    | Description                                 | Access        |
|--------|-----------------------------|---------------------------------------------|---------------|
| `GET`  | `/api/internal/users/{id}`  | Get basic user summary by ID                | Internal Only |
| `POST` | `/api/internal/users/batch` | Batch fetch user details for Data Hydration | Internal Only |

---

**Built with ❤️ by naveen**