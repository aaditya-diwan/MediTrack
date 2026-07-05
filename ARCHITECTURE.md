# MediTrack — System Architecture

> A HIPAA-minded, event-driven hospital management platform built as a Spring Boot
> microservice fleet behind a Next.js BFF, with a full observability + identity stack.
>
> **Stack:** Java 21 · Spring Boot 3.2.4 · PostgreSQL 15 · Redis 7 · Kafka 7.5 (Confluent) ·
> Next.js 16 / React 19 · Kong · Keycloak · Prometheus · Grafana · Jaeger · Elasticsearch + Kibana.
>
> All diagrams below are [Mermaid](https://mermaid.js.org/) and render natively on GitHub.

---

## 1. High-Level System Architecture (C4 Container view)

```mermaid
flowchart TB
    user(["👩‍⚕️ Clinician / Staff<br/>Web Browser"])

    %% ---------------- Frontend / BFF ----------------
    subgraph FE["🖥️ Frontend Tier — meditrack-ui (Next.js 16 · React 19)"]
        direction TB
        MW["Edge Middleware<br/>(proxy.ts)<br/>cookie auth-guard → /login"]
        PAGES["App Router Pages<br/>patients · doctors · appointments<br/>prescriptions · lab · insurance"]
        BFF["BFF API Routes (/app/api/*)<br/>per-domain reverse proxy<br/>injects Bearer JWT from httpOnly cookie"]
        MW --> PAGES --> BFF
    end

    %% ---------------- Edge (provisioned) ----------------
    subgraph EDGE["🚪 Edge / IAM (provisioned in compose)"]
        direction TB
        KONG["Kong API Gateway<br/>:8000 proxy · :8001 admin<br/>(+ kong-database)"]
        KC["Keycloak IAM<br/>:8180 · OIDC realm"]
    end

    %% ---------------- Microservices ----------------
    subgraph SVC["⚙️ Microservice Fleet — Spring Boot · Hexagonal · JWT-secured"]
        direction LR
        PAT["patient-service :8081<br/>👤 patients + medical records<br/>🔑 issues JWT (auth provider)"]
        LAB["lab-service :8082<br/>🧪 lab orders & results<br/>(transactional outbox)"]
        INS["insurance-service :8083<br/>🛡️ policies & claims"]
        DOC["doctor-service :8084<br/>🩺 doctors & availability"]
        APP["appointment-service :8085<br/>📅 booking & scheduling"]
        RX["prescription-service :8086<br/>💊 prescriptions"]
    end

    %% ---------------- Messaging ----------------
    subgraph MSG["📨 Event Backbone"]
        direction TB
        ZK["Zookeeper :2181"]
        KAFKA["Apache Kafka<br/>:9092 host · :29092 internal"]
        SR["Schema Registry<br/>:8081 internal · :18081 host"]
        ZK -.-> KAFKA
        KAFKA --- SR
    end

    %% ---------------- Data ----------------
    subgraph DATA["🗄️ State Stores"]
        direction TB
        PG[("PostgreSQL :5432<br/>patient_db · lab_db · insurance_db<br/>doctor_db · appointment_db · prescription_db<br/>keycloak · kong")]
        REDIS[("Redis :6379<br/>cache / TTL")]
    end

    %% ---------------- Observability ----------------
    subgraph OBS["📊 Observability"]
        direction TB
        PROM["Prometheus :9090<br/>scrapes /actuator/prometheus"]
        GRAF["Grafana :3000<br/>dashboards + alerts"]
        JAEGER["Jaeger :16686<br/>traces (Zipkin :9411)"]
        ES[("Elasticsearch :9200")]
        KIB["Kibana :5601<br/>audit-log search"]
        PROM --> GRAF
        ES --> KIB
    end

    %% ---------------- Edges ----------------
    user -->|HTTPS| FE
    BFF -->|"login → /api/v1/auth/authenticate"| PAT
    BFF -->|"REST + Bearer JWT (direct, per-service URL)"| SVC
    EDGE -.->|"optional managed edge path"| SVC
    KC -. "token issuer (provisioned)" .-> EDGE

    SVC -->|JDBC| PG
    SVC -->|cache| REDIS
    SVC -->|"produce / consume events"| KAFKA

    SVC -->|"metrics (Micrometer)"| PROM
    KONG -->|metrics| PROM
    SVC -->|"spans (Brave → Zipkin)"| JAEGER
    SVC -.->|"audit logs"| ES
    KC --> PG
    KONG --> PG

    classDef fe fill:#1e3a5f,stroke:#4f93ce,color:#fff;
    classDef svc fill:#143d2b,stroke:#3fa66a,color:#fff;
    classDef data fill:#4a2f10,stroke:#d08a30,color:#fff;
    classDef msg fill:#3d1f47,stroke:#a45fb8,color:#fff;
    classDef obs fill:#3d1414,stroke:#cc5151,color:#fff;
    classDef edge fill:#13343b,stroke:#3fb4c4,color:#fff;
    class MW,PAGES,BFF fe;
    class PAT,LAB,INS,DOC,APP,RX svc;
    class PG,REDIS data;
    class ZK,KAFKA,SR msg;
    class PROM,GRAF,JAEGER,ES,KIB obs;
    class KONG,KC edge;
```

> **Note on the edge tier:** Kong + Keycloak are fully provisioned in `docker-compose.yml`, but
> the current `meditrack-ui` BFF calls each service **directly** via per-service env URLs
> (`DOCTOR_SERVICE_URL`, `PATIENT_SERVICE_URL`, …) and uses **patient-service** as the JWT issuer
> (`POST /api/v1/auth/authenticate`). The gateway/IdP path is the intended production edge and is
> drawn with dashed lines to reflect "provisioned, not yet on the hot path."

---

## 2. Authentication & Request Flow

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant MW as Next.js Middleware
    participant BFF as BFF API Route
    participant PAT as patient-service (auth)
    participant SVC as Target Service

    B->>MW: GET /patients
    alt no "token" cookie
        MW-->>B: 302 redirect → /login
        B->>BFF: POST /api/auth/login {username,password}
        BFF->>PAT: POST /api/v1/auth/authenticate
        PAT-->>BFF: { jwt }
        BFF-->>B: Set-Cookie token=JWT (httpOnly, sameSite=lax, 24h)
    end
    B->>BFF: GET /api/doctors/... (cookie sent)
    BFF->>BFF: read token cookie
    BFF->>SVC: GET /api/v1/... + Authorization: Bearer JWT
    SVC->>SVC: Spring Security filter validates JWT (jjwt, HS256)
    SVC-->>BFF: 200 JSON
    BFF-->>B: 200 JSON
```

**Security properties**
- JWT secret shared across services via `JWT_SECRET` env (HS256, 24h expiry).
- Token stored **httpOnly** — not reachable by browser JS (XSS-resistant).
- Each Spring service runs a stateless `SecurityFilterChain` (`SessionCreationPolicy.STATELESS`, CSRF disabled).
- `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**` are public for ops & docs.

---

## 3. Event-Driven Choreography (Kafka topics)

Services are loosely coupled through domain events. Producers never call consumers directly.

```mermaid
flowchart LR
    PAT["patient-service"]
    LAB["lab-service"]
    INS["insurance-service"]
    DOC["doctor-service"]
    APP["appointment-service"]
    RX["prescription-service"]

    %% patient lifecycle
    PAT -- "patient.created.v1" --> INS
    PAT -- "patient.updated.v1 / patient.deleted.v1" --> KBUS{{Kafka}}
    PAT -- "patient-events (lab test ordered)" --> LAB

    %% lab
    LAB -- "lab.test.ordered.v1" --> KBUS
    LAB -- "lab.results.available.v1" --> KBUS
    LAB -- "lab.critical.result.v1" --> KBUS
    LAB -. "outbox relay → exactly-once publish" .- OUTBOX[(outbox table)]

    %% doctor / appointment / prescription
    DOC -- "doctor.created.v1" --> KBUS
    APP -- "appointment.booked.v1" --> KBUS
    APP -- "appointment.completed.v1" --> KBUS
    RX -- "prescription.issued.v1" --> KBUS
    RX -- "prescription.sent_to_pharmacy.v1" --> KBUS
    RX -- "prescription.sent_to_lab.v1" --> KBUS

    classDef p fill:#143d2b,stroke:#3fa66a,color:#fff;
    class PAT,LAB,INS,DOC,APP,RX p;
```

### Topic catalog

| Topic | Producer | Consumer(s) | Pattern |
|---|---|---|---|
| `patient.created.v1` | patient-service | insurance-service | direct publish |
| `patient.updated.v1` | patient-service | — | direct publish |
| `patient.deleted.v1` | patient-service | — | direct publish |
| `patient-events` (lab test ordered) | patient-service | lab-service | direct publish |
| `lab.test.ordered.v1` | lab-service | — | **transactional outbox** (`OutboxRelay`) |
| `lab.results.available.v1` | lab-service | — | event publisher |
| `lab.critical.result.v1` | lab-service | — | event publisher |
| `doctor.created.v1` | doctor-service | — | **post-commit async** (`@TransactionalEventListener` AFTER_COMMIT, non-fatal) |
| `appointment.booked.v1` | appointment-service | — | direct publish |
| `appointment.completed.v1` | appointment-service | — | direct publish |
| `prescription.issued.v1` | prescription-service | — | direct publish |
| `prescription.sent_to_pharmacy.v1` | prescription-service | — | direct publish |
| `prescription.sent_to_lab.v1` | prescription-service | — | direct publish |
| `prescription.safety.flagged.v1` | **ai-service** | (pharmacy · notification · audit) | best-effort produce (non-fatal) |

> **Reliability spectrum (strongest → weakest):**
> - `lab-service` — **transactional outbox**: events written to a DB outbox table inside the business
>   transaction and relayed to Kafka by a separate poller (`OutboxRelay`). At-least-once, no dual-write loss.
> - `doctor-service` — **post-commit async publish**: the use case emits a Spring `ApplicationEvent`;
>   a `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` handler sends to Kafka with a low
>   `max.block.ms` and swallows delivery errors. A broker outage **no longer blocks or rolls back**
>   doctor creation (graceful degradation, at-most-once). *(Hardened — see fixes below.)*
> - `patient` / `appointment` / `prescription` — still publish **inline** via `KafkaTemplate.send(...)`
>   inside their `@Transactional` methods, so a broker outage will fail (and roll back) the originating
>   request. **Recommended next step:** roll out the doctor-service pattern (or the outbox) to these three.

---

## 4. Per-Service Internal Architecture (Hexagonal / Clean)

Every service shares the same layered, ports-and-adapters layout. `doctor-service` shown as the template.

```mermaid
flowchart TB
    subgraph INTERFACES["interfaces — inbound adapters"]
        CTRL["REST Controller<br/>@RestController /api/v1/..."]
        DTO["Request / Response DTOs<br/>+ Bean Validation"]
        EXH["GlobalExceptionHandler<br/>@RestControllerAdvice"]
    end

    subgraph APPLICATION["application — use cases"]
        UC["Use-Case Interfaces<br/>Create / Get / List / ..."]
        APPSVC["ApplicationService<br/>orchestration + @Transactional"]
        EXC["Domain exceptions"]
    end

    subgraph DOMAIN["domain — core (no framework deps)"]
        MODEL["Entities / Aggregates<br/>(Doctor, AvailabilitySlot)"]
        REPOIF["Repository Ports (interfaces)"]
    end

    subgraph INFRA["infrastructure — outbound adapters"]
        JPA["JPA Entities + Spring Data repos"]
        MAPPER["Persistence Mappers"]
        REPOIMPL["Repository Impl (adapters)"]
        KPROD["Kafka Event Producer"]
        SEC["SecurityConfig (JWT filter)"]
        OAPI["OpenAPI / Swagger config"]
    end

    DB[("PostgreSQL")]
    K{{Kafka}}

    CTRL --> UC
    DTO -.-> CTRL
    CTRL --> EXH
    UC --> APPSVC
    APPSVC --> MODEL
    APPSVC --> REPOIF
    REPOIF -. implemented by .-> REPOIMPL
    REPOIMPL --> JPA --> DB
    REPOIMPL --> MAPPER
    APPSVC -- "publishEvent (in-txn)" --> EVT["Spring ApplicationEvent"]
    EVT -- "@TransactionalEventListener<br/>AFTER_COMMIT · @Async" --> KPROD
    KPROD -- "non-blocking · non-fatal" --> K
    SEC -.-> CTRL

    classDef i fill:#1e3a5f,stroke:#4f93ce,color:#fff;
    classDef a fill:#143d2b,stroke:#3fa66a,color:#fff;
    classDef d fill:#4a2f10,stroke:#d08a30,color:#fff;
    classDef inf fill:#3d1f47,stroke:#a45fb8,color:#fff;
    class CTRL,DTO,EXH i;
    class UC,APPSVC,EXC a;
    class MODEL,REPOIF d;
    class JPA,MAPPER,REPOIMPL,KPROD,SEC,OAPI inf;
```

**Cross-cutting per service:** Flyway DB migrations · Spring Boot Actuator · Micrometer +
Brave tracing → Jaeger · Prometheus registry · Redis cache · springdoc OpenAPI UI.

---

## 5. Deployment Topology (docker-compose)

```mermaid
flowchart TB
    subgraph NET["🐳 docker network: meditrack-network (bridge)"]
        direction TB
        subgraph APPS["Application containers"]
            UI2["meditrack-ui*"]
            PAT2["patient-service :8081"]
            LAB2["lab-service :8082"]
            INS2["insurance-service :8083"]
            DOC2["doctor-service :8084"]
            APP2["appointment-service :8085"]
            RX2["prescription-service :8086"]
        end
        subgraph INFRA2["Platform containers"]
            KAFKA2["kafka + zookeeper + schema-registry"]
            PG2[("postgres :5432")]
            RED2[("redis :6379")]
            KONG2["kong + kong-db"]
            KC2["keycloak :8180"]
        end
        subgraph OBS2["Observability containers"]
            PROM2["prometheus :9090"]
            GRAF2["grafana :3000"]
            JG2["jaeger :16686"]
            ES2["elasticsearch :9200"]
            KIB2["kibana :5601"]
        end
    end

    APPS --> INFRA2
    APPS --> OBS2
```

\* `meditrack-ui` runs as a Next.js app (dev/standalone); the six services build from per-service
`Dockerfile`s and start with `SPRING_PROFILES_ACTIVE=docker` (Postgres + internal Kafka listener
`kafka:29092`). Named volumes persist Postgres, Redis, Kafka, Grafana, Prometheus, Elasticsearch.

---

## 6. Quick Reference

### Service ports & datastores
| Service | Port | Database | Role |
|---|---|---|---|
| meditrack-ui (Next.js BFF) | 3001 | — | UI + reverse proxy + auth cookie |
| patient-service | 8081 | `patient_db` | patients, medical records, **JWT auth issuer** |
| lab-service (labrotary) | 8082 | `lab_db` | lab orders/results, **outbox** |
| insurance-service | 8083 | `insurance_db` | policies, claims (consumes patient events) |
| doctor-service | 8084 | `doctor_db` | doctors, availability slots |
| appointment-service | 8085 | `appointment_db` | appointment booking |
| prescription-service | 8086 | `prescription_db` | prescriptions |
| ai-service | 8089 | — (stateless) | clinical decision support via TensorX open-weight inference |

The UI now runs on `:3001` (`next dev -p 3001`) so it no longer collides with Grafana's host `:3000`.

### Platform & ops endpoints
| Component | Port(s) | Purpose |
|---|---|---|
| Kong | 8000 / 8443 proxy · 8001 / 8444 admin | API gateway (provisioned) |
| Keycloak | 8180 | OIDC identity provider (provisioned) |
| Kafka | 9092 (host) · 29092 (internal) | event backbone |
| Zookeeper | 2181 | Kafka coordination |
| Schema Registry | 18081 host · 8081 internal | Avro/JSON schema governance (remapped off patient-service's 8081) |
| PostgreSQL | 5432 | primary state store (per-service DBs) |
| Redis | 6379 | cache |
| Prometheus | 9090 | metrics scraping (`/actuator/prometheus`) |
| Grafana | 3000 | dashboards + alert rules |
| Jaeger | 16686 (UI) · 9411 (Zipkin ingest) | distributed tracing |
| Elasticsearch / Kibana | 9200 / 5601 | audit-log storage & search |

### Architectural patterns in play
- **Microservices** with database-per-service isolation.
- **Hexagonal / Clean Architecture** (interfaces → application → domain → infrastructure).
- **Event-driven choreography** over Kafka with versioned topics (`*.v1`).
- **Transactional Outbox** (lab-service) for reliable publish.
- **Backend-for-Frontend (BFF)** in Next.js, with httpOnly-cookie JWT session.
- **Observability triad**: metrics (Prometheus/Grafana), tracing (Micrometer→Jaeger), logs (ELK).
- **API Gateway + IdP** (Kong + Keycloak) provisioned for a managed edge.
- **Graceful degradation** of event publishing (doctor-service): write path stays available when
  Kafka is down.
- **AI clinical decision support** (ai-service): open-weight LLM inference behind a domain port,
  screening prescriptions for drug interactions and allergy conflicts.

---

## 6b. AI / Clinical Decision Support (ai-service :8089)

The first AI capability is a **prescription safety screen**: given a proposed prescription plus the
patient's current medications and documented allergies, `ai-service` returns a structured assessment
of **drug–drug interactions** and **allergy conflicts** with per-finding severity
(`NONE → MINOR → MODERATE → MAJOR → CONTRAINDICATED`) and a recommended action.

```mermaid
flowchart LR
    UI["meditrack-ui / clinician"] -->|"POST /api/v1/ai/prescription-safety (Bearer JWT)"| AISVC
    subgraph AISVC["ai-service (hexagonal, stateless)"]
      direction TB
      C["Controller"] --> UC["CheckPrescriptionSafety use case"]
      UC --> PORT["ClinicalReasoningPort"]
      PORT -. implemented by .-> ADP["TensorX adapter"]
    end
    ADP -->|"OpenAI-compatible /chat/completions"| TX["TensorX<br/>open-weight model<br/>(EU-sovereign · zero retention)"]
    UC -->|"if MAJOR/CONTRAINDICATED or allergy conflict"| K{{Kafka: prescription.safety.flagged.v1}}
    classDef p fill:#143d2b,stroke:#3fa66a,color:#fff;
    class C,UC,PORT,ADP p;
```

**Design choices**
- **Provider behind a port.** `ClinicalReasoningPort` is the domain abstraction; `TensorXClinicalReasoningAdapter`
  is one implementation. Swapping inference vendors (or dropping in a rules engine) never touches the domain.
- **TensorX for PHI-grade inference.** OpenAI-compatible API, **EU-sovereign hosting, zero data retention** —
  a defensible posture for clinical data versus a US LLM SaaS. Model is env-configurable
  (`TENSORX_MODEL`, default `deepseek/deepseek-chat-v3.1`); temperature pinned low (0.1) for repeatable, conservative output.
- **Stateless — no PHI at rest.** The service persists nothing; the request carries the full clinical
  picture and the response is returned and (if flagged) announced on the backbone.
- **Fail safe, not silent.** Missing key / upstream failure / unparseable reply → **502**, never a false
  all-clear. Unknown severity labels default to `MODERATE`; every response is stamped with an explicit
  *advisory-only, verify-with-a-pharmacist* disclaimer.

> **Roadmap (same service + client):** lab-result explainer, SOAP-note generation, and patient-history
> summary are follow-on use cases on the same `ClinicalReasoningPort`.

---

## 7. Fixes Applied (this pass)

Issues discovered while running and testing `doctor-service`, and the remediations made:

| # | Issue discovered | Severity | Fix |
|---|---|---|---|
| 1 | Entity↔schema column mismatch — `DoctorEntity.active`/`AvailabilitySlotEntity.available` mapped to `ACTIVE`/`AVAILABLE` but Flyway defines `is_active`/`is_available`. **Every read query threw 500.** | 🔴 Critical | Added `@Column(name = "is_active")` / `@Column(name = "is_available")`. |
| 2 | `createDoctor` published to Kafka **inline inside `@Transactional`**. With the broker down it **blocked 60s, returned 500, and rolled back** — creation was impossible without Kafka. | 🔴 Critical | Publish via Spring `ApplicationEvent` → `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`, non-fatal callback, `max.block.ms=5000`. Create now returns **201 in ~0.4s** with Kafka down. |
| 3 | Bean-validation failures returned **500** instead of 400 (no `MethodArgumentNotValidException` handler). | 🟠 High | Added handlers for `MethodArgumentNotValidException` (400 + field errors) and `HttpMessageNotReadableException` (400 for bad JSON / invalid enum). |
| 4 | Host port collision: `patient-service` and `schema-registry` both bound host `:8081`. | 🟡 Medium | Remapped schema-registry to host `:18081` (internal stays `:8081`). |
| 5 | Host port collision: UI dev server (`:3000`) vs Grafana (`:3000`). | 🟡 Medium | UI scripts pinned to `:3001`; Grafana keeps the conventional `:3000`. |

**Verified after fixes (Kafka intentionally down):** list/get/filter `200`, create `201` (~0.4s, persisted),
duplicate `409`, not-found `404`, set-schedule + get-slots `200`, missing-field & bad-enum `400`.

**Not yet addressed (recommended):** apply fix #2's pattern (or the outbox) to `patient`,
`appointment`, and `prescription` services, which still publish inline inside their transactions.

---
*Generated from the live repository surface: `docker-compose.yml`, per-service Spring code,
`monitoring/`, and `meditrack-ui/`. Dashed lines = provisioned-but-not-yet-on-hot-path.*
