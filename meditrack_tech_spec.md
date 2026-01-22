# MediTrack - Technical Specification Document

## Executive Summary

MediTrack is an event-driven microservices platform that solves healthcare data fragmentation by creating a real-time data exchange hub. The system enables seamless interoperability between hospitals, laboratories, insurance providers, and pharmacies through Apache Kafka-powered event streaming.

## 1. System Architecture Overview

### 1.1 Architectural Patterns
- **Event-Driven Architecture**: All inter-service communication via Kafka events
- **Hexagonal Architecture**: Clean separation of business logic from external concerns
- **Domain-Driven Design**: Service boundaries aligned with healthcare business domains
- **CQRS Pattern**: Separate read/write models for optimal performance
- **Event Sourcing**: Complete audit trail for regulatory compliance

### 1.2 Technology Stack Matrix

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Application** | Spring Boot | 3.2 | Microservices framework |
| **Event Streaming** | Apache Kafka | 3.6 | Event backbone |
| **Schema Management** | Confluent Schema Registry | 7.5 | Event schema evolution |
| **Database** | PostgreSQL | 15 | Transactional data |
| **Caching** | Redis | 7.2 | Session & query cache |
| **Authentication** | Keycloak | 23 | Identity & access management |
| **API Gateway** | Kong | 3.4 | Centralized API management |
| **Container** | Docker | 24 | Application containerization |
| **Orchestration** | Kubernetes | 1.28 | Container orchestration |
| **Monitoring** | Prometheus/Grafana | Latest | Metrics & visualization |
| **Tracing** | Jaeger | 1.50 | Distributed tracing |

## 2. Microservices Architecture

### 2.1 Service Boundaries & Responsibilities

#### 2.1.1 Patient Service
```yaml
Domain: Patient Management
Responsibility: Patient demographics, medical records, care coordination
Port: 8081
Database: PostgreSQL (patient_db)
Cache: Redis (patient_cache)
Events Published:
  - patient.created.v1
  - patient.updated.v1
  - patient.discharged.v1
  - lab.test.ordered.v1
Events Consumed:
  - lab.results.available.v1
  - prescription.filled.v1
```

**Core Entities:**
- Patient (Aggregate Root)
- MedicalRecord
- ContactInformation
- InsuranceDetails

**API Endpoints:**
```
POST   /api/v1/patients                      - Create patient
GET    /api/v1/patients/{id}                 - Get patient details
PUT    /api/v1/patients/{id}                 - Update patient
DELETE /api/v1/patients/{id}                 - Soft delete patient
GET    /api/v1/patients/{id}/timeline        - Healthcare timeline
GET    /api/v1/patients/search               - Search patients
POST   /api/v1/patients/{ssn}/order-labs     - Order lab tests
GET    /api/v1/patients/{id}/medical-records - Get medical records
POST   /api/v1/patients/{id}/medical-records - Create medical record
```

**Value Objects:**
- MRN (Medical Record Number)
- SSN (Social Security Number)
- DateOfBirth
- Address

#### 2.1.2 Laboratory Service
```yaml
Domain: Laboratory Operations
Responsibility: Test orders, results processing, equipment integration
Port: 8082
Database: PostgreSQL (lab_db)
Cache: Redis (lab_cache)
Events Published:
  - lab.test.ordered.v1
  - lab.results.available.v1
  - lab.test.cancelled.v1
  - lab.critical.result.v1
Events Consumed:
  - patient.created.v1
  - insurance.preauth.approved.v1
```

**Core Entities:**
- LabOrder (Aggregate Root)
- TestResult
- LabEquipment
- ReferenceRange
- Specimen

**API Endpoints:**
```
POST   /api/v1/lab/orders              - Create lab order
GET    /api/v1/lab/orders/{id}         - Get order details
PUT    /api/v1/lab/orders/{id}/status  - Update order status
POST   /api/v1/lab/results             - Submit test results
GET    /api/v1/lab/results/{orderId}   - Get test results
POST   /api/v1/lab/orders/{id}/cancel  - Cancel order
GET    /api/v1/lab/orders/pending      - Get pending orders
```

**Integration Points:**
- HL7 MLLP for lab equipment
- FHIR R4 for external systems
- LIS (Laboratory Information System) adapters

#### 2.1.3 Insurance Service
```yaml
Domain: Insurance & Claims
Responsibility: Eligibility verification, claims processing, pre-authorization
Port: 8083
Database: PostgreSQL (insurance_db)
Cache: Redis (insurance_cache)
Events Published:
  - insurance.eligibility.verified.v1
  - insurance.claim.processed.v1
  - insurance.preauth.approved.v1
  - insurance.preauth.denied.v1
Events Consumed:
  - patient.created.v1
  - lab.test.ordered.v1
  - prescription.created.v1
```

**Core Entities:**
- InsurancePolicy (Aggregate Root)
- Claim
- PreAuthorization
- Copayment
- Payer

**API Endpoints:**
```
POST   /api/v1/insurance/verify              - Verify eligibility
POST   /api/v1/insurance/preauth             - Request pre-authorization
GET    /api/v1/insurance/preauth/{id}        - Check preauth status
POST   /api/v1/insurance/claims              - Submit claim
GET    /api/v1/insurance/claims/{id}         - Get claim status
PUT    /api/v1/insurance/claims/{id}         - Update claim
GET    /api/v1/insurance/policies/{patientId} - Get patient policies
```

**External Integrations:**
- X12 EDI for payer communication
- Real-time eligibility APIs (270/271 transactions)
- Claims clearinghouses (837 transactions)

#### 2.1.4 Pharmacy Service
```yaml
Domain: Prescription Management
Responsibility: Prescription processing, drug interactions, inventory
Port: 8084
Database: PostgreSQL (pharmacy_db)
Cache: Redis (pharmacy_cache)
Events Published:
  - prescription.received.v1
  - prescription.filled.v1
  - medication.dispensed.v1
  - drug.interaction.detected.v1
Events Consumed:
  - patient.created.v1
  - insurance.preauth.approved.v1
```

**Core Entities:**
- Prescription (Aggregate Root)
- Medication
- DrugInteraction
- Inventory
- Dispense

