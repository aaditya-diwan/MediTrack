# MediTrack — Complete Interview Guide

Everything you need to explain this project in depth: what it does, every architectural decision, and the reasoning behind each one.

---

## Table of Contents

1. [What Is MediTrack?](#1-what-is-meditrack)
2. [System Architecture Overview](#2-system-architecture-overview)
3. [Domain-Driven Design](#3-domain-driven-design)
4. [Hexagonal Architecture](#4-hexagonal-architecture)
5. [Patient Service — Deep Dive](#5-patient-service--deep-dive)
6. [Lab Service — Deep Dive](#6-lab-service--deep-dive)
7. [Insurance Service — Deep Dive](#7-insurance-service--deep-dive)
8. [Event-Driven Architecture & Kafka](#8-event-driven-architecture--kafka)
9. [Transactional Outbox Pattern](#9-transactional-outbox-pattern)
10. [Security Architecture](#10-security-architecture)
11. [Caching Strategy](#11-caching-strategy)
12. [CQRS Pattern](#12-cqrs-pattern)
13. [Observability Stack](#13-observability-stack)
14. [Database Design](#14-database-design)
15. [API Gateway & Infrastructure](#15-api-gateway--infrastructure)
16. [Next.js Frontend](#16-nextjs-frontend)
17. [Key Architectural Trade-offs](#17-key-architectural-trade-offs)
18. [Common Interview Questions & Answers](#18-common-interview-questions--answers)

---

## 1. What Is MediTrack?

MediTrack is a **healthcare management platform** built as a set of microservices. It manages three core domains of a hospital's back-end operations:

- **Patients** — registering and tracking patients, their contact info, and medical history
- **Laboratory** — ordering lab tests, recording results, flagging critical values
- **Insurance** — storing insurance policies, tracking deductibles and out-of-pocket costs

The system is designed so that each domain is completely independent: it has its own database, its own codebase, and communicates with other services only through Kafka events — never by calling each other directly.

**Tech stack:**
- Java 21, Spring Boot 3.2.4
- Apache Kafka (event broker)
- PostgreSQL (primary database per service)
- Redis (cache per service)
- Next.js 16 (frontend UI)
- Kong (API gateway)
- Prometheus + Grafana + Jaeger (observability)

---

## 2. System Architecture Overview

```
Browser / Mobile
      │
      ▼
  Kong API Gateway (port 8000)
      │
   ┌──┴──────────────────────────┐
   │                             │
Patient Service (8081)     Lab Service (8082)
   │                             │
   └──────────── Kafka ──────────┘
                   │
          Insurance Service (8083)

Each service:
├── own PostgreSQL schema
├── own Redis cache
└── own JWT validation
```

**Why microservices?** Each domain has a different rate of change, different scaling needs, and different team ownership. The lab service might need to scale independently during peak testing hours without scaling up patient registration.

**Why not a monolith first?** The domains are bounded contexts with clear, stable interfaces between them. They communicate through well-defined events, which is a sign that microservices are appropriate rather than premature.

---

## 3. Domain-Driven Design

MediTrack applies DDD across all three services. Understanding DDD is critical for explaining why the code is structured the way it is.

### Core Concepts Used

**Bounded Contexts**

Each service is a bounded context — a linguistic and technical boundary within which a term has one precise meaning. "Patient" in the lab service is just a `patientId` string and a name snapshot. "Patient" in the patient service is a full aggregate with medical history. Same word, different meaning, different boundary.

**Aggregates**

An aggregate is a cluster of objects treated as a single unit for data changes. Every write goes through the aggregate root — nothing reaches inside and modifies a child entity directly.

| Service | Aggregate Root | Child Entities |
|---------|---------------|---------------|
| Patient | `Patient` | `MedicalRecord` |
| Lab | `LabOrder` | `TestInfo`, `DiagnosisCode` |
| Lab | `LabResult` | — |
| Insurance | `InsurancePolicy` | — |

For example, you never create a `MedicalRecord` without going through the `Patient` aggregate. The patient controls the consistency of its own medical history.

**Entities vs Value Objects**

Entities have identity — two patients are different even if all their fields match, because they have different IDs. Value objects have no identity — two `ContactInfo` objects with the same email are the same thing.

In the patient service:
- `Patient` is an entity — has a `PatientId`
- `MedicalRecord` is an entity — has a `recordId`
- `ContactInfo` (email, phone, address) is a value object — replaced in full, never partially updated
- `Insurance` (provider, policyNumber) is a value object
- `PatientId` is a value object wrapping a UUID with a `generate()` factory method — this prevents raw UUIDs from being passed where a PatientId is expected (type safety)

**Repositories**

Repositories are abstractions over data storage. The domain layer defines an interface (`PatientRepository`) and knows nothing about how data is actually stored. The infrastructure layer provides the implementation (`PatientRepositoryImpl`) using Spring Data JPA.

This means you could swap PostgreSQL for MongoDB tomorrow by changing only the infrastructure layer — the domain logic wouldn't change.

---

## 4. Hexagonal Architecture

Also called "Ports and Adapters." The idea is that your business logic lives in the centre of the hexagon, completely isolated from frameworks, databases, and HTTP. The outside world plugs into it through defined ports.

```
         HTTP Controller
               │  (adapter)
               ▼
         ┌─────────┐
         │  App    │ ← Use Case interfaces (ports)
         │  Layer  │
         │         │
         │ Domain  │
         └─────────┘
               │
        ┌──────┴──────┐
   JPA Repo         Kafka Producer
  (adapter)          (adapter)
```

### Layers in MediTrack

**Domain layer** — pure Java. No Spring annotations. Contains entities, value objects, repository interfaces (ports), and domain logic. It can be unit-tested without starting Spring.

**Application layer** — orchestrates use cases. Contains service classes, DTOs, and mappers. Calls domain objects and repository ports. Has `@Transactional` but no HTTP or JPA specifics.

**Infrastructure layer** — implements the ports. Contains JPA entities and repositories, Kafka producers and consumers, Redis config, JWT filter, security config. This layer depends on frameworks.

**Interfaces layer** — the inbound adapters. REST controllers translate HTTP requests into application layer calls. `GlobalExceptionHandler` translates domain exceptions into HTTP responses.

**Why this matters in an interview:** The architecture ensures that if Spring Boot is deprecated tomorrow, the domain logic doesn't change. The investment in business logic is protected.

---

## 5. Patient Service — Deep Dive

### What it does

Patient service is the source of truth for:
1. Patient demographics (name, DOB, contact info, SSN, MRN)
2. Medical records (diagnoses and treatments)
3. Authentication — it issues JWTs for the whole platform

### Domain Model

```
Patient (Aggregate Root)
├── PatientId (value object — wraps UUID)
├── MRN (value object)
├── SSN (value object)
├── ContactInfo (value object: email, phone, address)
├── Insurance (value object: provider, policyNumber)
└── List<MedicalRecord>
        ├── recordId
        ├── diagnosis
        ├── treatment
        └── date
```

`Patient.create()` is a factory method that validates the invariants before constructing the object. You can't create an invalid `Patient`.

### CQRS

The application layer splits into two services:

`PatientCommandService` handles writes:
- `createPatient()` — checks for duplicate MRN, persists, increments `meditrack_patient_registrations_total` metric counter
- `updatePatient()` — fetches existing, applies changes, increments `meditrack_patient_updates_total`

`PatientQueryService` handles reads:
- `getPatientById()` — fetches from cache first (`@Cacheable("patients")`)
- `searchPatients()` — queries by MRN, SSN, first name, last name (`@Cacheable("patient_search")`)
- `getPatientTimeline()` — returns ordered medical records

The facade `PatientApplicationService` implements all use case interfaces and delegates to the two services. This is the only thing the controllers talk to.

**Why CQRS?** Reads and writes have different scaling and caching characteristics. Separating them means caching logic doesn't pollute write paths, and you can independently optimize each side.

### JWT Authentication

The patient service owns authentication for the whole platform because it's the service that knows about users (doctors, nurses, etc.). Other services just validate the token — they don't call the patient service to check it.

**How it works:**

1. Client sends `POST /api/v1/auth/authenticate` with username + password
2. `AuthenticationController` calls Spring Security's `AuthenticationManager.authenticate()`
3. Spring Security calls `UserDetailsService` to load the user, then `BCryptPasswordEncoder` to verify the password
4. On success, `JwtUtil.generateToken()` creates a signed token containing:
   - Subject: username
   - Claim `roles`: `["ROLE_ADMIN", "ROLE_DOCTOR"]`
   - IssuedAt + Expiration (24 hours)
   - Signature: HMAC-SHA256 with a 256-bit secret key from `JWT_SECRET` env var

5. On every subsequent request, `JwtRequestFilter` (extends `OncePerRequestFilter`):
   - Extracts `Authorization: Bearer <token>` header
   - Parses and validates signature using same secret key
   - Checks expiration
   - Extracts roles from claims
   - Builds `UsernamePasswordAuthenticationToken` and sets it in `SecurityContextHolder`
   - Spring Security then applies role-based access rules

**Why JWT over sessions?** Stateless — any instance of any service can validate the token without a central session store. Horizontally scalable with zero coordination.

### Security Config

```java
// Public endpoints
/api/v1/auth/**          → permitAll()
/actuator/health         → permitAll()
/actuator/info           → permitAll()
/swagger-ui/**           → permitAll()

// Role-based rules
POST/PUT/PATCH /api/v1/patients/** → ADMIN, DOCTOR, NURSE
DELETE /api/v1/patients/**         → ADMIN only
GET    /api/v1/patients/**         → ADMIN, DOCTOR, NURSE, LAB_TECH
POST   /api/v1/medical-records/**  → ADMIN, DOCTOR, NURSE
DELETE /api/v1/medical-records/**  → ADMIN only
```

### Kafka Events Published

When a physician places a lab order through the patient service:

```
POST /api/v1/patients/{ssn}/order-labs/
  → LabOrderEventPublisher
    → Kafka topic "patient-events"
      → EventType: "lab.test.ordered.v1"
        → Lab service consumes and creates LabOrder
```

Event payload: `LabTestOrderedPayload` containing an `Order` (orderId, patientId, doctorId, testCode, priority, notes) and a `PatientSnapshot` (mrn, firstName, lastName, dateOfBirth). The snapshot avoids a synchronous patient lookup in the lab service.

---

## 6. Lab Service — Deep Dive

### What it does

Lab service manages the full lifecycle of lab testing:
1. Receives lab order events from the patient service via Kafka
2. Accepts direct lab order creation via REST
3. Accepts lab results posted by technicians
4. Flags critical results (dangerously abnormal values)
5. Publishes result events back onto Kafka for other services

### Domain Model

```
LabOrder (Aggregate Root)
├── id (UUID)
├── patientId
├── facilityId
├── orderingPhysicianId
├── preAuthorizationId
├── orderTimestamp
├── priority (STAT | URGENT | ROUTINE)
├── status (PENDING → RECEIVED → IN_PROGRESS → COMPLETED)
├── List<DiagnosisCode> (ICD-10 codes)
└── List<TestInfo> (testCode, testName, specimenType)

LabResult
├── id (UUID)
├── orderId (FK to LabOrder)
├── testCode
├── loincCode (standardized clinical terminology)
├── resultValue + resultUnit
├── referenceRange
├── abnormalFlag (NORMAL | LOW | HIGH | CRITICALLY_LOW | CRITICALLY_HIGH | ABNORMAL)
├── status (PRELIMINARY → FINAL → CORRECTED | AMENDED)
├── performedBy, performedAt
├── verifiedBy, verifiedAt
└── critical (boolean — derived from abnormalFlag)
```

`LabResult.isCritical()` returns true when `abnormalFlag` is `CRITICALLY_LOW` or `CRITICALLY_HIGH`. This is a business rule on the domain — not in a controller, not in a database query.

### Order Status State Machine

```
PENDING → RECEIVED → IN_PROGRESS → COMPLETED
                  ↘              ↘
               CANCELLED       CANCELLED
```

`RECEIVED` is set immediately when an order is created (either via REST or Kafka consumption). `IN_PROGRESS` when processing begins. `COMPLETED` when all results are submitted.

### Kafka Consumer — LabOrderEventConsumer

The lab service subscribes to the `patient-events` topic and filters for events with type `"lab.test.ordered.v1"`. When it receives one, it calls `CreateLabOrderUseCase` to create the order.

Key design choices:
- **Manual acknowledgment**: The consumer calls `acknowledgment.acknowledge()` only after successfully saving the order. If saving fails, no acknowledgment is sent, and Kafka will re-deliver.
- **Dead Letter Topic**: If the consumer fails after 5 retries with exponential backoff (1s → 2s → 4s → 8s → 16s), the message is sent to a DLT so a human can inspect it without it blocking the main topic.
- **Consumer group**: `lab-service` — only one consumer group processes each message (but within the group, 3 concurrent consumers can process different partitions in parallel).

### Transactional Outbox

This is the most sophisticated part of the lab service and worth explaining carefully. See [section 9](#9-transactional-outbox-pattern) for the full explanation.

### Metrics

The application layer injects `MeterRegistry` (Micrometer) and increments a counter every time an order is created:

```java
meterRegistry.counter("meditrack_lab_orders_created_total").increment();
```

This is a custom business metric (not just HTTP metrics) that you can query in Prometheus and display on Grafana. It lets you track how many lab orders are being placed per minute without writing custom SQL.

### MapStruct

The lab service uses MapStruct for compile-time code generation of mappers between DTOs, domain objects, and JPA entities. It generates all the boilerplate `setField()` calls at compile time, so there's zero reflection overhead at runtime. The other services do this manually.

---

## 7. Insurance Service — Deep Dive

### What it does

Insurance service manages insurance policies attached to patients. It tracks:
- Policy details (payer, plan, group number, subscriber)
- Financial tracking (copay, deductible amount vs amount met, out-of-pocket max vs met)
- Policy lifecycle (`active` flag, effective dates, termination dates)

### Domain Model

```
InsurancePolicy (Aggregate Root)
├── policyId
├── patientId
├── policyNumber (unique — DuplicatePolicyException if same number exists)
├── payerId, payerName
├── planName, groupNumber
├── subscriberId, subscriberName
├── relationship (SELF | SPOUSE | CHILD | PARENT | DOMESTIC_PARTNER | OTHER)
├── effectiveDate, terminationDate
├── active (boolean)
├── copayAmount, deductibleAmount, deductibleMet
├── outOfPocketMax, outOfPocketMet
└── createdAt, updatedAt
```

`InsurancePolicy.initialize()` is called by the application service before persisting. It sets:
- `policyId` = new UUID
- `active` = true
- `deductibleMet` = 0, `outOfPocketMet` = 0
- timestamps

`isActiveOn(LocalDate date)` checks whether `effectiveDate <= date <= terminationDate`, which lets you query historical coverage.

**Optimistic locking**: `InsurancePolicyEntity` has a `@Version` column. If two requests try to update the same policy simultaneously, one will get an `OptimisticLockingFailureException` rather than silently overwriting the other's changes.

### Kafka Consumer

The insurance service subscribes to `patient-events` and listens for `"patient.created.v1"` events. Currently it logs the event — the hook is there for future features like auto-creating an eligibility verification check when a patient is registered.

---

## 8. Event-Driven Architecture & Kafka

### Why Kafka?

The three services never call each other directly over HTTP. If they did:
- The patient service would need to know the lab service's URL
- If the lab service is down, placing a lab order would fail
- Tight coupling — changing the lab service API would break the patient service

With Kafka:
- Services are **temporally decoupled** — the lab service can be down for an hour; when it comes back up, it processes all the events it missed
- Services are **spatially decoupled** — they don't know each other's locations
- Events are durable — stored on disk by Kafka, replayed if needed

### Topic Design

```
patient-events
├── patient.created.v1    → Insurance Service consumes
├── patient.updated.v1    → Insurance Service consumes
├── patient.deleted.v1    → Insurance Service consumes
└── lab.test.ordered.v1   → Lab Service consumes

lab-events
├── lab.results.available.v1  → future consumers
└── lab.critical.result.v1    → future alerting service
```

**Why shared topics with event type discriminators?** It reduces the number of topics to manage and lets a new consumer subscribe to all patient events by subscribing to one topic and filtering on `eventType`. The downside is that consumers receive events they don't care about and have to discard them.

### Producer Config (Critical Settings)

```yaml
acks: all                    # Wait for all in-sync replicas to acknowledge
retries: 3                   # Retry on transient failures
enable-idempotence: true     # Exactly-once produce (deduplication by sequence number)
max-in-flight-requests: 5    # Allow up to 5 in-flight requests per partition
compression-type: snappy     # Compress batches — good balance of CPU vs network
```

`acks: all` is important in a healthcare context — you don't want to lose an event because the broker leader crashed before replicating.

`enable-idempotence` ensures that even if the producer retries (because it didn't receive an ack before timeout), the broker deduplicates by sequence number and won't store the message twice.

### Consumer Config (Critical Settings)

```yaml
auto-offset-reset: earliest  # On first run, start from beginning of topic
isolation-level: read_committed  # Don't consume messages from uncommitted transactions
```

`read_committed` is important because the lab service uses Kafka transactions via the outbox relay. Without this, a consumer could read an event that was later rolled back.

---

## 9. Transactional Outbox Pattern

This is the most important reliability pattern in the codebase. It solves a fundamental distributed systems problem.

### The Problem

Imagine the lab service needs to save a `LabOrder` to its database AND publish an event to Kafka. You can't do both atomically — there's no two-phase commit between a SQL database and Kafka.

**Option A: Save DB first, then publish to Kafka**
- DB save succeeds, Kafka publish fails → event is lost, other services never know the order was created

**Option B: Publish to Kafka first, then save DB**
- Kafka publish succeeds, DB save fails → event exists but no order in DB → inconsistency

### The Solution

The outbox pattern makes the Kafka publish part of the same database transaction as the business write:

**Step 1 — Write (one transaction):**
```
BEGIN TRANSACTION
  INSERT INTO lab_orders (id, patient_id, status, ...) VALUES (...)
  INSERT INTO outbox_events (id, topic, payload, status) VALUES (..., 'PENDING')
COMMIT
```

Both writes succeed or both fail. The database is always consistent.

**Step 2 — Relay (separate scheduled process):**

`OutboxRelay` runs every 5 seconds:
```java
@Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:5000}")
public void relay() {
    List<OutboxEvent> pending = outboxRepo.findByStatus(PENDING);
    for (OutboxEvent event : pending) {
        kafkaTemplate.send(event.getTopic(), event.getPayload()).get(); // blocking
        event.setStatus(PROCESSED);
        outboxRepo.save(event);
    }
}
```

**What each `OutboxEvent` record contains:**

| Field | Example Value |
|-------|--------------|
| id | UUID |
| topic | `lab.test.ordered.v1` |
| aggregateId | order UUID |
| eventType | `lab.test.ordered.v1` |
| payload | JSON string of the full event |
| status | `PENDING` → `PROCESSED` / `FAILED` |
| retryCount | 0, 1, 2 (max 3) |
| createdAt | timestamp |
| processedAt | timestamp |
| errorMessage | failure reason if FAILED |

**Retry logic:** If `kafkaTemplate.send().get()` throws, `retryCount` is incremented. After 3 failures the status becomes `FAILED` and a human needs to investigate — the event is not lost, it's in the database.

**Why `get()` (blocking)?** To prevent the relay from moving to the next event before confirming the previous one was delivered to the broker. Non-blocking sends could outpace the acknowledgment.

### At-Least-Once Delivery

Because the relay could crash after sending to Kafka but before marking the event as `PROCESSED`, it might send the same event twice. This is at-least-once delivery. Consumers should be idempotent — if they receive the same event twice, they should handle it gracefully (check if the order already exists before creating a new one).

### Why only the lab service uses Outbox

The lab service is the most critical path — losing a lab order event means a doctor never gets results. The patient and insurance services use simple Kafka sends, which are acceptable for their use cases (a patient creation event that's lost means the insurance service just won't auto-verify eligibility — recoverable).

---

## 10. Security Architecture

### JWT Flow

```
Login Request
    │
    ▼
AuthenticationController
    │
    ▼
Spring Security AuthenticationManager
    │ loads user from UserDetailsService
    │ verifies password with BCryptPasswordEncoder
    ▼
JwtUtil.generateToken()
    │ HMAC-SHA256 sign with JWT_SECRET
    │ embed roles: ["ROLE_ADMIN"]
    │ expiration: 24 hours
    ▼
Return { "jwt": "eyJ..." }

──────────────────────────────

Protected Request
    │ Authorization: Bearer eyJ...
    ▼
JwtRequestFilter (OncePerRequestFilter)
    │ parse + validate signature
    │ check expiration
    │ extract username + roles
    ▼
SecurityContextHolder.setAuthentication(
    new UsernamePasswordAuthenticationToken(username, null, roles)
)
    │
    ▼
Spring Security method-level @PreAuthorize checks pass
    │
    ▼
Controller method executes
```

### Role Hierarchy

| Role | Can do |
|------|-------|
| `ADMIN` | Everything including delete |
| `DOCTOR` | Create/update patients, records, order labs |
| `NURSE` | Create/update patients and records |
| `LAB_TECH` | Read-only on patients and records |

### Why JWT Secret Must Be 256 Bits

HMAC-SHA256 requires a minimum 256-bit (32-byte) key to be cryptographically secure. Shorter keys make brute-force attacks feasible. The `JWT_SECRET` environment variable enforces this at startup.

### CORS Config

The allowed origins come from the `CORS_ALLOWED_ORIGINS` environment variable. In development this is `localhost:3000` (the Next.js UI) and `localhost:8080`. In production you'd set it to your actual domain. This means the API is only callable from trusted origins in a browser context.

---

## 11. Caching Strategy

### Redis Cache Configuration

All three services use Redis with Spring Cache abstraction (`@Cacheable`, `@CachePut`, `@CacheEvict`).

| Service | Cache Names | TTL |
|---------|------------|-----|
| Patient | `patients`, `patient_search`, `medical_records`, `lab_orders` | 1 hour |
| Lab | `lab_orders`, `lab_results`, `test_catalog` | 24 hours |
| Insurance | `policies`, `eligibility_verifications`, `claims`, `pre_authorizations` | 1 hour |

**Why different TTLs?** Test catalog data changes rarely (new tests are added infrequently), so it can be cached for 24 hours. Patient data changes more frequently (updates to contact info, new medical records), so 1 hour is safer.

**Null values are not cached.** If `getPatientById()` returns null (patient not found), that null is not stored in Redis. This prevents cache poisoning where repeated "not found" queries would fill Redis with null entries.

**Cache prefix is enabled.** Each cache entry has a prefix matching the cache name. This prevents collisions between `patients::uuid1` and `policies::uuid1` if they ever share the same Redis keyspace.

### How `@Cacheable` works

```java
@Cacheable(value = "patients", key = "#patientId")
public PatientResponse getPatientById(UUID patientId) {
    // This block only executes on cache miss
    return patientRepository.findById(patientId)...
}
```

On first call: cache miss → hits database → stores result in Redis → returns result.  
On subsequent calls within 1 hour: cache hit → returns from Redis → database not touched.

---

## 12. CQRS Pattern

CQRS stands for **Command Query Responsibility Segregation**. In the patient service this means the service layer is split into two classes:

**`PatientCommandService`** handles state-changing operations:
- Has write-optimised code paths
- Checks for business rule violations before writing (duplicate MRN)
- Increments Prometheus counters after successful writes
- All methods are `@Transactional` (readOnly = false)

**`PatientQueryService`** handles reads:
- All methods are `@Transactional(readOnly = true)` — tells Hibernate not to dirty-check entities, tells the DB driver to use a read replica if available
- All methods are annotated with `@Cacheable` to short-circuit database queries

**`PatientApplicationService`** is the facade — it implements all use case interfaces and delegates to the right service. Controllers only know about the facade.

**Why not full CQRS with separate read models?** That would mean maintaining a separate denormalized read database that is updated via events. That complexity is justified only at very high read volumes. Here, the cache provides enough performance isolation between reads and writes.

---

## 13. Observability Stack

### Three Pillars

**Metrics — Prometheus + Grafana**

Every service exposes `/actuator/prometheus` with:
- Standard JVM metrics: heap usage, GC pause time, thread count
- HTTP metrics: request count, latency histogram, error rate
- Custom business metrics: `meditrack_patient_registrations_total`, `meditrack_lab_orders_created_total`

Latency histograms have pre-configured SLO buckets: 50ms, 100ms, 200ms, 500ms, 1s, 2s, 3s. This lets you answer "what percentage of requests completed in under 200ms?" directly in Prometheus without post-processing.

Grafana reads from Prometheus and shows dashboards. You can set alert rules: "alert if error rate > 1% for 5 minutes."

**Traces — Jaeger**

Micrometer Tracing with Brave generates a trace ID for every incoming request and propagates it through the call graph. Even though services communicate through Kafka (asynchronous), the trace ID is embedded in the Kafka message headers so the full chain from "patient service received a lab order request" to "lab service created the order" is visible in one trace in Jaeger UI.

Sampling:
- Dev: 100% (every request is traced — good for debugging, expensive in prod)
- Prod: 10% (1 in 10 requests traced — balances observability vs storage cost)

**Logs — Elasticsearch + Kibana**

Logs include the trace ID and span ID:
```
2025-04-17 10:00:00 [http-nio-8081-exec-1] INFO  PatientController [traceId=abc123, spanId=def456] Creating patient MRN-001
```

In Kibana you can search for a trace ID from Jaeger and see all the log lines from all services that belong to that trace. This cross-service log correlation is only possible because trace IDs are propagated automatically.

Note: `BasicBinder TRACE` logging is suppressed to avoid logging patient PHI (Protected Health Information) into SQL bind parameter traces.

---

## 14. Database Design

### Schema Per Service

Each service has its own PostgreSQL database (patient_db, lab_db, insurance_db). There are no cross-service joins — if the lab service needs patient information, it gets it from the `PatientSnapshot` embedded in the Kafka event at the time the order was placed.

**Why?** Shared databases create implicit coupling. If the patient service changes a column name, the lab service breaks. With separate schemas, each service evolves independently.

### Flyway Migrations

Schema changes are applied via Flyway versioned migrations:
- `V1__create_patients_table.sql`, `V2__add_medical_records.sql`, etc.
- Applied automatically at startup
- Baseline-on-migrate is enabled — Flyway won't fail if the schema already partially exists (useful when adding Flyway to an existing database)
- H2-specific variants for local development (PostgreSQL `PL/pgSQL` triggers don't work in H2, so local migrations skip them)

### Key Design Patterns

**Soft delete** is not used — `DELETE /medical-records/{id}` performs a hard delete. In a real HIPAA-compliant system, audit logging of deletions would be required.

**Optimistic locking** in `InsurancePolicyEntity` via `@Version`:
```java
@Version
private Long version;
```
If two API requests both read the policy at version 5 and try to update it, one will fail with `ObjectOptimisticLockingFailureException`. This is preferable to pessimistic locking (which holds a database row lock and kills throughput).

**`@ElementCollection`** in `LabOrderEntity` for `tests` and `diagnosisCodes`. These are stored in separate tables (`lab_order_tests`, `lab_order_diagnosis_codes`) but fetched eagerly with the parent entity. This is appropriate because tests are small and always needed with the order.

**30+ indexes** in the lab database covering status, date, patient_id, code fields. Critical for query performance — a query for "all PENDING orders for patient X" without an index would be a full table scan.

**Trigger-based audit trail** in lab and insurance: PostgreSQL triggers fire on status changes and insert rows into audit tables. This is separate from application-level logging.

---

## 15. API Gateway & Infrastructure

### Kong API Gateway

Kong sits in front of all services on port 8000. It provides:
- **Routing**: routes `/api/v1/patients/**` to the patient service, etc.
- **Rate limiting**: prevents a single client from hammering the services
- **Authentication plugin**: can validate JWTs at the gateway level before requests even reach a service
- **Load balancing**: if multiple instances of a service are running, Kong distributes requests

The admin API on port 8001 lets you configure routes, plugins, and upstreams without restarting Kong.

### Keycloak

Keycloak is an identity provider running on port 8180. It supports OAuth2, OpenID Connect, and SAML. In this project it's present as infrastructure but the application uses its own JWT auth (in patient-service). A production upgrade path would be to replace the custom JWT with Keycloak tokens, letting Keycloak handle user management, password resets, multi-factor authentication, etc.

### Docker Compose

The full stack is defined in `docker-compose.yml`. Key aspects:
- All services share a `meditrack-network` bridge network — they communicate using container names as hostnames
- Persistent volumes for Postgres, Redis, Kafka, Zookeeper, Elasticsearch
- Health checks on every service — application containers have `depends_on: condition: service_healthy` for their dependencies (e.g., patient-service won't start until Kafka, Postgres, and Redis are healthy)
- Environment variables are loaded from a `.env` file at the root

**Startup order:**
1. Zookeeper → Kafka → Schema Registry
2. PostgreSQL (runs init scripts to create all databases)
3. Redis
4. Kong DB → Kong Migrations → Kong
5. Application services (patient, lab, insurance)

---

## 16. Next.js Frontend

### Architecture Decision

The UI uses the **API proxy pattern**: the browser never calls the Java services directly. Instead, Next.js App Router API routes act as a thin server-side proxy that:
1. Reads the `httpOnly` JWT cookie (invisible to JavaScript — XSS-safe)
2. Attaches it as an `Authorization: Bearer` header
3. Forwards the request to the appropriate microservice

```
Browser
  │ fetch("/api/patients/search?query=Jane")
  ▼
Next.js Route Handler (/src/app/api/patients/[...path]/route.ts)
  │ reads httpOnly cookie → attaches Authorization header
  ▼
Patient Service (http://localhost:8081/api/v1/patients/search?query=Jane)
```

**Why this matters:**
- **No CORS issues** — the browser only talks to Next.js (same origin); cross-origin requests happen server-to-server
- **XSS-safe token storage** — `httpOnly` cookies can't be read by JavaScript, so even if a malicious script runs on the page, it can't steal the token
- **One URL for all services** — the client doesn't need to know about ports 8081/8082/8083

### Auth Guard (proxy.ts)

Next.js 16 renamed `middleware.ts` to `proxy.ts`. This file runs on every request before rendering:

```typescript
export function proxy(req: NextRequest) {
  const token = req.cookies.get("token");
  if (!token && !req.nextUrl.pathname.startsWith("/login")) {
    return NextResponse.redirect(new URL("/login", req.url));
  }
  return NextResponse.next();
}
```

If the cookie is missing, any protected route redirects to `/login`. Login itself is excluded from this check.

### Key Pages

| Route | What it does |
|-------|-------------|
| `/login` | Posts credentials to `/api/auth/login`, which calls the patient service and sets the cookie |
| `/patients` | Search bar that calls `/api/v1/patients/search`, renders `PatientCard` list |
| `/patients/[id]` | Tabbed view: Overview, Medical Records, Order Labs |
| `/patients/[id]/records/new` | Form that posts to `/api/v1/medical-records` |
| `/lab/orders` | Multi-section form for creating a `LabOrderRequest` |
| `/lab/orders/[orderId]` | Shows `LabResultResponse` list with color-coded `LabResultBadge` |
| `/lab/results/critical` | Table of all critical results, sorted by `performedAt` desc |
| `/insurance` | Lookup policies by patient UUID, renders `InsurancePolicyCard` |
| `/insurance/new` | Form for `CreatePolicyRequest` |

---

## 17. Key Architectural Trade-offs

### 1. Microservices vs Monolith

**Chose microservices because:** bounded contexts are clear, domains evolve at different rates, different scaling needs.

**Cost:** operational complexity. You need Kafka, separate databases, distributed tracing, and a gateway. A small team would struggle. This is a conscious trade-off for a system designed to grow.

### 2. Kafka vs REST for Inter-Service Communication

**Chose Kafka because:** temporal decoupling, durability, fan-out (multiple services can consume the same event), and the outbox pattern becomes possible.

**Cost:** no synchronous request-response. If the lab service needs to return an order ID immediately, it can't — it creates the order asynchronously. The REST endpoint for placing an order through the patient service returns immediately (200 OK) before the lab service has created the order. The UI must account for this (poll or delay).

### 3. Custom JWT vs Keycloak

**Chose custom JWT for now because:** simpler to set up, no external dependency on Keycloak being healthy.

**Cost:** no user management UI, no password reset flow, no OAuth2/OIDC, no federation with enterprise identity providers. Keycloak is in the stack as the future upgrade path.

### 4. Outbox Pattern only in Lab Service

**Chose selective adoption because:** adding outbox to all services adds complexity and a scheduled relay component.

**Reasoning:** lab results are the most critical domain — a lost "results available" event means a doctor doesn't get notified of critical values. Patient creation events are less critical (if insurance misses a patient.created event, the patient can still be registered manually).

### 5. H2 for Local Dev vs PostgreSQL for Docker

**Chose dual database profiles because:** H2 starts instantly with zero config, making local development fast.

**Cost:** H2 doesn't support PostgreSQL-specific syntax. Flyway migrations have H2-compatible variants (no triggers). You can write code that works in H2 but fails in PostgreSQL — specifically around triggers and PL/pgSQL. The test suite uses H2, so PostgreSQL-specific behavior is only tested in Docker.

### 6. At-Least-Once vs Exactly-Once Delivery

**Chose at-least-once** (via Outbox) over exactly-once because:
- Exactly-once Kafka transactions require Kafka Streams or careful coordination, adding significant complexity
- At-least-once with idempotent consumers is a well-understood and simpler pattern
- The consumer (lab service) is designed to handle duplicate events (check if order already exists)

---

## 18. Common Interview Questions & Answers

**Q: Why didn't you use Spring Cloud or Eureka for service discovery?**

A: Docker's built-in DNS handles service discovery within the compose network — container names resolve to IP addresses. For production, Kubernetes would provide service discovery natively. Eureka adds a service registry that needs to be kept healthy, which is another failure point. Since all services are behind Kong, Kong handles routing — services don't need to discover each other.

---

**Q: How does the lab service guarantee it doesn't lose a Kafka event even if it crashes mid-processing?**

A: Through manual acknowledgment. When a `LabTestOrderedEvent` arrives, the consumer saves the order to the database within a transaction and only then calls `acknowledgment.acknowledge()`. If the service crashes before acknowledging, Kafka will redeliver the message when the service restarts. The order might be created twice — which is why the consumer should check for an existing order before creating.

---

**Q: What happens if the outbox relay crashes halfway through sending events?**

A: Any events that were sent to Kafka but not yet marked `PROCESSED` will be resent when the relay restarts. This is the at-least-once guarantee. Consumers that receive the same event twice need to handle it idempotently (check `eventId` for deduplication, or check if the record already exists).

---

**Q: How do you handle a patient who has multiple insurance policies?**

A: The insurance service stores a `List<InsurancePolicy>` per patient. Each policy has its own `relationship` (SELF, CHILD, etc.) and `active` flag. The endpoint `GET /api/v1/insurance/policies/patient/{patientId}` returns all policies. The domain method `isActiveOn(date)` determines which policies were active on a given date for billing queries.

---

**Q: Why does the lab service use MapStruct but the patient service does manual mapping?**

A: MapStruct was added to the lab service as a deliberate comparison. MapStruct generates mapping code at compile time (annotation processor) — zero reflection, catches missing mappings as compile errors, and eliminates mapping boilerplate. The patient service uses hand-written mappers which are more explicit but more verbose. In a real team you'd standardise on one approach.

---

**Q: What would you change to make this HIPAA-compliant?**

A: Several things:
1. Encrypt SSN and sensitive fields at rest (the `InsurancePolicyEntity` has an annotation placeholder for this)
2. Audit log every read of PHI, not just writes
3. Enforce TLS everywhere (Kong, service-to-service)
4. Implement field-level encryption for SSN/DOB in the database
5. Add a data retention policy — automatically archive or delete records older than the retention period
6. Replace H2 with PostgreSQL in test so encryption extensions (pgcrypto) can be tested
7. Turn off all actuator endpoints except health and info in production, or restrict them behind network ACLs

---

**Q: How would you scale this system if patient volume grew 100x?**

A: Different parts need different approaches:
- **Patient service reads**: horizontal scaling (stateless JWT) + read replicas for PostgreSQL + Redis already handles most reads
- **Lab results**: partition Kafka by `patientId` — related events stay on the same partition, preserving order; add more consumer instances up to the partition count
- **Insurance**: mostly read-heavy — Redis caching + read replicas sufficient
- **Write throughput**: HikariCP pools are already tuned (max 20 connections); PostgreSQL handles this well with connection pooling (PgBouncer in front)
- **Bottleneck candidate**: the outbox relay runs as a single scheduled thread — under extreme volume you'd shard the outbox by tenant or partition and run parallel relays

---

**Q: Why is `@Transactional(readOnly = true)` important?**

A: Three reasons:
1. Tells Hibernate to skip dirty checking at the end of the transaction (entities won't be compared to their snapshot — saves CPU)
2. Tells the JDBC driver (or JPA provider) it may use a read replica if one is configured
3. Documents intent — future developers know this method must not write

---

**Q: How does the Next.js UI handle a 401 when the JWT expires?**

A: The API proxy routes return the backend's status code directly to the browser. A 401 response from the backend passes through the proxy to the browser's `fetch()` call. The page components check for `res.ok` — if false and the status is 401, they redirect to `/login`. A more robust implementation would add a response interceptor that centrally handles 401s across all API calls.

---

**Q: What is the purpose of the `PatientSnapshot` in the `LabTestOrderedPayload`?**

A: When a physician orders a lab test, the lab service needs to know the patient's name and MRN to display on reports. Without the snapshot, the lab service would need to make a synchronous HTTP call to the patient service to look up the patient — introducing tight coupling and a failure point. By embedding the snapshot at the time the event is created, the lab service is self-contained. It's a **read-time denormalization** — the data might drift (if the patient's name changes), but for a lab report, the name at the time of ordering is the correct name to show.

---

**Q: Walk me through exactly what happens when a doctor orders a CBC for a patient.**

A: 

1. The doctor clicks "Order Labs" in the UI on `/patients/{id}`
2. The `OrderLabsTab` component POSTs to `/api/patients/{ssn}/order-labs/` (Next.js proxy)
3. The proxy reads the `httpOnly` JWT cookie, adds it as `Authorization: Bearer` header, forwards to patient-service
4. `PatientLabOrderController` receives the request, validates the JWT (JwtRequestFilter), checks the role (DOCTOR or ADMIN)
5. `LabOrderEventPublisher.publish()` constructs a `LabTestOrderedPayload` with the order details and a `PatientSnapshot`
6. The payload is serialised to JSON and sent to the `patient-events` Kafka topic with `acks: all`
7. Patient service returns 200 OK to the UI
8. On the lab service side, `LabOrderEventConsumer` receives the event from Kafka (consumer group: `lab-service`)
9. It checks `eventType == "lab.test.ordered.v1"`, converts to `LabOrderRequest`, calls `CreateLabOrderUseCase`
10. `LabOrderApplicationService.createOrder()` opens a transaction, calls `LabOrder.initialize()` (sets status=RECEIVED, generates UUID), saves to `lab_orders` table, saves an `OutboxEvent` to `outbox_events` table with status=PENDING — same transaction
11. Transaction commits. `meditrack_lab_orders_created_total` counter increments
12. `acknowledgment.acknowledge()` is called — Kafka offset is committed
13. Five seconds later, `OutboxRelay` picks up the PENDING event, publishes to `lab.test.ordered.v1` Kafka topic, marks PROCESSED
14. The doctor can now query `/api/lab/results/order/{orderId}` — returns empty initially
15. A lab technician later POSTs a result to `/api/v1/lab/results` — the result is saved, `LabResultsAvailableEvent` is published to `lab-events`

---

*This document covers every layer of the system in enough detail to answer any follow-up question in a senior or mid-level backend interview.*
