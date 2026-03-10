# Architecture & Development Guidelines

Welcome to the Identity Service (`identity-service`) of The Hive Project. This document outlines the architectural
choices, design patterns, engineering best practices, and development workflows used in this service.

---

## 1. Architectural Patterns

### 1.1. Package-by-Feature Architecture

The project follows a **Package-by-Feature** organization. Each major functional area contains its own controllers,
services, repositories, and entities. This increases modularity and ensures that related code stays together.

* **`auth/`**: Handles registration, login, JWT issuance, password resets, and security filters.
* **`user/`**: Core user management, profile updates, and status handling.
* **`admin/`**: High-level administrative operations and user management for system admins.
* **`internal/`**: Secure Service-to-Service (S2S) endpoints for other microservices in the Hive ecosystem.
* **`common/`**: Shared utilities (e.g., `TsidFactory`), base entities (`BaseEntity`), and global exception handling.

### 1.2. Event-Driven Architecture (EDA)

The service communicates with other microservices asynchronously using **RabbitMQ**.

* Example: When a user requests a password reset, a `ForgotPasswordEvent` is published to RabbitMQ via
  `NotificationProducer.kt`. The Notification Service consumes this to send the reset email, keeping the Identity
  Service decoupled from email logic.

---

## 2. Design Patterns

### 2.1. DTO (Data Transfer Object) Pattern

We strictly separate database entities (`User`) from API payloads (`UserDto`, `RegisterRequest`). This prevents database
schemas from leaking to the client and allows independent evolution of the API and the database.

### 2.2. Mapper Pattern (via Kotlin Extension Functions)

We utilize Kotlin's extension functions for clean and idiomatic mapping between entities and DTOs.

* Example: `fun User.toDto(): UserDto` located in `UserMapper.kt`.

### 2.3. Repository Pattern

Data access is abstracted using Spring Data JPA Repositories. The service layer interacts with these interfaces rather
than dealing directly with SQL, allowing easy mocking during tests.

### 2.4. Factory Pattern

Complex object creation, especially when it involves multiple dependencies or complex logic (like creating a user with
specific roles), is centralized in Factory classes (e.g., `UserFactory.kt`).

---

## 3. Engineering Best Practices

### 3.1. Multi-tenant RBAC (Zero-Trust)

* **Multi-tenant RBAC:** Security is stateless and JWT-driven. Instead of global roles, we use domain-specific
  permissions (e.g., `events:ROLE_ORGANIZER`, `movies:ROLE_USER`).
* **JWT Claims:** The JWT contains a `permissions` map grouping roles by domain (`domain -> List<Role>`).

### 3.2. Data Integrity & Auditing

* **Soft Deletion:** Records are never physically deleted. `BaseEntity` includes a `deleted` flag and `deletedAt`
  timestamp.
* **Optimistic Locking:** Entities use the `@Version` annotation. This prevents "lost updates" if two administrative
  sessions attempt to modify the same user simultaneously.
* **Auditing:** `createdAt`, `updatedAt`, `createdBy`, and `updatedBy` are automatically tracked via `AuditConfig` and
  `BaseEntity`.

### 3.3. Global Exception Handling

Exceptions are thrown from the service layer and intercepted by `GlobalExceptionHandler.kt`. This ensures consistent,
standardized JSON error responses (using `ApiErrorResponse`) across the entire API.

### 3.4. Security-to-Service (S2S) Authentication

Internal endpoints are protected via HMAC-SHA256 signature validation using a shared secret, managed by
`InternalServiceFilter` and `S2SAuthUtil`.

---

## 4. Testing Strategy

* **Unit Tests (`src/test/kotlin/.../unit`):** Fast, isolated tests focusing on business logic using Mockito to mock
  repositories and dependencies.
* **Integration Tests (`src/test/kotlin/.../integration`):** End-to-end API tests using `MockMvc`. These tests verify
  security filters, controller logic, and request/response mapping.

---

## 5. How to Add a New Feature (Developer Guide)

If you need to add a new feature (e.g., "User Preferences"), follow this workflow:

1. **Define the Entity (`user/entity/UserPreference.kt`):**
    * Create the JPA Entity. Inherit from `BaseEntity`.
    * Create the `UserPreferenceRepository` interface.
2. **Define the DTOs (`user/dto/UserPreferenceDto.kt`):**
    * Create response DTOs and request objects with validation.
3. **Define the Service Interface & Implementation (`user/service/`):**
    * Define operations and implement them with `@Transactional`.
4. **Write Mappers (`user/mapper/`):**
    * Add mapping logic to convert between Entity and DTO.
5. **Expose the API (`user/controller/`):**
    * Inject the Service and add REST mappings.
    * Secure endpoints using `@PreAuthorize`.
6. **Write Tests:**
    * Unit tests for the service logic.
    * Integration tests for the controller and security.