**API Endpoints:**
```
POST   /api/v1/pharmacy/prescriptions        - Create prescription
GET    /api/v1/pharmacy/prescriptions/{id}   - Get prescription
PUT    /api/v1/pharmacy/prescriptions/{id}/fill - Fill prescription
POST   /api/v1/pharmacy/interactions/check   - Check drug interactions
GET    /api/v1/pharmacy/inventory            - Check inventory
PUT    /api/v1/pharmacy/inventory/{drugId}   - Update inventory
GET    /api/v1/pharmacy/prescriptions/patient/{id} - Patient prescriptions
```

**External Integrations:**
- NCPDP SCRIPT for e-prescribing
- Drug database APIs (First Databank, Lexicomp)
- POS system integration

### 2.2 Cross-Cutting Services

#### 2.2.1 Notification Service
```yaml
Purpose: Real-time notifications and alerts
Port: 8085
Technologies: Spring Boot, WebSocket, FCM
Events Consumed: All domain events
Delivery Channels:
  - Push notifications (Mobile)
  - SMS via Twilio
  - Email via SendGrid
  - In-app notifications (WebSocket)
```

**Notification Types:**
- Critical lab results
- Appointment reminders
- Prescription ready notifications
- Insurance claim status updates

#### 2.2.2 Audit Service
```yaml
Purpose: HIPAA compliance and regulatory audit trails
Port: 8086
Technologies: Spring Boot, Elasticsearch
Events Consumed: All domain events + system events
Storage: Long-term archival in S3
Retention: 7 years (regulatory requirement)
```

**Audit Event Types:**
- Data access logs
- Authentication events
- Data modification events
- System configuration changes

#### 2.2.3 API Gateway Service
```yaml
Purpose: Single entry point, routing, authentication
Port: 8080
Technology: Kong Gateway
Features:
  - Rate limiting
  - JWT validation
  - Request/response transformation
  - API versioning
  - CORS handling
```

## 3. Event Streaming Architecture

### 3.1 Kafka Topic Design

| Topic Name | Partitions | Replication | Retention | Purpose |
|------------|------------|-------------|-----------|---------|
| `patient-events` | 12 | 3 | 30 days | Patient lifecycle events |
| `lab-events` | 8 | 3 | 90 days | Laboratory test events |
| `insurance-events` | 6 | 3 | 365 days | Insurance/claims events |
| `pharmacy-events` | 8 | 3 | 90 days | Prescription events |
| `audit-events` | 4 | 3 | 7 years | Compliance audit trail |
| `notification-events` | 4 | 3 | 7 days | Real-time notifications |
| `dlq-events` | 2 | 3 | 30 days | Dead letter queue |

### 3.2 Event Schema Evolution Strategy

**Avro Schema Example:**
```json
{
  "namespace": "com.meditrack.events",
  "type": "record",
  "name": "PatientCreatedEvent",
  "version": "1.0",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "eventType", "type": "string", "default": "patient.created.v1"},
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "source", "type": "string"},
    {"name": "patientId", "type": "string"},
    {"name": "demographics", "type": {
      "type": "record",
      "name": "Demographics",
      "fields": [
        {"name": "firstName", "type": "string"},
        {"name": "lastName", "type": "string"},
        {"name": "dateOfBirth", "type": "string"},
        {"name": "ssn", "type": ["null", "string"], "default": null},
        {"name": "mrn", "type": "string"},
        {"name": "gender", "type": ["null", "string"], "default": null},
        {"name": "phone", "type": ["null", "string"], "default": null},
        {"name": "email", "type": ["null", "string"], "default": null}
      ]
    }},
    {"name": "address", "type": ["null", {
      "type": "record",
      "name": "Address",
      "fields": [
        {"name": "street", "type": "string"},
        {"name": "city", "type": "string"},
        {"name": "state", "type": "string"},
        {"name": "zipCode", "type": "string"},
        {"name": "country", "type": "string", "default": "USA"}
      ]
    }], "default": null}
  ]
}
```

**Schema Evolution Rules:**
- Backward compatibility maintained for 2 major versions
- Field additions: Always optional with defaults
- Field deletions: Deprecated for 6 months before removal
- Type changes: Use union types for transitions
- Schema Registry validates all schema changes

### 3.3 Kafka Configuration

**Producer Configuration:**
```yaml
spring:
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
        compression.type: snappy
        schema.registry.url: http://localhost:8081
```

**Consumer Configuration:**
```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: localhost:9092
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        isolation.level: read_committed
        max.poll.records: 100
        schema.registry.url: http://localhost:8081
    listener:
      ack-mode: manual
```

### 3.4 Kafka Streams Processing

#### Real-time Patient Care Coordination
```java
@Configuration
public class CareCoordinationStreamConfig {

    @Bean
    public KStream<String, CareEvent> careCoordinationStream(
            StreamsBuilder streamsBuilder) {

        KStream<String, PatientEvent> patientEvents =
            streamsBuilder.stream("patient-events");

        KStream<String, LabEvent> labEvents =
            streamsBuilder.stream("lab-events");

        // Join patient events with lab events
        KStream<String, CareEvent> coordinatedEvents = patientEvents
            .join(labEvents,
                (patient, lab) -> createCareEvent(patient, lab),
                JoinWindows.of(Duration.ofHours(24)),
                StreamJoined.with(Serdes.String(), patientSerde, labSerde))
            .filter((key, event) -> event.requiresAction());

        coordinatedEvents.to("care-coordination-events");

        return coordinatedEvents;
    }
}
```

#### Insurance Pre-authorization Workflow
```java
@Configuration
public class PreAuthStreamConfig {

    @Bean
    public KStream<String, PreAuthResult> preAuthStream(
            StreamsBuilder streamsBuilder) {

        KStream<String, LabOrderEvent> labOrders =
            streamsBuilder.stream("lab-events");

        KTable<String, InsurancePolicy> policies =
            streamsBuilder.table("insurance-policies-topic");

        return labOrders
            .filter((key, order) -> order.requiresPreAuth())
            .selectKey((key, order) -> order.getPatientId())
            .join(policies,
                (order, policy) -> checkPreAuth(order, policy),
                Joined.with(Serdes.String(), orderSerde, policySerde))
            .mapValues(this::determineAuthorizationStatus)
            .peek((key, result) -> publishAuthResult(result));
    }
}
```

