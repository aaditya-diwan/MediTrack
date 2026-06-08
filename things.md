# MediTrack — Improvement Checklist

A comprehensive list of every improvement identified across the entire repository.
Organized by priority and category.

---

## CRITICAL — Fix Immediately

### Security
- [x] **`SecurityConfig.java`** — `.anyRequest().permitAll()` means every endpoint is open. Lock down routes with proper role-based rules (e.g. `.anyRequest().authenticated()`).
- [x] **`docker-compose.yml`** — Hardcoded credentials (`meditrack_password`, `meditrack_redis_password`, `admin`) must be extracted into a `.env` file. Add `.env.example` for developers.
- [x] **JWT secret** — `jwt.secret` in `application.properties` is a placeholder. It must be injected via environment variable and never committed to git.
- [x] **No input validation on `LabOrderRequest.java`** — Add `@Valid`, `@NotNull`, `@NotBlank`, and size constraints to all request DTOs. Update controllers to use `@Valid` on `@RequestBody`.

### Logging
- [x] **`InsuranceApiClient.java`** — 3 `System.out.println()` calls. Replace with SLF4J (`@Slf4j` + `log.info/warn/error`).
- [x] **`NotificationApiClient.java`** — 6 `System.out.println()` calls. Replace with SLF4J.
- [x] **`PatientEventProducer.java`** — 5 `System.out.println()` calls. Replace with SLF4J.
- [x] Audit every file for remaining `System.out.println()` usage and remove all of them from production code paths.
- [x] **PHI log leak** — Removed `org.hibernate.type.descriptor.sql.BasicBinder: TRACE` from all three `application.yml` files. This level logs every JDBC bind parameter value (SSNs, dates, diagnoses) in plaintext.
- [x] **`AuthenticationRequest` / `AuthenticationResponse`** extracted to proper top-level files with Lombok. Added `@NotBlank` validation. Controller method return type tightened from `ResponseEntity<?>` to `ResponseEntity<AuthenticationResponse>`.

### Bugs Fixed
- [x] **`LabOrderEventConsumer` setter mismatch** — `testInfo.setNotes()` → `testInfo.setClinicalNotes()` (field is `clinicalNotes` in `TestInfoDto`; would not have compiled).
- [x] **`InsurancePolicy` missing `Relationship` enum** — created `Relationship.java` in insurance-service domain; service would not have compiled without it.
- [x] **Patient→Lab event payload mismatch** — Created `LabTestOrderedPayload.java` in patient-service matching the `LabTestOrderedEvent` shape the lab consumer expects. Added `sendLabTestOrderedEvent()` to `PatientEventProducer`. Added `PATIENT_EVENTS = "patient-events"` and `EVENT_TYPE_LAB_TEST_ORDERED` to patient `EventTopics`. The patient service was previously sending a raw `String patientId` to the wrong topic; the consumer was trying to deserialize it as a structured JSON object and would fail every message.

### New
- [x] **CI/CD** — `.github/workflows/ci.yml` added. Runs build + test + OWASP dependency check for each service on every push to `main` and every PR.
- [x] **Unbounded `getCriticalResults()`** — Added `limit` parameter (1–500, default 100) to service and controller. Was previously fetching the entire table with no cap.
- [x] **`IllegalArgumentException` → proper domain exceptions** in `LabResultApplicationService` — now throws `LabOrderNotFoundException` and `LabResultNotFoundException` which map to 404 via the `GlobalExceptionHandler`.

### Test Coverage
- [x] **All test files in `patient-service` are commented out** — Uncommented and fixed:
  - `PatientControllerTest.java` — rewritten as `@WebMvcTest` (security excluded, `@MockBean` use cases, tests 201/200/404)
  - `MedicalRecordControllerTest.java` — rewritten as `@WebMvcTest` (5 tests covering CRUD + 404)
  - `PatientCommandServiceTest.java` — fixed to inject `SimpleMeterRegistry` (constructor injection after metrics addition)
  - `PatientQueryServiceTest.java` — already working, no changes needed
  - `MedicalRecordApplicationServiceTest.java` — already working, no changes needed
