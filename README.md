# MediTrack - Healthcare Data Exchange Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6-red.svg)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)

MediTrack is an event-driven microservices platform that solves healthcare data fragmentation by creating a real-time data exchange hub. The system enables seamless interoperability between hospitals, laboratories, insurance providers, and pharmacies through Apache Kafka-powered event streaming.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Local Development](#local-development)
- [Docker Deployment](#docker-deployment)
- [Service Ports](#service-ports)
- [API Documentation](#api-documentation)
- [Monitoring & Observability](#monitoring--observability)
- [Database Schema](#database-schema)
- [Testing](#testing)
- [Project Structure](#project-structure)

## 🎯 Overview

### The Problem We Solve

Healthcare data is fragmented across multiple systems (hospital EMRs, lab systems, insurance databases, pharmacy systems), leading to:
- Manual, error-prone data entry
- Delayed care decisions
- Poor patient experience
- Inconsistent information

### Our Solution

MediTrack creates a **central data exchange hub** that connects all healthcare systems in real-time using:
- **Event-Driven Architecture**: All inter-service communication via Kafka events
- **Hexagonal Architecture**: Clean separation of business logic from external concerns
- **Domain-Driven Design**: Service boundaries aligned with healthcare business domains
- **CQRS Pattern**: Separate read/write models for optimal performance

## 🏗️ Architecture

### Microservices

```
┌─────────────────┐     ┌──────────────────┐     ┌────────────────────┐
│ Patient Service │ ──► │   Apache Kafka   │ ◄── │ Laboratory Service │
│    (Port 8081)  │     │  Event Streaming │     │    (Port 8082)     │
└─────────────────┘     └──────────────────┘     └────────────────────┘
         │                        │                         │
         │                        ▼                         │
         │              ┌──────────────────┐               │
         └────────────► │ Insurance Service│ ◄─────────────┘
                        │   (Port 8083)    │
                        └──────────────────┘
```

### Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Framework** | Spring Boot 3.2 | Microservices foundation |
| **Event Streaming** | Apache Kafka 3.6 | Event backbone |
| **Schema Registry** | Confluent 7.5 | Event schema evolution |
| **Database** | PostgreSQL 15 | Transactional data |
| **Caching** | Redis 7.2 | Session & query cache |
| **Authentication** | Keycloak 23 | Identity & access management |
| **API Gateway** | Kong 3.4 | Centralized API management |
| **Monitoring** | Prometheus/Grafana | Metrics & visualization |
| **Tracing** | Jaeger 1.50 | Distributed tracing |

## 📦 Prerequisites

### Required Software

- **Java 17+** (Patient Service) or **Java 21+** (Lab & Insurance Services)
- **Maven 3.9+**
- **Docker 24+** & **Docker Compose**
- **Git**

### For Windows Development

- Install Kafka to `C:\kafka` if running locally without Docker
- Or use Docker Compose (recommended)

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd MediTrack

# Start all services with infrastructure
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f patient-service
```

### Option 2: Local Development

```bash
# 1. Start infrastructure services
docker-compose up -d postgres redis kafka zookeeper schema-registry

# 2. Build all services
cd services/patient-service && ./mvnw clean install
cd ../labrotary-service && ./mvnw clean install
cd ../insurance-service && ./mvnw clean install

# 3. Run services (in separate terminals)
cd services/patient-service && ./mvnw spring-boot:run
cd services/labrotary-service && ./mvnw spring-boot:run
cd services/insurance-service && ./mvnw spring-boot:run
```

## 💻 Local Development

### Running Individual Services

#### Patient Service
```bash
cd services/patient-service
./mvnw spring-boot:run

# With Docker profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

#### Laboratory Service
```bash
cd services/labrotary-service
./mvnw spring-boot:run
```

#### Insurance Service
```bash
cd services/insurance-service
./mvnw spring-boot:run
```

### Database Access

**H2 Console (Local Development):**
- Patient Service: http://localhost:8081/h2-console
- Lab Service: http://localhost:8082/h2-console
- Insurance Service: http://localhost:8083/h2-console

**PostgreSQL (Docker):**
```bash
# Connect to PostgreSQL
docker exec -it meditrack-postgres psql -U meditrack -d patient_db

# List all databases
\l

# Connect to specific database
\c lab_db

# List tables
\dt
```

### Kafka Operations

```bash
# Windows (if Kafka installed locally at C:\kafka)
cd C:\kafka

# List topics
.\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --list

# Consume patient events
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic patient-events --from-beginning

# Consume lab events
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic lab-events --from-beginning
```

## 🐳 Docker Deployment

### Build Service Images

```bash
# Build all services
docker-compose build

# Build specific service
docker-compose build patient-service
```

### Start Complete Stack

```bash
# Start everything
docker-compose up -d

# Start only infrastructure
docker-compose up -d postgres redis kafka zookeeper

# Start specific services
docker-compose up -d patient-service lab-service
```

## 🔌 Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| **Patient Service** | 8081 | Patient management API |
| **Laboratory Service** | 8082 | Lab orders & results API |
| **Insurance Service** | 8083 | Insurance & claims API |
| **Kong API Gateway** | 8000 | HTTP proxy |
| **PostgreSQL** | 5432 | Database |
| **Redis** | 6379 | Cache |
| **Kafka** | 9092 | Event broker |
| **Schema Registry** | 8081 | Avro schemas |
| **Keycloak** | 8180 | Authentication |
| **Prometheus** | 9090 | Metrics |
| **Grafana** | 3000 | Dashboards |
| **Jaeger UI** | 16686 | Distributed tracing |

## 📚 API Documentation

### Patient Service

```bash
# Create patient
POST http://localhost:8081/api/v1/patients
Content-Type: application/json

{
  "mrn": "MRN-001",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1980-01-15",
  "gender": "MALE",
  "email": "john.doe@example.com",
  "phone": "555-1234"
}

# Get patient by ID
GET http://localhost:8081/api/v1/patients/{id}

# Order lab test
POST http://localhost:8081/api/v1/patients/{ssn}/order-labs
Content-Type: application/json

{
  "testCodes": ["CBC", "BMP"],
  "priority": "ROUTINE"
}
```

### Laboratory Service

```bash
# Create lab order
POST http://localhost:8082/api/v1/lab/orders
Content-Type: application/json

{
  "patientId": "patient-uuid",
  "orderingProviderId": "DR-001",
  "tests": [{"testCode": "CBC", "testName": "Complete Blood Count"}],
  "priority": "ROUTINE"
}
```

### Actuator Endpoints

All services expose Spring Boot Actuator endpoints:

```bash
# Health check
GET http://localhost:8081/actuator/health

# Prometheus metrics
GET http://localhost:8081/actuator/prometheus
```

## 📊 Monitoring & Observability

### Grafana Dashboards

Access Grafana at **http://localhost:3000** (admin/admin)

Pre-configured dashboards:
- **MediTrack Platform Overview**: Service health, request rates, response times
- **JVM Metrics**: Memory usage, GC activity, thread pools

### Prometheus

Access Prometheus at **http://localhost:9090**

Key metrics:
- `http_server_requests_seconds`: Request latency
- `jvm_memory_used_bytes`: JVM memory usage
- `kafka_consumer_lag`: Event processing lag

### Jaeger Tracing

Access Jaeger UI at **http://localhost:16686**

Track request flows across services with distributed tracing.

## 🗄️ Database Schema

### Patient Service Schema

- `patients`: Core patient demographics
- `patient_addresses`: Multiple addresses per patient
- `patient_insurance`: Insurance policies
- `medical_records`: Comprehensive medical history
- `patient_timeline`: Unified care timeline
- `lab_orders`: Lab test orders

### Laboratory Service Schema

- `lab_orders`: Test orders
- `lab_tests`: Individual tests with results
- `specimens`: Physical specimens
- `reference_ranges`: Normal value ranges
- `test_catalog`: Available tests

### Insurance Service Schema

- `insurance_policies`: Patient policies
- `payers`: Insurance companies
- `pre_authorizations`: Pre-auth requests
- `insurance_claims`: Claims

## 🧪 Testing

### Run Unit Tests

```bash
cd services/patient-service
./mvnw test

# Run specific test class
./mvnw test -Dtest=PatientControllerTest
```

## 📁 Project Structure

```
MediTrack/
├── services/
│   ├── patient-service/          # Patient management microservice
│   ├── labrotary-service/        # Laboratory microservice
│   └── insurance-service/        # Insurance microservice
├── infrastructure/
│   └── postgres/init-scripts/    # Database initialization
├── monitoring/
│   ├── prometheus/               # Prometheus config & alerts
│   └── grafana/                  # Grafana dashboards
├── docker-compose.yml            # Complete stack definition
├── meditrack_tech_spec.md        # Technical specification
└── README.md                     # This file
```

---

**Built with ❤️ for better healthcare interoperability**