## 4. Data Architecture

### 4.1 Database Design Strategy

#### Patient Service Schema
```sql
-- Patient aggregate
CREATE TABLE patients (
    patient_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    mrn VARCHAR(20) UNIQUE NOT NULL,
    ssn VARCHAR(11) UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    CONSTRAINT chk_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN'))
);

-- Address value object
CREATE TABLE patient_addresses (
    address_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    address_type VARCHAR(20) NOT NULL,
    street_line1 VARCHAR(255) NOT NULL,
    street_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    country VARCHAR(50) DEFAULT 'USA',
    is_primary BOOLEAN DEFAULT FALSE,
    CONSTRAINT chk_address_type CHECK (address_type IN ('HOME', 'WORK', 'BILLING'))
);

-- Insurance details
CREATE TABLE patient_insurance (
    insurance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    policy_number VARCHAR(50) NOT NULL,
    payer_id VARCHAR(50) NOT NULL,
    payer_name VARCHAR(255) NOT NULL,
    group_number VARCHAR(50),
    policy_holder_name VARCHAR(255),
    relationship_to_insured VARCHAR(20),
    effective_date DATE NOT NULL,
    termination_date DATE,
    is_primary BOOLEAN DEFAULT FALSE,
    CONSTRAINT chk_relationship CHECK (relationship_to_insured IN ('SELF', 'SPOUSE', 'CHILD', 'OTHER'))
);

-- Medical records
CREATE TABLE medical_records (
    record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    record_type VARCHAR(50) NOT NULL,
    record_date TIMESTAMP WITH TIME ZONE NOT NULL,
    provider_id VARCHAR(50) NOT NULL,
    diagnosis_codes TEXT[],
    procedure_codes TEXT[],
    notes TEXT,
    attachments JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_record_type CHECK (record_type IN ('VISIT', 'LAB', 'IMAGING', 'PROCEDURE', 'MEDICATION'))
);

-- Indexes
CREATE INDEX idx_patients_mrn ON patients(mrn);
CREATE INDEX idx_patients_ssn ON patients(ssn);
CREATE INDEX idx_patients_tenant ON patients(tenant_id);
CREATE INDEX idx_addresses_patient ON patient_addresses(patient_id);
CREATE INDEX idx_insurance_patient ON patient_insurance(patient_id);
CREATE INDEX idx_medical_records_patient ON medical_records(patient_id);
CREATE INDEX idx_medical_records_date ON medical_records(record_date DESC);

-- Row Level Security
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON patients
    FOR ALL TO application_role
    USING (tenant_id = current_setting('app.current_tenant'));
```

#### Laboratory Service Schema
```sql
-- Lab order aggregate
CREATE TABLE lab_orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    mrn VARCHAR(20) NOT NULL,
    ordering_provider_id VARCHAR(50) NOT NULL,
    ordering_provider_name VARCHAR(255) NOT NULL,
    order_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    priority VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    specimen_collected_at TIMESTAMP WITH TIME ZONE,
    specimen_received_at TIMESTAMP WITH TIME ZONE,
    results_reported_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version INTEGER DEFAULT 0,
    CONSTRAINT chk_priority CHECK (priority IN ('STAT', 'URGENT', 'ROUTINE')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'COLLECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

-- Test information
CREATE TABLE lab_tests (
    test_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab_orders(order_id) ON DELETE CASCADE,
    test_code VARCHAR(50) NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    loinc_code VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    result_value VARCHAR(500),
    result_unit VARCHAR(50),
    reference_range VARCHAR(100),
    abnormal_flag VARCHAR(10),
    performed_at TIMESTAMP WITH TIME ZONE,
    performed_by VARCHAR(100),
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(100),
    CONSTRAINT chk_test_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_abnormal_flag CHECK (abnormal_flag IN ('L', 'H', 'LL', 'HH', 'N', 'A'))
);

-- Diagnosis codes
CREATE TABLE order_diagnoses (
    diagnosis_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab_orders(order_id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    code_system VARCHAR(20) NOT NULL DEFAULT 'ICD-10',
    description TEXT,
    is_primary BOOLEAN DEFAULT FALSE
);

-- Specimens
CREATE TABLE specimens (
    specimen_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab_orders(order_id) ON DELETE CASCADE,
    specimen_type VARCHAR(50) NOT NULL,
    collection_date TIMESTAMP WITH TIME ZONE,
    collected_by VARCHAR(100),
    received_date TIMESTAMP WITH TIME ZONE,
    condition VARCHAR(50),
    CONSTRAINT chk_specimen_type CHECK (specimen_type IN ('BLOOD', 'URINE', 'TISSUE', 'SWAB', 'OTHER'))
);

-- Indexes
CREATE INDEX idx_lab_orders_patient ON lab_orders(patient_id);
CREATE INDEX idx_lab_orders_mrn ON lab_orders(mrn);
CREATE INDEX idx_lab_orders_status ON lab_orders(status);
CREATE INDEX idx_lab_orders_date ON lab_orders(order_date DESC);
CREATE INDEX idx_lab_tests_order ON lab_tests(order_id);
CREATE INDEX idx_lab_tests_code ON lab_tests(test_code);
```