- [x] **`lab-service` test class is empty** — Added `LabOrderApplicationServiceTest` (outbox, metrics, initialize) and `LabResultApplicationServiceTest` (normal/critical/not-found/limit enforcement).
- [x] **`insurance-service` test class is empty** — Added `InsurancePolicyApplicationServiceTest` (create/duplicate/getById/notFound/list).

---

## HIGH PRIORITY — Fix Before Production

### Incomplete Implementations (Stubs)
- [x] **`PatientEventProducer.java`** — All three events now publish to Kafka via `KafkaTemplate` with proper SLF4J logging. Thread.sleep removed.
- [ ] **`InsuranceApiClient.java`** — Stub with SLF4J logging. Replace with real `WebClient` call when insurance backend is available.
- [ ] **`NotificationApiClient.java`** — Stub with SLF4J logging. Replace with real notification provider (email/SMS gateway) when available.
- [x] **Insurance service** — Full implementation: `InsurancePolicyEntity`, `JpaInsurancePolicyRepository`, `InsurancePolicyRepositoryImpl`, `CreatePolicyUseCase`, `GetPolicyUseCase`, `GetPatientPoliciesUseCase`, `InsurancePolicyApplicationService`, `PatientCreatedEventConsumer` (Kafka), `KafkaConsumerConfig` with DLQ+backoff, `DuplicatePolicyException` → 409. H2-compatible migration under `db/migration/h2/`.

### Anti-Patterns
- [x] **`PatientEventProducer.java`** — `Thread.sleep()` removed. All three methods now do real Kafka sends.
- [x] **`AuthenticationController.java`** — Rethrows `BadCredentialsException` directly; GlobalExceptionHandler maps it to 401.
- [x] **Inline event DTOs in consumer classes** — `LabTestOrderedEvent` extracted to `infrastructure/messaging/event/LabTestOrderedEvent.java`. Consumer now imports it.
- [x] **Magic strings for event types** — `EventTopics.java` created in both patient-service and lab-service. All publishers and consumers use the constants.
- [ ] **Generic catch blocks** — Replace remaining broad `catch (Exception e)` blocks with specific types as domain exceptions are filled in.

### Exception Handling
- [x] Create custom exception hierarchy — Added `DuplicatePatientException`, `EventPublishException` (patient-service); `LabServiceException`, `LabOrderNotFoundException`, `LabResultNotFoundException` (lab-service); `InsuranceServiceException`, `PolicyNotFoundException` (insurance-service).
- [x] **Global exception handler** — Stack trace logging, `BadCredentialsException` → 401, `MethodArgumentNotValidException` → 400, `DuplicatePatientException` → 409. Lab-service and insurance-service now both have a `GlobalExceptionHandler`.
- [x] Silent failures in Kafka event publisher — **Transactional Outbox** implemented in lab-service: `OutboxEvent` entity, `JpaOutboxEventRepository`, `OutboxEventRepositoryImpl`, `OutboxRelay` (`@Scheduled`, polls PENDING, publishes, marks PROCESSED/FAILED with retries). `LabOrderApplicationService` now writes to outbox instead of directly calling Kafka. `@EnableScheduling` added to `LabrotaryServiceApplication`. `V3__create_outbox_table.sql` migration added.

### Kafka & Messaging
- [x] Configure **Dead Letter Queues (DLQ)** — `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` added to `KafkaConsumerConfig`. Failed messages go to `<topic>.DLT`.
- [x] Add **retry logic with exponential backoff** — `ExponentialBackOff` configured: 1s initial, 2x multiplier, max 5 attempts.
- [ ] Integrate **Confluent Schema Registry** — still manual JSON; schema validation not enforced.
- [ ] Implement an **event versioning strategy** — `EventTopics` constants use `v1` suffix; consumers still must handle version negotiation explicitly.

### API & Endpoints
- [x] Add **OpenAPI/Swagger documentation** — `springdoc-openapi-starter-webmvc-ui` added to all three services. `OpenApiConfig` created in each. Swagger UI at `/swagger-ui.html`. Patient-service SecurityConfig permits Swagger paths.
- [ ] Add **pagination** (`Pageable`) to all list endpoints that could return unbounded results.
- [x] Create standardized **error response DTOs** — `ErrorResponse` record created in all three services. All `GlobalExceptionHandler` methods now return `ResponseEntity<ErrorResponse>` instead of `ResponseEntity<Object>` with `Map<String,Object>`.
- [ ] Define and document an **API versioning migration strategy** — all endpoints use `/api/v1/` but there is no plan for v2.

