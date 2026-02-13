# 🏦 SecureBank — Microservices Banking System

A **production-grade banking backend** built with 4 independent Spring Boot microservices, JWT-based security, PostgreSQL, and comprehensive unit testing.

## 🏗 Architecture

```
┌──────────────┐   ┌──────────────┐   ┌──────────────────┐   ┌────────────────────┐
│ User Service │   │   Account    │   │   Transaction    │   │   Notification     │
│   (8081)     │   │   Service    │   │    Service       │   │    Service         │
│              │   │   (8082)     │   │    (8083)        │   │    (8084)          │
│ • Register   │   │ • Create     │   │ • Deposit        │   │ • Log Events       │
│ • Login/JWT  │   │ • Balance    │   │ • Withdraw       │   │ • Audit Trail      │
│ • Profile    │   │ • Freeze     │   │ • Transfer       │   │ • Query Logs       │
└──────┬───────┘   └──────┬───────┘   │ • History        │   └────────┬───────────┘
       │                  │           │ • Pessimistic    │            │
       │                  │           │   Locking        │            │
       └─────── JWT ──────┴───────────┴──────────────────┴────────────┘
          ↕                  ↕                ↕                  ↕
    ┌──────────┐      ┌──────────┐     ┌──────────┐       ┌──────────┐
    │ PG: users│      │PG: accts │     │PG: txns  │       │PG: notifs│
    └──────────┘      └──────────┘     └──────────┘       └──────────┘
```

## ⚡ Tech Stack

| Category | Technologies |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.2, Spring Security, Spring Data JPA |
| **Auth** | JWT (JJWT 0.12.5), BCrypt |
| **Database** | PostgreSQL (per-service), Hibernate ORM |
| **Testing** | JUnit 5, Mockito, H2 (in-memory) |
| **Build** | Maven (Maven Wrapper included) |

## 🔐 Security

- **JWT Authentication** — 24-hour tokens with HMAC-SHA256
- **BCrypt Password Hashing** — secure password storage
- **Role-Based Authorization** — `ADMIN` / `CUSTOMER` roles
- **Stateless Sessions** — no server-side session state
- **Public endpoints**: `/api/v1/auth/register`, `/api/v1/auth/login` only

## 🚀 Quick Start

### Prerequisites
- Java 17+
- PostgreSQL running on `localhost:5432`

### 1. Create Databases
```sql
CREATE DATABASE securebank_users;
CREATE DATABASE securebank_accounts;
CREATE DATABASE securebank_transactions;
CREATE DATABASE securebank_notifications;
```

### 2. Clone & Build
```bash
git clone https://github.com/varunkumarcs22055/SecureBank.git
cd SecureBank
mvnw.cmd install -DskipTests    # Windows
./mvnw install -DskipTests      # Linux/Mac
```

### 3. Run Services
```bash
# Each in a separate terminal
mvnw.cmd spring-boot:run -pl user-service
mvnw.cmd spring-boot:run -pl account-service
mvnw.cmd spring-boot:run -pl transaction-service
mvnw.cmd spring-boot:run -pl notification-service
```

### 4. Run Tests
```bash
mvnw.cmd test
```

## 📡 API Reference

### User Service (`:8081`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register user |
| `POST` | `/api/v1/auth/login` | Public | Login → JWT |
| `GET` | `/api/v1/users/profile` | JWT | View profile |

### Account Service (`:8082`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/accounts` | JWT | Create account |
| `GET` | `/api/v1/accounts/{id}` | JWT | View balance |
| `GET` | `/api/v1/accounts/user/{userId}` | JWT | List accounts |
| `PATCH` | `/api/v1/accounts/{id}/freeze` | ADMIN | Freeze account |
| `PATCH` | `/api/v1/accounts/{id}/unfreeze` | ADMIN | Unfreeze account |

### Transaction Service (`:8083`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/transactions/deposit` | JWT | Deposit |
| `POST` | `/api/v1/transactions/withdraw` | JWT | Withdraw |
| `POST` | `/api/v1/transactions/transfer` | JWT | Transfer |
| `GET` | `/api/v1/transactions/account/{id}` | JWT | History |

### Notification Service (`:8084`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/notifications/log` | JWT | Log event |
| `GET` | `/api/v1/notifications/account/{id}` | JWT | Account logs |
| `GET` | `/api/v1/notifications/user/{id}` | JWT | User logs |

## 📦 Sample Requests

### Register
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"john@bank.com","password":"Str0ng!Pass","fullName":"John Doe","phone":"+1234567890"}'
```

### Login
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@bank.com","password":"Str0ng!Pass"}'
```

### Deposit (with JWT)
```bash
curl -X POST http://localhost:8083/api/v1/transactions/deposit \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"accountId":"<UUID>","amount":5000.00,"description":"Initial deposit"}'
```

### Transfer (Concurrency-Safe)
```bash
curl -X POST http://localhost:8083/api/v1/transactions/transfer \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"<UUID>","toAccountId":"<UUID>","amount":1000.00,"description":"Rent"}'
```

## 🧠 Key Architectural Decisions

| Decision | Rationale |
|---|---|
| **UUID primary keys** | Globally unique, no sequential ID exposure |
| **NUMERIC(15,2) for money** | Avoids floating-point precision issues |
| **Pessimistic write locking** | Prevents race conditions in concurrent transfers |
| **Deterministic lock ordering** | Acquires locks by UUID order to prevent deadlocks |
| **REPEATABLE_READ isolation** | Ensures consistent reads within transfer transactions |
| **DTO pattern** | Decouples entities from API, prevents data leakage |
| **Constructor injection** | Immutable dependencies, testable, Spring best practice |
| **Per-service databases** | True microservices data isolation |
| **BCrypt + JWT** | Industry-standard stateless auth |
| **@ControllerAdvice** | Centralized, consistent error handling |

## 🧪 Testing

- **25 unit tests** across all services
- **Mockito** — mocked repositories for isolation
- **H2** — in-memory DB for integration tests
- Tests cover: happy paths, validation errors, duplicate resources, insufficient balance, frozen accounts, transfer rollback scenarios

## 📂 Project Structure

```
SecureBank/
├── common/                 # Shared: JWT, exceptions, DTOs
├── user-service/           # Auth + profile (port 8081)
├── account-service/        # Account CRUD (port 8082)
├── transaction-service/    # Deposit/Withdraw/Transfer (port 8083)
├── notification-service/   # Audit logging (port 8084)
└── pom.xml                 # Parent POM
```

Each service follows: `entity/` → `repository/` → `dto/` → `service/` → `controller/` → `config/`

## 🛡 Error Response Format
```json
{
  "timestamp": "2026-02-13T21:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient balance. Available: 500.00, Requested: 1000.00",
  "path": "/api/v1/transactions/withdraw"
}
```

---

**Built by Varun Kumar** | Java · Spring Boot · Microservices