#### Insurance Service Schema
```sql
-- Insurance policies
CREATE TABLE insurance_policies (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    policy_number VARCHAR(50) NOT NULL UNIQUE,
    payer_id VARCHAR(50) NOT NULL,
    payer_name VARCHAR(255) NOT NULL,
    plan_name VARCHAR(255),
    group_number VARCHAR(50),
    subscriber_id VARCHAR(50) NOT NULL,
    subscriber_name VARCHAR(255),
    relationship VARCHAR(20) NOT NULL,
    effective_date DATE NOT NULL,
    termination_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    copay_amount DECIMAL(10,2),
    deductible_amount DECIMAL(10,2),
    deductible_met DECIMAL(10,2) DEFAULT 0,
    out_of_pocket_max DECIMAL(10,2),
    out_of_pocket_met DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_relationship CHECK (relationship IN ('SELF', 'SPOUSE', 'CHILD', 'PARENT', 'OTHER'))
);

-- Pre-authorizations
CREATE TABLE pre_authorizations (
    preauth_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES insurance_policies(policy_id),
    patient_id UUID NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    service_code VARCHAR(50) NOT NULL,
    requested_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    requested_by VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_date TIMESTAMP WITH TIME ZONE,
    decision_by VARCHAR(100),
    approval_number VARCHAR(50),
    denial_reason TEXT,
    valid_from DATE,
    valid_until DATE,
    units_approved INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_preauth_status CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'EXPIRED'))
);

-- Claims
CREATE TABLE insurance_claims (
    claim_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES insurance_policies(policy_id),
    patient_id UUID NOT NULL,
    claim_number VARCHAR(50) UNIQUE,
    service_date DATE NOT NULL,
    provider_id VARCHAR(50) NOT NULL,
    provider_name VARCHAR(255) NOT NULL,
    service_location VARCHAR(100),
    diagnosis_codes TEXT[] NOT NULL,
    procedure_codes TEXT[] NOT NULL,
    billed_amount DECIMAL(10,2) NOT NULL,
    allowed_amount DECIMAL(10,2),
    paid_amount DECIMAL(10,2),
    patient_responsibility DECIMAL(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    submission_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_date TIMESTAMP WITH TIME ZONE,
    payment_date TIMESTAMP WITH TIME ZONE,
    denial_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_claim_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'PROCESSING', 'PAID', 'DENIED', 'APPEALED'))
);

-- Indexes
CREATE INDEX idx_policies_patient ON insurance_policies(patient_id);
CREATE INDEX idx_policies_policy_number ON insurance_policies(policy_number);
CREATE INDEX idx_preauth_policy ON pre_authorizations(policy_id);
CREATE INDEX idx_preauth_status ON pre_authorizations(status);
CREATE INDEX idx_claims_policy ON insurance_claims(policy_id);
CREATE INDEX idx_claims_patient ON insurance_claims(patient_id);
CREATE INDEX idx_claims_status ON insurance_claims(status);
```

### 4.2 Caching Strategy

#### Redis Cache Layers
```yaml
Cache Configuration:
  L1 - Application Cache (Caffeine):
    - Type: In-memory local cache
    - TTL: 5 minutes
    - Max Size: 1000 entries per service
    - Eviction: LRU

  L2 - Distributed Cache (Redis):
    Patient Cache:
      - Key Pattern: patient:{patientId}
      - TTL: 1 hour
      - Invalidation: On patient.updated.v1 event

    Patient Search Cache:
      - Key Pattern: patient:search:{hash}
      - TTL: 15 minutes
      - Eviction: LRU

    Lab Results Cache:
      - Key Pattern: lab:results:{orderId}
      - TTL: 24 hours
      - Invalidation: Never (immutable)

    Insurance Verification Cache:
      - Key Pattern: insurance:verify:{policyId}
      - TTL: 1 hour
      - Invalidation: On policy.updated.v1 event

    Session Cache:
      - Key Pattern: session:{sessionId}
      - TTL: 30 minutes (sliding)
      - Eviction: On logout
```

