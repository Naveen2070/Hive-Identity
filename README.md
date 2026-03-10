<p align="center">
<img src="https://raw.githubusercontent.com/Naveen2070/The-Hive-Project/main/assets/hive-identity-logo.png" alt="Hive Identity Logo" width="150"/>
</p>

<h1 align="center">Hive-Identity (Auth & User Service)</h1>

<p align="center"><em>The strict gatekeeper and central identity provider for the EventHive ecosystem, managing multi-tenant RBAC and secure user lifecycles.</em></p>

<p align="center">
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
<img src="https://img.shields.io/badge/Framework-Spring_Boot_3-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3"/>
<img src="https://img.shields.io/badge/Database-PostgreSQL-336791?logo=postgresql&logoColor=white" alt="PostgreSQL"/>
<img src="https://img.shields.io/badge/Messaging-RabbitMQ-FF6600?logo=rabbitmq&logoColor=white" alt="RabbitMQ"/>
<img src="https://img.shields.io/badge/Security-JWT_+_HMAC-red" alt="Security"/>
<img src="https://img.shields.io/badge/ID_Gen-TSID-blue" alt="TSID"/>
<img src="https://img.shields.io/badge/Containerization-Docker-2496ED?logo=docker&logoColor=white" alt="Docker"/>
<img src="https://img.shields.io/github/license/Naveen2070/The-Hive-Project" alt="License"/>
</p>

---

> **Hive-Identity** is the foundational security layer of the Hive platform. Built with **Kotlin** and **Spring Boot 3
**, it
> provides a centralized, stateless authentication mechanism using JWTs, implements a robust multi-tenant RBAC model,
> and facilitates secure service-to-service (S2S) communication across the microservice cluster.

---

### 🔗 Associated Repositories