### Build & Dependencies
- [x] **Unify Spring Boot versions** — all three services now use `3.2.4`.
- [x] **Unify Java versions** — patient-service bumped from 17 → 21. All services now on Java 21.
- [x] **Upgrade JJWT** from `0.11.5` to `0.12.6` — `JwtUtil.java` updated to new API (`Jwts.parser()`, `parseSignedClaims()`, `getPayload()`, fluent builder methods).
- [x] Add **Maven dependency vulnerability scanning** — `org.owasp:dependency-check-maven:9.0.9` added to all three `pom.xml` files. Fails build on CVSS ≥ 7.
- [ ] Extract shared event DTOs, common utilities, and constants into a dedicated **`common` or `shared` Maven module** to avoid duplication.

---

## MEDIUM PRIORITY — Should Complete

### Security (Continued)
- [x] Configure **CORS** — `CorsConfigurationSource` bean added to patient-service `SecurityConfig`. Origins configurable via `CORS_ALLOWED_ORIGINS` env var; defaults to `localhost:3000` and `localhost:8080` for dev.
- [ ] Implement **API rate limiting** (e.g. using a Bucket4j + Redis integration or Kong's rate-limit plugin which is already in the stack).
- [ ] Configure **request timeouts** on all outbound `RestTemplate` or `WebClient` calls to prevent cascading failures.
- [ ] Implement **circuit breaker** pattern (Resilience4j) for calls to external services (insurance, notification APIs).
- [ ] **SSN and other PII** are noted as "encrypted in application layer" in schema comments, but no encryption is implemented. Implement field-level encryption (e.g. AES-256) for SSN and other sensitive fields before storing.
- [ ] Add **CSRF protection** or explicitly document why it is disabled (stateless JWT API is the usual reason — add a comment).

### Testing
- [ ] Write **Testcontainers-based integration tests** for all services — spin up real PostgreSQL, Redis, and Kafka instances for testing.
- [ ] Add **Kafka consumer/producer integration tests** to verify event publishing and consumption end-to-end.
- [ ] Add **controller slice tests** with `@WebMvcTest` for all controllers.
- [ ] Define minimum **code coverage thresholds** in the Maven build (e.g. 80% via JaCoCo) and fail the build if not met.

### Observability
- [x] **Prometheus alert rules** — Emitting `meditrack_patient_registrations_total` (PatientCommandService) and `meditrack_lab_orders_created_total` (LabOrderApplicationService) via Micrometer `Counter`. Also added `meditrack_patient_updates_total`.
- [ ] Add **Prometheus exporters** for Kafka, PostgreSQL, and Redis in `prometheus.yml` (currently commented out).
- [ ] Emit custom **business metrics** via Micrometer (lab order creation rate, patient registration count, insurance verification latency, etc.).
- [ ] Verify **Jaeger/Zipkin compatibility** — docker-compose exposes port 9411 (Zipkin). Ensure the Spring Sleuth/Micrometer Tracing config points to the correct endpoint format.
- [ ] Add a **request/response logging interceptor** that logs method, path, status code, and duration for every HTTP request (without logging PHI).

### Database
- [ ] **Patient Service V1 migration is dropped entirely by V2** (via `DROP TABLE IF EXISTS`). Remove V1 or make V2 a proper incremental migration.
- [ ] Document and implement a **data retention policy** — healthcare data has regulatory retention requirements.
- [ ] Review and add **missing CASCADE DELETE relationships** in the lab schema where child records should be removed with parent records.
- [ ] Enforce **row-level security** in PostgreSQL for multi-tenant scenarios if applicable.

### Caching
- [ ] **Redis is configured but not used** — implement caching for frequently read, infrequently changed data (e.g. patient demographics, insurance plan details).
- [ ] Define a **cache eviction strategy** and TTLs appropriate for each data type.

### Authentication & Authorization
- [ ] **Keycloak is in the stack but not integrated** — either complete Keycloak OIDC integration or clearly document the chosen auth approach. The current JWT filter and Keycloak both handling auth is inconsistent.
- [x] Implement **role-based access control (RBAC)** — `Role` enum (ROLE_ADMIN/DOCTOR/NURSE/LAB_TECH), `UserDetailsServiceImpl` assigns roles, `JwtUtil` embeds `roles` claim in JWT, `JwtRequestFilter` extracts authorities from JWT claim, `SecurityConfig` has per-method HTTP-verb rules + `@EnableMethodSecurity`.
- [ ] Add integration tests for authentication flows.

### Search
- [ ] **`PatientSearchController` exists but implementation is likely incomplete** — verify and implement patient search by name, DOB, MRN, phone, etc.
- [ ] Evaluate whether **Elasticsearch** (which is already in the stack) should back patient search. If not, remove it from the stack to reduce resource usage.

---

## LOW PRIORITY — Nice to Have

### Deployment & Infrastructure
- [ ] Add **Kubernetes manifests** (Deployments, Services, ConfigMaps, Secrets, HPA) for production deployment.
- [ ] Set **memory and CPU limits** on all containers in `docker-compose.yml` to prevent resource starvation.
- [ ] Add a **documented backup strategy** for PostgreSQL data volumes.
- [ ] Make **Kafka and Zookeeper high-availability** — add a second broker and ZooKeeper node in docker-compose for realistic local testing.
- [ ] **Kong API Gateway** is in the stack but services are not routed through it. Either wire services through Kong (for rate limiting, auth, routing) or remove it.
- [ ] Configure **Elasticsearch log shipping** — it is in the docker-compose stack but no log shipper (Filebeat/Logstash) is configured.

### API & Documentation
- [ ] Create an **API documentation portal** (e.g. Redoc or Swagger UI) accessible at a well-known URL.
- [ ] Add **`@Operation` and `@ApiResponse` annotations** (OpenAPI) to all controller methods.
- [ ] Implement **bulk operation endpoints** for scenarios like batch lab result uploads.
- [ ] Add a **data export endpoint** for patient records (FHIR format would be ideal for healthcare interoperability).

### Code Quality
- [ ] Create **Architectural Decision Records (ADRs)** in a `/docs/adr/` directory documenting why hexagonal architecture, Kafka, PostgreSQL, etc. were chosen.
- [ ] Add a **`.env.example`** file at the repository root showing all required environment variables without real values.
- [ ] Add a **`CONTRIBUTING.md`** with local setup instructions, branching strategy, and PR guidelines.
- [ ] Enforce **consistent code style** with a shared Checkstyle or SpotBugs configuration across all services.
- [ ] Add **pre-commit hooks** (e.g. with Husky or a simple git hook) to run linting and build verification before commits.
- [ ] Consider **GraphQL** as an alternative or supplement to REST for flexible client queries (e.g. patient portal front-end).
- [ ] Add **performance benchmarks** (JMH) for critical hot paths (patient lookup, lab result retrieval).
- [ ] Implement **feature flags** for safely rolling out new behavior in production.

### Developer Experience
- [ ] Add a top-level **`Makefile`** or shell script with common commands (`make build`, `make test`, `make start`, `make stop`) to simplify local development.
- [ ] Add **Docker health-check dependencies** (`depends_on: condition: service_healthy`) in docker-compose so services only start after their dependencies are ready.
- [ ] Create a **Postman/Bruno collection** for manual API testing and add it to the repository.

---

## Summary Table

| Category | Critical | High | Medium | Low |
|---|---|---|---|---|
| Security | 4 | 5 | 6 | 0 |
| Testing | 3 | 4 | 4 | 0 |
| Code Quality | 5 | 6 | 2 | 5 |
| Messaging / Kafka | 0 | 4 | 2 | 0 |
| API / Endpoints | 1 | 4 | 3 | 3 |
| Observability | 0 | 0 | 5 | 2 |
| Database | 0 | 0 | 4 | 1 |
| Infrastructure | 0 | 0 | 0 | 7 |
| Dependencies | 0 | 4 | 0 | 0 |

**Total items: ~80+**