#### Cache Implementation
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Patient cache - 1 hour TTL
        cacheConfigurations.put("patients", defaultConfig
            .entryTtl(Duration.ofHours(1)));

        // Patient search cache - 15 minutes TTL
        cacheConfigurations.put("patient_search", defaultConfig
            .entryTtl(Duration.ofMinutes(15)));

        // Lab results cache - 24 hours TTL
        cacheConfigurations.put("lab_results", defaultConfig
            .entryTtl(Duration.ofHours(24)));

        // Insurance verification - 1 hour TTL
        cacheConfigurations.put("insurance_verification", defaultConfig
            .entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
```

#### Cache Invalidation Handler
```java
@Component
public class CacheInvalidationHandler {

    @Autowired
    private CacheManager cacheManager;

    @KafkaListener(topics = "patient-events")
    public void handlePatientEvent(PatientEvent event) {
        if ("patient.updated.v1".equals(event.getEventType())) {
            evictPatientCaches(event.getPatientId());
        }
    }

    private void evictPatientCaches(String patientId) {
        Cache patientCache = cacheManager.getCache("patients");
        if (patientCache != null) {
            patientCache.evict(patientId);
        }

        // Invalidate all search results
        Cache searchCache = cacheManager.getCache("patient_search");
        if (searchCache != null) {
            searchCache.clear();
        }
    }
}
```

## 5. Security Architecture

### 5.1 Authentication & Authorization

#### OAuth2/OIDC with Keycloak
```yaml
Keycloak Configuration:
  Realm: meditrack
  Client ID: meditrack-api
  Grant Types:
    - authorization_code
    - refresh_token
    - client_credentials
  Token Settings:
    Access Token Lifespan: 5 minutes
    Refresh Token Lifespan: 30 minutes
    SSO Session Idle: 30 minutes
    SSO Session Max: 10 hours
```

#### JWT Token Structure
```json
{
  "sub": "user123",
  "iss": "https://keycloak.meditrack.com/realms/meditrack",
  "aud": "meditrack-api",
  "exp": 1609459200,
  "iat": 1609455600,
  "auth_time": 1609455600,
  "realm_access": {
    "roles": ["DOCTOR", "HEALTHCARE_PROVIDER"]
  },
  "resource_access": {
    "patient-service": {
      "roles": ["PATIENT_READ", "PATIENT_WRITE"]
    },
    "lab-service": {
      "roles": ["LAB_ORDER_CREATE", "LAB_RESULTS_READ"]
    }
  },
  "tenant_id": "hospital-001",
  "provider_id": "DOC-12345",
  "preferred_username": "dr.smith@hospital.com"
}
```

#### Spring Security Configuration
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/patients/**").hasRole("HEALTHCARE_PROVIDER")
                .requestMatchers("/api/v1/lab/**").hasAnyRole("DOCTOR", "LAB_TECHNICIAN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }
}
```

#### Role-Based Access Control (RBAC)
```yaml
Healthcare Roles:
  HEALTHCARE_ADMIN:
    - Full system access
    - User management
    - Configuration management

  DOCTOR:
    - Read/write patient data
    - Order lab tests
    - Create prescriptions
    - View lab results
    - Create medical records

  NURSE:
    - Read patient data
    - Update vital signs
    - View lab results
    - Limited medical record access

  LAB_TECHNICIAN:
    - Read lab orders
    - Update order status
    - Enter test results
    - Manage specimens

  PHARMACIST:
    - Read prescriptions
    - Fill prescriptions
    - Check drug interactions
    - Manage inventory

  INSURANCE_AGENT:
    - Verify eligibility
    - Process claims
    - Manage pre-authorizations

  PATIENT:
    - Read own medical records
    - View lab results
    - Manage appointments
    - Update personal information

Permission Matrix:
  Patient Data:
    READ: [DOCTOR, NURSE, PATIENT(own)]
    WRITE: [DOCTOR, NURSE]
    DELETE: [HEALTHCARE_ADMIN]

  Lab Orders:
    CREATE: [DOCTOR]
    READ: [DOCTOR, NURSE, LAB_TECHNICIAN, PATIENT(own)]
    UPDATE_STATUS: [LAB_TECHNICIAN]
    CANCEL: [DOCTOR, LAB_TECHNICIAN]

  Lab Results:
    READ: [DOCTOR, NURSE, LAB_TECHNICIAN, PATIENT(own)]
    WRITE: [LAB_TECHNICIAN]
    VERIFY: [LAB_TECHNICIAN(supervisor)]

  Prescriptions:
    CREATE: [DOCTOR]
    READ: [DOCTOR, PHARMACIST, PATIENT(own)]
    FILL: [PHARMACIST]

  Insurance Claims:
    CREATE: [HEALTHCARE_ADMIN, INSURANCE_AGENT]
    READ: [DOCTOR, INSURANCE_AGENT, PATIENT(own)]
    UPDATE: [INSURANCE_AGENT]
```

### 5.2 Data Encryption

#### Encryption at Rest
```yaml
Database Encryption:
  PostgreSQL:
    - Method: Transparent Data Encryption (TDE)
    - Algorithm: AES-256
    - Key Management: AWS KMS / Azure Key Vault
    - Key Rotation: Every 90 days

  Redis:
    - RDB Encryption: Enabled
    - AOF Encryption: Enabled
    - Key: Customer-managed via KMS

  Kafka:
    - Topic Encryption: Enabled
    - Algorithm: AES-256-GCM
    - Key Management: Confluent Schema Registry + KMS

Field-Level Encryption:
  Sensitive Fields:
    - SSN: AES-256 with column-level encryption
    - Credit Card: Tokenization via payment gateway
    - Medical Notes: AES-256 with application-level encryption

  Implementation:
    - Spring Crypto for field encryption
    - Separate encryption keys per tenant
    - Key derivation using PBKDF2
```

#### Encryption Configuration
```java
@Configuration
public class EncryptionConfig {

    @Bean
    public TextEncryptor textEncryptor(
            @Value("${encryption.key}") String key,
            @Value("${encryption.salt}") String salt) {
        return Encryptors.text(key, salt);
    }

    @Bean
    public BytesEncryptor bytesEncryptor(
            @Value("${encryption.key}") String key,
            @Value("${encryption.salt}") String salt) {
        return Encryptors.standard(key, salt);
    }
}

@Component
public class SensitiveDataEncryptor {

    @Autowired
    private TextEncryptor textEncryptor;

    public String encryptSSN(String ssn) {
        return textEncryptor.encrypt(ssn);
    }

    public String decryptSSN(String encryptedSSN) {
        return textEncryptor.decrypt(encryptedSSN);
    }
}
```

#### Encryption in Transit
```yaml
Internal Communication:
  Service-to-Service:
    - Protocol: mTLS (Mutual TLS)
    - Version: TLS 1.3
    - Certificate Authority: Internal CA
    - Certificate Rotation: Every 90 days
    - Cipher Suites: TLS_AES_256_GCM_SHA384

  Database Connections:
    - PostgreSQL: SSL mode=verify-full
    - Redis: TLS with certificate validation

  Kafka:
    - Protocol: SASL_SSL
    - SASL Mechanism: SCRAM-SHA-512
    - TLS Version: 1.3

External Communication:
  Client APIs:
    - Protocol: HTTPS only
    - TLS Version: 1.3
    - HSTS: Enabled (max-age=31536000)
    - Certificate: Wildcard SSL from Let's Encrypt

  Third-party APIs:
    - TLS 1.3 required
    - Certificate pinning for critical integrations
    - API key authentication over HTTPS
```

### 5.3 HIPAA Compliance Framework

#### Technical Safeguards
```yaml
Access Control (§164.312(a)):
  - Unique user identification for all users
  - Emergency access procedures documented
  - Automatic logoff after 15 minutes inactivity
  - Encryption and decryption mechanisms
  - Role-based access control (RBAC)

Audit Controls (§164.312(b)):
  - All PHI access logged to audit service
  - Failed authentication attempts tracked
  - System configuration changes audited
  - Audit logs immutable and encrypted
  - Log retention: 7 years

Integrity (§164.312(c)):
  - Checksums for data transmission
  - Digital signatures for critical transactions
  - Data validation at API boundaries
  - Database transaction integrity

Person or Entity Authentication (§164.312(d)):
  - Multi-factor authentication for privileged users
  - JWT tokens with short expiration
  - Certificate-based authentication for services
  - Biometric authentication for mobile apps

Transmission Security (§164.312(e)):
  - End-to-end encryption for all data transmission
  - VPN for remote access
  - Secure file transfer protocols (SFTP)
  - Network segmentation and firewalls
```

#### Audit Logging Implementation
```java
@Aspect
@Component
public class AuditLoggingAspect {

    @Autowired
    private AuditEventPublisher auditPublisher;

    @Around("@annotation(auditable)")
    public Object auditDataAccess(ProceedingJoinPoint joinPoint,
                                  Auditable auditable) throws Throwable {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String action = auditable.action();
        String resourceType = auditable.resourceType();

        AuditEvent auditEvent = AuditEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(Instant.now())
            .userId(userId)
            .action(action)
            .resourceType(resourceType)
            .ipAddress(getClientIpAddress())
            .userAgent(getUserAgent())
            .build();

        try {
            Object result = joinPoint.proceed();
            auditEvent.setStatus("SUCCESS");
            auditEvent.setResourceId(extractResourceId(result));
            return result;
        } catch (Exception e) {
            auditEvent.setStatus("FAILURE");
            auditEvent.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditPublisher.publish(auditEvent);
        }
    }
}

// Usage
@Auditable(action = "READ", resourceType = "PATIENT")
public Patient getPatient(String patientId) {
    return patientRepository.findById(patientId);
}
```

## 6. Monitoring & Observability

### 6.1 Metrics Collection

#### Prometheus Metrics
```yaml
Business Metrics:
  - meditrack_patient_registrations_total (Counter)
  - meditrack_lab_orders_created_total (Counter)
  - meditrack_lab_turnaround_time_seconds (Histogram)
  - meditrack_insurance_approval_rate (Gauge)
  - meditrack_prescription_fills_total (Counter)
  - meditrack_critical_results_total (Counter)

Technical Metrics:
  - http_server_requests_seconds (Histogram)
  - jvm_memory_used_bytes (Gauge)
  - jvm_gc_pause_seconds (Summary)
  - jdbc_connections_active (Gauge)
  - kafka_consumer_lag (Gauge)
  - cache_hits_total (Counter)
  - cache_misses_total (Counter)

Infrastructure Metrics:
  - system_cpu_usage (Gauge)
  - system_memory_usage (Gauge)
  - disk_io_bytes (Counter)
  - network_io_bytes (Counter)
```

#### Custom Metrics Implementation
```java
@Component
public class BusinessMetrics {

    private final Counter patientRegistrations;
    private final Counter labOrdersCreated;
    private final Histogram labTurnaroundTime;
    private final Gauge insuranceApprovalRate;

    public BusinessMetrics(MeterRegistry registry) {
        this.patientRegistrations = Counter.builder("meditrack.patient.registrations")
            .description("Total number of patient registrations")
            .tag("service", "patient-service")
            .register(registry);

        this.labOrdersCreated = Counter.builder("meditrack.lab.orders.created")
            .description("Total number of lab orders created")
            .tag("service", "lab-service")
            .register(registry);

        this.labTurnaroundTime = Histogram.builder("meditrack.lab.turnaround.seconds")
            .description("Lab test turnaround time in seconds")
            .baseUnit("seconds")
            .buckets(300, 900, 1800, 3600, 7200, 14400, 28800, 86400)
            .register(registry);
    }

    public void recordPatientRegistration() {
        patientRegistrations.increment();
    }

    public void recordLabOrderCreated(String priority) {
        labOrdersCreated.increment(Tag.of("priority", priority));
    }

    public void recordLabTurnaround(Duration duration) {
        labTurnaroundTime.record(duration.getSeconds());
    }
}
```

### 6.2 Distributed Tracing

#### Jaeger Configuration
```yaml
spring:
  application:
    name: ${SERVICE_NAME}
  sleuth:
    sampler:
      probability: 1.0  # 100% sampling in dev, 10% in prod
    baggage:
      remote-fields:
        - tenant-id
        - user-id
        - correlation-id
      correlation-fields:
        - tenant-id
        - user-id
  zipkin:
    base-url: http://jaeger:9411
    enabled: true
```

#### Trace Context Propagation
```java
@Component
public class TraceContextFilter extends OncePerRequestFilter {

    @Autowired
    private Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        Span span = tracer.currentSpan();
        if (span != null) {
            // Add business context to trace
            span.tag("tenant.id", getTenantId(request));
            span.tag("user.id", getUserId(request));
            span.tag("user.role", getUserRole(request));

            // Add to MDC for logging
            MDC.put("traceId", span.context().traceId());
            MDC.put("spanId", span.context().spanId());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### 6.3 Logging Strategy

#### Structured Logging
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceId=%X{traceId:-}, spanId=%X{spanId:-}, tenantId=%X{tenantId:-}] - %msg%n"
  level:
    root: INFO
    com.meditrack: DEBUG
    org.springframework.kafka: WARN
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

#### Logging Implementation
```java
@Slf4j
@Component
public class StructuredLogger {

    public void logBusinessEvent(String eventType, Map<String, Object> context) {
        log.info("Business event occurred: {}",
            Map.of(
                "eventType", eventType,
                "timestamp", Instant.now(),
                "context", context,
                "tenantId", MDC.get("tenantId"),
                "userId", MDC.get("userId")
            ));
    }

    public void logSecurityEvent(String action, String resource, String result) {
        log.warn("Security event: {}",
            Map.of(
                "action", action,
                "resource", resource,
                "result", result,
                "userId", MDC.get("userId"),
                "ipAddress", getClientIp(),
                "timestamp", Instant.now()
            ));
    }
}
```

### 6.4 Alerting Rules

#### Prometheus Alert Rules
```yaml
groups:
  - name: meditrack_alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected in {{ $labels.service }}"
          description: "Error rate is {{ $value }} errors/sec"

      # High response time
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time in {{ $labels.service }}"
          description: "P95 response time is {{ $value }}s"

      # Kafka consumer lag
      - alert: HighKafkaConsumerLag
        expr: kafka_consumer_lag > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High Kafka consumer lag in {{ $labels.service }}"
          description: "Consumer lag is {{ $value }} messages"

      # Database connection pool exhaustion
      - alert: DatabaseConnectionPoolExhaustion
        expr: (jdbc_connections_active / jdbc_connections_max) > 0.8
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value }}%"

      # Memory usage
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage in {{ $labels.service }}"
          description: "Memory usage is {{ $value }}%"