* 👉 **[The-Hive-Project (Main Hub)](https://github.com/Naveen2070/The-Hive-Project)**
* 👉 **[Hive-Event (Core API)](https://github.com/Naveen2070/The-Hive-Project/tree/main/services/core-api)**
* 👉 **[Hive-Forager-UI (Frontend)](https://github.com/Naveen2070/Hive-Forager-UI)**

---

## 🚀 Key Features

* **🛡️ Multi-Tenant RBAC:** Implements a sophisticated domain-based permission model. Users are assigned roles within
  specific domains (e.g., `events:ROLE_ORGANIZER`, `movies:ROLE_USER`), allowing for fine-grained access control across
  different platform segments.
* **🔑 Secure JWT Lifecycle:**
  * **Stateless Auth:** Issues signed JWTs containing a comprehensive `permissions` map and domain access list.
  * **Refresh Tokens:** Database-backed refresh token rotation for secure, long-lived sessions.
  * **Blacklisting:** Immediate token invalidation upon logout using an optimized in-memory blacklist.
* **🤝 Zero-Trust S2S Auth:** Protects internal data-fetching endpoints (`/api/internal/**`) with **HMAC-SHA256
  signatures**. Requires time-sensitive hashes generated with a shared secret to prevent replay attacks and unauthorized
  internal access.
* **🆔 High-Performance IDs:** Utilizes **TSIDs (Time-Sorted Identifiers)** for all primary keys, ensuring global
  uniqueness, URL safety, and optimal database indexing performance.
* **🐇 Async Notification Engine:** Decouples user interactions from notification delivery. Password reset events and
  welcome triggers are published to **RabbitMQ**, ensuring low-latency API responses.
* **👤 Complete User Lifecycle:** Centralized logic for registration, multi-factor profile updates, secure password
  hashing (BCrypt), and automated account status management.
* **👑 Administrative Control:** Powerful admin suite for global user search, manual role provisioning, and account
  auditing/moderation.

---

## 🛠️ Tech Stack

* **Language:** Kotlin (JDK 21)
* **Framework:** Spring Boot 3.4.3
* **Security:** Spring Security, JWT (jjwt 0.12.6), HMAC-SHA256
* **Database:** PostgreSQL 17
* **ORM:** Spring Data JPA (Hibernate)
* **Migration:** Liquibase
* **Messaging:** RabbitMQ (AMQP)
* **ID Generation:** TSID (Hypersistence Utils)
* **API Documentation:** OpenAPI 3 / Swagger (SpringDoc 2.8.5)
* **Build Tool:** Gradle (Kotlin DSL)

---

## 🏗️ Architecture

The project follows a **Feature-Based (Package-by-Feature)** architecture to maximize modularity and maintain clear
domain boundaries. Below are the structural diagrams of the Hive-Identity engine.

### High-Level Ecosystem

```mermaid
flowchart TB

classDef external fill:#f5f5f5,stroke:#9e9e9e,stroke-width:2px,color:#212121
classDef platform fill:#e3f2fd,stroke:#64b5f6,stroke-width:2px,color:#0d47a1
classDef identity fill:#fce4ec,stroke:#f48fb1,stroke-width:2px,color:#c2185b

subgraph USERS ["Users"]
    user[End User]
    admin[Platform Admin]
end

subgraph HIVE ["The Hive Platform Context"]
    frontend["Frontend Applications"]:::platform
    gateway["Nginx API Gateway"]:::platform
    identity["Hive-Identity (IAM Service)"]:::identity
    core_api["Hive-Event (Core API)"]:::platform
    movies["Hive-Movie (Service)"]:::platform
end

subgraph EXTERNAL ["External Systems"]
    email["Email Provider"]:::external
end

user --> frontend
admin --> frontend

frontend --> gateway
gateway --> identity
gateway --> core_api
gateway --> movies

core_api -. HMAC S2S .-> identity
movies -. HMAC S2S .-> identity
identity --> email
```

### Container Architecture

```mermaid
flowchart TB
    classDef edge fill:#fff3e0,stroke:#ffcc80,stroke-width:2px,color:#e65100
    classDef identity fill:#fce4ec,stroke:#f48fb1,stroke-width:2px,color:#c2185b
    classDef db fill:#eceff1,stroke:#b0bec5,stroke-width:2px,color:#263238
    classDef broker fill:#ffebee,stroke:#ef9a9a,stroke-width:2px,color:#b71c1c
    classDef core fill:#e8f5e9,stroke:#a5d6a7,stroke-width:2px,color:#1b5e20

    subgraph EDGE ["Edge Layer"]
      nginx["Nginx API Gateway"]:::edge
    end

    subgraph DOCKER ["Docker Network"]
      direction TB
      
      subgraph SERVICE ["Identity Service"]
        auth["Hive-Identity Engine (Kotlin + Spring Boot)"]:::identity
      end

      subgraph PERSISTENCE ["Data Storage"]
        postgres[(PostgreSQL 17)]:::db
      end

      subgraph MESSAGING ["Event Bus"]
        rabbit[(RabbitMQ)]:::broker
      end
      
      core_api["Hive-Event Engine"]:::core
      movies["Hive-Movie Engine"]:::core
    end

    nginx --> auth
    auth --> postgres
    auth -- Publish Auth Events --> rabbit
    core_api -. HMAC S2S .-> auth
    movies -. HMAC S2S .-> auth
```

### Layered Architecture

```mermaid
flowchart TB

classDef layer_api fill:#e3f2fd,stroke:#90caf9,stroke-width:2px,color:#0d47a1
classDef layer_app fill:#e8f5e9,stroke:#a5d6a7,stroke-width:2px,color:#1b5e20
classDef layer_dom fill:#fff3e0,stroke:#ffcc80,stroke-width:2px,color:#e65100
classDef layer_infra fill:#f3e5f5,stroke:#ce93d8,stroke-width:2px,color:#4a148c

subgraph API_LAYER ["Presentation Layer (api)"]
    ctrl[Controllers]:::layer_api
    dto[DTOs]:::layer_api
    mapper[Mappers]:::layer_api
end

subgraph APP_LAYER ["Application Layer (application)"]
    svc[Service Implementations]:::layer_app
    usecase[Security / JWT Services]:::layer_app
end

subgraph DOMAIN_LAYER ["Domain Layer (domain)"]
    model[Entities / Roles]:::layer_dom
    repo_intf[Repository Interfaces]:::layer_dom
    logic[Auth Business Logic]:::layer_dom
end

subgraph INFRA_LAYER ["Infrastructure Layer (infrastructure)"]
    repo_impl[JPA Repositories]:::layer_infra
    sec[Security Filters / HMAC]:::layer_infra
    mq[RabbitMQ Producers]:::layer_infra
    tsid[TSID Factory]:::layer_infra
end

API_LAYER --> APP_LAYER
APP_LAYER --> DOMAIN_LAYER
APP_LAYER --> INFRA_LAYER
INFRA_LAYER --> DOMAIN_LAYER
```

### Zero-Trust Security Model

```mermaid
flowchart LR

classDef client fill:#e3f2fd,stroke:#90caf9,stroke-width:2px,color:#0d47a1
classDef gateway fill:#fff3e0,stroke:#ffcc80,stroke-width:2px,color:#e65100
classDef identity fill:#fce4ec,stroke:#f48fb1,stroke-width:2px,color:#c2185b
classDef service fill:#e8f5e9,stroke:#a5d6a7,stroke-width:2px,color:#1b5e20

user[User]:::client
gateway[Nginx Gateway]:::gateway
identity[Hive-Identity]:::identity
events[Hive-Event / Movie]:::service

user -->|1. Authenticate| identity
identity -->|2. Issue Signed JWT| user
user -->|3. Request with JWT| gateway
gateway -->|4. Route Request| events
events -.->|5. HMAC S2S Request| identity
identity -.->|6. Resolve User Data| events
```

### Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    APP_USERS ||--o{ USER_ROLES : "has"
    ROLES ||--o{ USER_ROLES : "defined in"
    APP_USERS ||--o{ REFRESH_TOKENS : "owns"
    APP_USERS ||--o{ PASSWORD_RESET_TOKENS : "requests"

    APP_USERS {
        long id PK "TSID (Time-Sorted ID)"
        string email UK "Unique email address"
        string password_hash "BCrypt hashed password"
        string full_name "User's display name"
        jsonb domain_access "JSON array of allowed domains"
        long created_by FK "References APP_USERS(id)"
        long updated_by FK "References APP_USERS(id)"
        long deleted_by FK "References APP_USERS(id)"
        timestamp created_at "Creation timestamp"
        timestamp updated_at "Last update timestamp"
        long version "Optimistic locking version"
        boolean is_active "Global activity status"
        boolean is_deleted "Soft-delete flag"
        timestamp deleted_at "Timestamp of deletion"
    }

    ROLES {
        int id PK "Auto-increment ID"
        string name UK "Role name (e.g. ROLE_USER, ROLE_ADMIN)"
        long created_by FK "References APP_USERS(id)"
        long updated_by FK "References APP_USERS(id)"
        long deleted_by FK "References APP_USERS(id)"
        timestamp created_at "Creation timestamp"
        timestamp updated_at "Last update timestamp"
        long version "Optimistic locking version"
        boolean is_active "Activity status"
        boolean is_deleted "Soft-delete flag"
        timestamp deleted_at "Timestamp of deletion"
    }

    USER_ROLES {
        long id PK "TSID (Time-Sorted ID)"
        long user_id FK "References APP_USERS(id)"
        int role_id FK "References ROLES(id)"
        string domain "Specific domain (e.g. 'events', 'movies')"
        long created_by FK "References APP_USERS(id)"
        long updated_by FK "References APP_USERS(id)"
        long deleted_by FK "References APP_USERS(id)"
        timestamp created_at "Creation timestamp"
        timestamp updated_at "Last update timestamp"
        long version "Optimistic locking version"
        boolean is_active "Status within domain"
        boolean is_deleted "Soft-delete flag"
        timestamp deleted_at "Timestamp of deletion"
    }

    REFRESH_TOKENS {
        long id PK "TSID (Time-Sorted ID)"
        long user_id FK "References APP_USERS(id)"
        string token UK "Unique UUID token"
        timestamp expiry_date "Token expiration time"
        long created_by FK "References APP_USERS(id)"
        long updated_by FK "References APP_USERS(id)"
        long deleted_by FK "References APP_USERS(id)"
        timestamp created_at "Creation timestamp"
        timestamp updated_at "Last update timestamp"
        long version "Optimistic locking version"
        boolean is_active "Status"
        boolean is_deleted "Soft-delete flag"
        timestamp deleted_at "Timestamp of deletion"
    }

    PASSWORD_RESET_TOKENS {
        long id PK "TSID (Time-Sorted ID)"
        long user_id FK "References APP_USERS(id)"
        string token UK "Unique UUID token"
        timestamp expiry_date "Token expiration time"
        long created_by FK "References APP_USERS(id)"
        long updated_by FK "References APP_USERS(id)"
        long deleted_by FK "References APP_USERS(id)"
        timestamp created_at "Creation timestamp"
        timestamp updated_at "Last update timestamp"
        long version "Optimistic locking version"
        boolean is_active "Status"
        boolean is_deleted "Soft-delete flag"
        timestamp deleted_at "Timestamp of deletion"
    }
```

---

## 📂 Project Structure

The project follows a **Feature-Based (Package-by-Feature)** architecture to maximize modularity and maintain clear
domain boundaries:

```text
src/main/kotlin/com/thehiveproject/identity_service
├── auth                # Authentication feature (Login, Register, JWT, Refresh Tokens)
│   ├── controller      # Auth REST endpoints
│   ├── dto             # Auth-specific DTOs
│   ├── security        # JWT Filters, UserDetails, S2S Filters
│   └── service         # Auth business logic & token management
├── user                # User management feature (Profiles, Roles, Entities)
│   ├── controller      # User profile REST endpoints
│   ├── entity          # User, Role, UserRole JPA entities
│   ├── mapper          # Entity-to-DTO conversion logic
│   └── service         # User CRUD & Role management
├── admin               # Administrative feature (Global user & role management)
├── internal            # Internal-only endpoints for service-to-service calls
├── common              # Shared logic (TSID Factory, Base Entities, Exceptions)
├── config              # Spring beans, Security, Audit, and RabbitMQ config
└── notification        # RabbitMQ producers for async events
```

---

## ⚙️ Getting Started (How to Run)

### Prerequisites

* **Java 21** (for manual runs)
* **Docker & Docker Compose**
* **RabbitMQ**
* **PostgreSQL**

### 1. Clone the Repository

```bash
git clone https://github.com/Naveen2070/The-Hive-Project.git
cd The-Hive-Project/services/identity-service
```

### 2. Environment Variables (`.env`)

```ini
# Database
DB_USERNAME=admin
DB_PASSWORD=SuperSecretPassword123!

# JWT Security
JWT_SECRET=your_super_secret_jwt_key_here
JWT_EXPIRATION_MS=3600000

# Zero-Trust S2S Config
INTERNAL_SHARED_SECRET=your_s2s_shared_key
```

### 3. Run via Docker Compose

```bash
docker-compose up --build -d
```

---

## 🔌 API Endpoints

### 🔐 Authentication

| Method | Endpoint                    | Description                           | Access |
|--------|-----------------------------|---------------------------------------|--------|
| `POST` | `/api/auth/register`        | Register a new user                   | Public |
| `POST` | `/api/auth/login`           | Login and receive JWTs                | Public |
| `POST` | `/api/auth/refresh`         | Rotate access token via refresh token | Public |
| `POST` | `/api/auth/logout`          | Invalidate tokens                     | Auth   |
| `POST` | `/api/auth/forgot-password` | Initiate password reset (Email)       | Public |
| `POST` | `/api/auth/reset-password`  | Complete password reset               | Public |

### 👤 User Profile

| Method   | Endpoint                     | Description                | Access |
|----------|------------------------------|----------------------------|--------|
| `GET`    | `/api/users/me`              | Fetch current user profile | Auth   |
| `PATCH`  | `/api/users/me`              | Update profile (Full Name) | Auth   |
| `POST`   | `/api/users/change-password` | Securely change password   | Auth   |
| `DELETE` | `/api/users/deactivate/me`   | Self-deactivation          | Auth   |

### 👑 Administration

| Method   | Endpoint                       | Description                            | Access        |
|----------|--------------------------------|----------------------------------------|---------------|
| `GET`    | `/api/admin/users`             | Paginated user search                  | `SUPER_ADMIN` |
| `GET`    | `/api/admin/users/{id}`        | Get full user record by ID             | `SUPER_ADMIN` |
| `POST`   | `/api/admin/users`             | Create internal user with manual roles | `SUPER_ADMIN` |
| `PATCH`  | `/api/admin/users/{id}/status` | Toggle user active status (Ban/Unban)  | `SUPER_ADMIN` |
| `DELETE` | `/api/admin/users/{id}/hard`   | Permanent record removal               | `SUPER_ADMIN` |

### 🤖 Internal (S2S)

| Method | Endpoint                    | Description                               | Access   |
|--------|-----------------------------|-------------------------------------------|----------|
| `GET`  | `/api/internal/users/{id}`  | Resolve user summary (for data hydration) | Internal |
| `POST` | `/api/internal/users/batch` | Batch resolve user summaries              | Internal |

---

<p align="center">
Built with ❤️, ☕, and secure distributed systems.🛡️<br>
<b>Architected and maintained by <a href="https://github.com/Naveen2070">Naveen</a></b>
</p>