```

## 7. Deployment Architecture

### 7.1 Docker Configuration

#### Patient Service Dockerfile
```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy artifact
COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Expose port
EXPOSE 8081

# JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Docker Compose (Development)
```yaml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    volumes:
      - zookeeper-data:/var/lib/zookeeper/data
      - zookeeper-logs:/var/lib/zookeeper/log

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    volumes:
      - kafka-data:/var/lib/kafka/data

  schema-registry:
    image: confluentinc/cp-schema-registry:7.5.0
    depends_on:
      - kafka
    ports:
      - "8081:8081"
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:9092

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_USER: meditrack
      POSTGRES_PASSWORD: meditrack_password
      POSTGRES_DB: meditrack
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d

  redis:
    image: redis:7.2-alpine
    command: redis-server --requirepass meditrack_redis_password
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  patient-service:
    build: ./services/patient-service
    depends_on:
      - kafka
      - postgres
      - redis
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/meditrack
      SPRING_DATASOURCE_USERNAME: meditrack
      SPRING_DATASOURCE_PASSWORD: meditrack_password
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PASSWORD: meditrack_redis_password
      JWT_SECRET: ${JWT_SECRET}

  lab-service:
    build: ./services/labrotary-service
    depends_on:
      - kafka
      - postgres
      - redis
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/meditrack
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_REDIS_HOST: redis

  insurance-service:
    build: ./services/insurance-service
    depends_on:
      - kafka
      - postgres
      - redis
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/meditrack
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus

  grafana:
    image: grafana/grafana:latest
    depends_on:
      - prometheus
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_INSTALL_PLUGINS: grafana-clock-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "5775:5775/udp"
      - "6831:6831/udp"
      - "6832:6832/udp"
      - "5778:5778"
      - "16686:16686"
      - "14268:14268"
      - "14250:14250"
      - "9411:9411"

volumes:
  zookeeper-data:
  zookeeper-logs:
  kafka-data:
  postgres-data:
  redis-data:
  prometheus-data:
  grafana-data:
```

### 7.2 Kubernetes Deployment

#### Patient Service Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: patient-service
  namespace: meditrack
  labels:
    app: patient-service
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: patient-service
  template:
    metadata:
      labels:
        app: patient-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: patient-service
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
      - name: patient-service
        image: meditrack/patient-service:latest
        imagePullPolicy: Always
        ports:
        - name: http
          containerPort: 8081
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          value: "kafka:9092"
        - name: SPRING_REDIS_HOST
          value: "redis"
        - name: SPRING_REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: patient-service
  namespace: meditrack
  labels:
    app: patient-service
spec:
  type: ClusterIP
  ports:
  - port: 8081
    targetPort: 8081
    protocol: TCP
    name: http
  selector:
    app: patient-service
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: patient-service-hpa
  namespace: meditrack
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: patient-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 2
        periodSeconds: 30
      selectPolicy: Max
```

#### Ingress Configuration
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: meditrack-ingress
  namespace: meditrack
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/cors-enable: "true"
    nginx.ingress.kubernetes.io/enable-opentracing: "true"
spec:
  tls:
  - hosts:
    - api.meditrack.com
    secretName: meditrack-tls
  rules:
  - host: api.meditrack.com
    http:
      paths:
      - path: /api/v1/patients
        pathType: Prefix
        backend:
          service:
            name: patient-service
            port:
              number: 8081
      - path: /api/v1/lab
        pathType: Prefix
        backend:
          service:
            name: lab-service
            port:
              number: 8082
      - path: /api/v1/insurance
        pathType: Prefix
        backend:
          service:
            name: insurance-service
            port:
              number: 8083
      - path: /api/v1/pharmacy
        pathType: Prefix
        backend:
          service:
            name: pharmacy-service
            port:
              number: 8084
```

## 8. Testing Strategy

### 8.1 Unit Testing
```java
@ExtendWith(MockitoExtension.class)
class PatientCommandServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientEventPublisher eventPublisher;

    @InjectMocks
    private PatientCommandService patientCommandService;

    @Test
    void createPatient_ShouldCreatePatientAndPublishEvent() {
        // Given
        CreatePatientCommand command = CreatePatientCommand.builder()
            .mrn("MRN-001")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        Patient patient = Patient.builder()
            .id(UUID.randomUUID())
            .mrn(new MRN("MRN-001"))
            .firstName("John")
            .lastName("Doe")
            .build();

        when(patientRepository.save(any(Patient.class)))
            .thenReturn(patient);

        // When
        Patient result = patientCommandService.createPatient(command);

        // Then
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(patientRepository).save(any(Patient.class));
        verify(eventPublisher).publishPatientCreated(any(PatientCreatedEvent.class));
    }
}
```

### 8.2 Integration Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@DirtiesContext
class PatientControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void createPatient_ShouldReturnCreatedPatient() {
        // Given
        CreatePatientRequest request = CreatePatientRequest.builder()
            .mrn("MRN-TEST-001")
            .firstName("Jane")
            .lastName("Smith")
            .dateOfBirth("1985-05-15")
            .build();

        // When
        ResponseEntity<PatientResponse> response = restTemplate.postForEntity(
            "/api/v1/patients",
            request,
            PatientResponse.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Jane", response.getBody().getFirstName());

        // Verify database
        Optional<Patient> savedPatient = patientRepository
            .findByMrn(new MRN("MRN-TEST-001"));
        assertTrue(savedPatient.isPresent());
    }
}
```

### 8.3 Contract Testing
```java
@WebMvcTest(PatientController.class)
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
class PatientControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientApplicationService patientService;

    @Test
    void getPatient_ShouldMatchContract() throws Exception {
        // Given
        UUID patientId = UUID.randomUUID();
        PatientResponse expectedResponse = PatientResponse.builder()
            .id(patientId.toString())
            .mrn("MRN-001")
            .firstName("John")
            .lastName("Doe")
            .build();

        when(patientService.getPatient(patientId))
            .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(patientId.toString()))
            .andExpect(jsonPath("$.mrn").value("MRN-001"))
            .andExpect(jsonPath("$.firstName").value("John"));
    }
}
```

### 8.4 End-to-End Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class PatientLabOrderE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Test
    void orderLabTest_ShouldTriggerLabServiceViaKafka() throws Exception {
        // 1. Create patient
        CreatePatientRequest patientRequest = new CreatePatientRequest(/*...*/);
        Patient patient = createPatient(patientRequest);

        // 2. Order lab test
        OrderLabTestRequest labRequest = OrderLabTestRequest.builder()
            .testCode("CBC")
            .priority("ROUTINE")
            .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/patients/{ssn}/order-labs",
            labRequest,
            String.class,
            patient.getSsn()
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 3. Verify event published to Kafka
        ConsumerRecord<String, LabTestOrderedEvent> record =
            kafkaTestListener.poll(Duration.ofSeconds(10));

        assertNotNull(record);
        assertEquals("lab.test.ordered.v1", record.value().getEventType());
        assertEquals("CBC", record.value().getOrder().getTestCode());
    }
}
```

## 9. Performance Requirements

### 9.1 Response Time SLAs
```yaml
API Response Times (95th percentile):
  Patient Service:
    - GET /patients/{id}: < 200ms
    - POST /patients: < 500ms
    - GET /patients/search: < 1000ms
    - GET /patients/{id}/timeline: < 2000ms

  Lab Service:
    - POST /lab/orders: < 500ms
    - GET /lab/results/{id}: < 300ms
    - GET /lab/orders/pending: < 1000ms

  Insurance Service:
    - POST /insurance/verify: < 2000ms
    - POST /insurance/preauth: < 3000ms
    - GET /insurance/claims/{id}: < 500ms

Event Processing:
  - Kafka event end-to-end latency: < 1000ms (p95)
  - Event processing time: < 500ms per event
  - Maximum consumer lag: 100 messages
```

### 9.2 Throughput Requirements
```yaml
Peak Load Capacity:
  - Patient registrations: 1000/minute
  - Lab orders: 5000/minute
  - Lab results: 3000/minute
  - Insurance verifications: 2000/minute
  - Concurrent users: 10,000

Kafka Throughput:
  - Events per second: 10,000
  - Peak throughput: 50MB/second
  - Message retention: 30 days minimum
```

### 9.3 Load Testing Configuration
```java
@LoadTest
public class PatientServiceLoadTest {

    @Test
    public void testPatientCreationUnderLoad() {
        ScenarioBuilder scenario = scenario("Patient Creation")
            .exec(
                http("Create Patient")
                    .post("/api/v1/patients")
                    .header("Content-Type", "application/json")
                    .body(StringBody("${patientJson}"))
                    .check(status().is(201))
            );

        setUp(
            scenario.injectOpen(
                rampUsersPerSec(10).to(100).during(60),
                constantUsersPerSec(100).during(300),
                rampUsersPerSec(100).to(10).during(60)
            )
        ).protocols(
            http.baseUrl("http://localhost:8081")
        ).assertions(
            global().responseTime().percentile(95).lt(500),
            global().successfulRequests().percent().gt(99.5)
        );
    }
}
```

## 10. Disaster Recovery & Business Continuity

### 10.1 Backup Strategy
```yaml
Database Backups:
  PostgreSQL:
    - Full backup: Daily at 2 AM UTC
    - Incremental backup: Every 6 hours
    - Point-in-time recovery: Enabled
    - Retention: 30 days
    - Storage: AWS S3 / Azure Blob (encrypted)

  Kafka:
    - Topic snapshots: Daily
    - Offset backups: Hourly
    - Retention: 7 days

Application Backups:
  - Configuration files: Version controlled in Git
  - Secrets: Backed up in KMS
  - Docker images: Stored in registry with tags
```

### 10.2 Disaster Recovery Plan
```yaml
Recovery Time Objective (RTO): 4 hours
Recovery Point Objective (RPO): 1 hour

DR Procedures:
  1. Database Recovery:
     - Restore from latest snapshot
     - Apply WAL logs for point-in-time recovery
     - Validate data integrity

  2. Kafka Recovery:
     - Deploy new Kafka cluster
     - Restore topics from snapshots
     - Restart consumers with saved offsets

  3. Service Recovery:
     - Deploy services to DR region
     - Update DNS to point to DR cluster
     - Verify service health
     - Resume traffic
```

## 11. Compliance & Regulatory Requirements

### 11.1 HIPAA Compliance Checklist
```yaml
Administrative Safeguards:
  ✓ Security Management Process
  ✓ Workforce Security
  ✓ Information Access Management
  ✓ Security Awareness Training
  ✓ Security Incident Procedures
  ✓ Contingency Plan
  ✓ Business Associate Agreements

Physical Safeguards:
  ✓ Facility Access Controls
  ✓ Workstation Use and Security
  ✓ Device and Media Controls

Technical Safeguards:
  ✓ Access Control
  ✓ Audit Controls
  ✓ Integrity Controls
  ✓ Transmission Security
```

### 11.2 Data Retention Policy
```yaml
Patient Data:
  - Medical Records: 7 years after last treatment
  - Lab Results: 7 years
  - Prescriptions: 7 years
  - Insurance Claims: 10 years

Audit Logs:
  - Access Logs: 7 years
  - Security Events: 7 years
  - System Logs: 90 days

Soft Delete Implementation:
  - Mark records as deleted
  - Retain for retention period
  - Hard delete after retention expires
```

## 12. Development Workflow

### 12.1 Git Branching Strategy
```
main (production)
  └── develop
       ├── feature/PATIENT-001-add-search
       ├── feature/LAB-045-critical-results
       └── bugfix/INS-023-claim-calculation
```

### 12.2 CI/CD Pipeline
```yaml
stages:
  - build
  - test
  - security-scan
  - package
  - deploy

build:
  - mvn clean compile
  - SonarQube analysis

test:
  - mvn test
  - Integration tests
  - Code coverage (min 80%)

security-scan:
  - OWASP dependency check
  - Container scanning
  - Secret scanning

package:
  - Build Docker image
  - Push to registry
  - Tag with version

deploy:
  - Deploy to dev (automatic)
  - Deploy to staging (automatic)
  - Deploy to production (manual approval)
```

## 13. Migration Strategy

### 13.1 Data Migration
```sql
-- Phase 1: Create new tables
-- Phase 2: Dual-write to old and new systems
-- Phase 3: Backfill historical data
-- Phase 4: Validate data consistency
-- Phase 5: Switch read traffic to new system
-- Phase 6: Decommission old system
```

This completes the comprehensive technical specification for MediTrack. All components are production-ready and can be implemented as specified.
