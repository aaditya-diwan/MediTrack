# MediTrack — Service Testing Guide

End-to-end guide for testing all three microservices manually (curl), via the UI, and through automated test suites. Covers auth, happy paths, validation, security, error scenarios, and Kafka event verification.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Start the Stack](#2-start-the-stack)
3. [Authentication](#3-authentication)
4. [Patient Service (port 8081)](#4-patient-service)
5. [Lab Service (port 8082)](#5-lab-service)
6. [Insurance Service (port 8083)](#6-insurance-service)
7. [Cross-Service Flows](#7-cross-service-flows)
8. [Error & Validation Scenarios](#8-error--validation-scenarios)
9. [Security & Authorization](#9-security--authorization)
10. [Observability](#10-observability)
11. [Automated Tests](#11-automated-tests)

---

## 1. Prerequisites

| Tool | Purpose |
|------|---------|
| Docker Desktop | Run the full stack |
| `curl` | HTTP testing |
| `jq` | Pretty-print JSON responses |
| `kafkacat` / `kcat` | Inspect Kafka messages |

Export a shorthand for `curl` with JSON headers:

```bash
alias jcurl='curl -s -H "Content-Type: application/json"'
```

---

## 2. Start the Stack

```bash
# From the repo root
docker compose up -d

# Verify all containers are healthy
docker compose ps
```

**Expected containers:**

| Container | Port(s) | Health Check |
|-----------|---------|--------------|
| `meditrack-patient-service` | 8081 | `GET /actuator/health` |
| `meditrack-lab-service` | 8082 | `GET /actuator/health` |
| `meditrack-insurance-service` | 8083 | `GET /actuator/health` |
| `meditrack-postgres` | 5432 | — |
| `meditrack-redis` | 6379 | — |
| `meditrack-kafka` | 9092, 29092 | — |
| `meditrack-zookeeper` | 2181 | — |
| `meditrack-jaeger` | 16686 | — |
| `meditrack-prometheus` | 9090 | — |
| `meditrack-grafana` | 3000 | — |
| `meditrack-kong` | 8000 (proxy), 8001 (admin) | — |

**Verify each service is up:**

```bash
curl -s http://localhost:8081/actuator/health | jq .
curl -s http://localhost:8082/actuator/health | jq .
curl -s http://localhost:8083/actuator/health | jq .
```

Expected response:

```json
{ "status": "UP" }
```

---

## 3. Authentication

All endpoints (except `/api/v1/auth/**`, `/actuator/health`, `/actuator/info`) require a Bearer JWT. Authentication is handled by the **patient-service**.

### 3.1 Obtain a JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "doctor1",
    "password": "password123"
  }' | jq -r '.jwt')

echo "Token: $TOKEN"
```

Store the token for reuse:

```bash
export AUTH="Authorization: Bearer $TOKEN"
```

Use it in subsequent requests:

```bash
curl -s http://localhost:8081/api/v1/patients/<id> -H "$AUTH" | jq .
```

### 3.2 Token Details

| Property | Value |
|----------|-------|
| Expiry | 24 hours (`jwt.expiration=86400`) |
| Algorithm | HS256 |
| Header key | `Authorization: Bearer <token>` |

### 3.3 Invalid Credentials

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username": "nobody", "password": "wrong"}'
```

Expected response (`401 Unauthorized`):

```json
{
  "timestamp": "2025-04-17T10:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Bad credentials",
  "path": "/api/v1/auth/authenticate"
}
```

### 3.4 Missing Token

```bash
curl -s http://localhost:8081/api/v1/patients/some-id
```

Expected: `401 Unauthorized`

### 3.5 Expired / Malformed Token

```bash
curl -s http://localhost:8081/api/v1/patients/some-id \
  -H "Authorization: Bearer thisIsNotAValidToken"
```

Expected: `401 Unauthorized`

---

## 4. Patient Service

**Base URL:** `http://localhost:8081`

---

### 4.1 Create Patient

**`POST /api/v1/patients`**  
Roles: `ADMIN`, `DOCTOR`, `NURSE`

```bash
PATIENT=$(curl -s -X POST http://localhost:8081/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{
    "mrn": "MRN-001",
    "ssn": "123-45-6789",
    "firstName": "Jane",
    "lastName": "Smith",
    "dateOfBirth": "1985-06-15",
    "email": "jane.smith@example.com",
    "phoneNumber": "555-0100",
    "address": "42 Elm Street, Springfield, IL 62701",
    "insuranceProvider": "Blue Cross Blue Shield",
    "insurancePolicyNumber": "BCBS-9900001"
  }')

echo "$PATIENT" | jq .
export PATIENT_ID=$(echo "$PATIENT" | jq -r '.id')
echo "Patient ID: $PATIENT_ID"
```

Expected status: `201 Created`

Expected response shape:

```json
{
  "id": "uuid",
  "mrn": "MRN-001",
  "ssn": "123-45-6789",
  "firstName": "Jane",
  "lastName": "Smith",
  "dateOfBirth": "1985-06-15",
  "email": "jane.smith@example.com",
  "phoneNumber": "555-0100",
  "address": "42 Elm Street, Springfield, IL 62701",
  "insuranceProvider": "Blue Cross Blue Shield",
  "insurancePolicyNumber": "BCBS-9900001",
  "medicalHistory": []
}
```

---

### 4.2 Get Patient by ID

**`GET /api/v1/patients/{id}`**  
Roles: `ADMIN`, `DOCTOR`, `NURSE`, `LAB_TECH`

```bash
curl -s http://localhost:8081/api/v1/patients/$PATIENT_ID \
  -H "$AUTH" | jq .
```

Expected status: `200 OK`

---

### 4.3 Update Patient

**`PUT /api/v1/patients/{id}`**  
Roles: `ADMIN`, `DOCTOR`, `NURSE`

```bash
curl -s -X PUT http://localhost:8081/api/v1/patients/$PATIENT_ID \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{
    "phoneNumber": "555-9999",
    "address": "100 Oak Avenue, Chicago, IL 60601"
  }' | jq .
```

Expected status: `200 OK`

---

### 4.4 Search Patients

**`GET /api/v1/patients/search?query=<string>`**  
Roles: All authenticated

```bash
# Search by name
curl -s "http://localhost:8081/api/v1/patients/search?query=Jane" \
  -H "$AUTH" | jq .

# Search by MRN
curl -s "http://localhost:8081/api/v1/patients/search?query=MRN-001" \
  -H "$AUTH" | jq .

# Search by email
curl -s "http://localhost:8081/api/v1/patients/search?query=jane.smith" \
  -H "$AUTH" | jq .
```

Expected status: `200 OK`, body is a JSON array.

---

### 4.5 Get Patient Timeline

**`GET /api/v1/patients/search/{id}/timeline`**  
Roles: All authenticated

```bash
curl -s "http://localhost:8081/api/v1/patients/search/$PATIENT_ID/timeline" \
  -H "$AUTH" | jq .
```

Expected response shape:

```json
{
  "patientId": "uuid",
  "timeline": []
}
```

---

### 4.6 Create Medical Record

**`POST /api/v1/medical-records`**  
Roles: `ADMIN`, `DOCTOR`, `NURSE`

```bash
RECORD=$(curl -s -X POST http://localhost:8081/api/v1/medical-records \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{
    \"patientId\": \"$PATIENT_ID\",
    \"diagnosis\": \"Type 2 Diabetes Mellitus\",
    \"treatment\": \"Metformin 500mg twice daily, dietary modifications\",
    \"date\": \"2025-04-17\"
  }")

echo "$RECORD" | jq .
export RECORD_ID=$(echo "$RECORD" | jq -r '.recordId')
echo "Record ID: $RECORD_ID"
```

Expected status: `201 Created`

Expected response shape:

```json
{
  "recordId": "uuid",
  "diagnosis": "Type 2 Diabetes Mellitus",
  "treatment": "Metformin 500mg twice daily, dietary modifications",
  "date": "2025-04-17"
}
```

---

### 4.7 Get Medical Record by ID

**`GET /api/v1/medical-records/{recordId}`**  
Roles: `ADMIN`, `DOCTOR`, `NURSE`, `LAB_TECH`

```bash
curl -s http://localhost:8081/api/v1/medical-records/$RECORD_ID \
  -H "$AUTH" | jq .
```

---

### 4.8 Get All Records for Patient

**`GET /api/v1/medical-records/patient/{patientId}`**  
Roles: `ADMIN`, `DOCTOR`, `NURSE`, `LAB_TECH`

```bash
curl -s http://localhost:8081/api/v1/medical-records/patient/$PATIENT_ID \
  -H "$AUTH" | jq .
```

Expected: JSON array of `MedicalRecordResponse` objects.

---

### 4.9 Update Medical Record

**`PUT /api/v1/medical-records/{recordId}`**  
Roles: `ADMIN`, `DOCTOR`, `NURSE`

```bash
curl -s -X PUT http://localhost:8081/api/v1/medical-records/$RECORD_ID \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{
    "treatment": "Metformin 1000mg twice daily, dietary modifications, exercise program"
  }' | jq .
```

Expected status: `200 OK`

---

### 4.10 Delete Medical Record

**`DELETE /api/v1/medical-records/{recordId}`**  
Roles: `ADMIN` only

```bash
curl -s -X DELETE http://localhost:8081/api/v1/medical-records/$RECORD_ID \
  -H "$AUTH" -w "\nHTTP Status: %{http_code}\n"
```

Expected status: `204 No Content`

---

### 4.11 Order Lab Test via Patient (Kafka trigger)

**`POST /api/v1/patients/{ssn}/order-labs/`**  
Roles: `ADMIN`, `DOCTOR`

This endpoint fires a `LabTestOrderedEvent` onto the `patient-events` Kafka topic, which the lab service consumes asynchronously to create a `LabOrder`.

```bash
# Use the patient's SSN, not their UUID
export PATIENT_SSN="123-45-6789"

curl -s -X POST "http://localhost:8081/api/v1/patients/$PATIENT_SSN/order-labs/" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{
    "testCode": "CBC",
    "priority": "ROUTINE",
    "doctorId": "doctor-001",
    "notes": "Annual complete blood count"
  }' -w "\nHTTP Status: %{http_code}\n"
```

Expected status: `200 OK`  
Expected body: plain text confirmation message.

> **Note:** The `LabOrder` in the lab service is created asynchronously after Kafka consumption. Wait a few seconds before querying it.

---

## 5. Lab Service

**Base URL:** `http://localhost:8082`

The lab service uses the **same JWT** issued by patient-service.

---

### 5.1 Create Lab Order (Direct)

**`POST /api/v1/lab/orders`**

Use this to create a lab order directly (bypassing the Kafka flow). For the Kafka-triggered flow, see [section 4.11](#411-order-lab-test-via-patient-kafka-trigger).

```bash
LAB_ORDER=$(curl -s -X POST http://localhost:8082/api/v1/lab/orders \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{
    "patientId": "patient-uuid-or-mrn",
    "facilityId": "facility-001",
    "orderingPhysicianId": "doctor-001",
    "preAuthorizationId": "AUTH-XYZ",
    "orderTimestamp": "2025-04-17T10:00:00Z",
    "priority": "ROUTINE",
    "diagnosisCodes": [
      {
        "system": "ICD-10",
        "code": "E11.9",
        "description": "Type 2 diabetes mellitus without complications"
      }
    ],
    "tests": [
      {
        "testCode": "CBC",
        "testName": "Complete Blood Count",
        "specimenType": "BLOOD",
        "clinicalNotes": "Fasting required"
      },
      {
        "testCode": "HBA1C",
        "testName": "Hemoglobin A1C",
        "specimenType": "BLOOD",
        "clinicalNotes": "3-month glycemic control"
      }
    ]
  }')

echo "$LAB_ORDER" | jq .
export ORDER_ID=$(echo "$LAB_ORDER" | jq -r '.id')
echo "Lab Order ID: $ORDER_ID"
```

Expected status: `201 Created`

Expected response:

```json
{ "id": "uuid" }
```

---

### 5.2 Priority Values

| Value | When to Use |
|-------|-------------|
| `ROUTINE` | Standard turnaround (24–48 h) |
| `URGENT` | Same-day results needed |
| `STAT` | Immediate — life-threatening situation |

---

### 5.3 Submit Lab Result

**`POST /api/v1/lab/results`**

```bash
RESULT=$(curl -s -X POST http://localhost:8082/api/v1/lab/results \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{
    \"orderId\": \"$ORDER_ID\",
    \"testCode\": \"CBC\",
    \"testName\": \"Complete Blood Count\",
    \"loincCode\": \"58410-2\",
    \"resultValue\": \"7.5\",
    \"resultUnit\": \"10^3/µL\",
    \"referenceRange\": \"4.5-11.0\",
    \"abnormalFlag\": \"NORMAL\",
    \"performedBy\": \"lab-tech-001\",
    \"performedAt\": \"2025-04-17T11:30:00Z\",
    \"notes\": \"Sample was clear, no haemolysis\"
  }")

echo "$RESULT" | jq .
export RESULT_ID=$(echo "$RESULT" | jq -r '.id')
echo "Result ID: $RESULT_ID"
```

Expected status: `201 Created`

Expected response shape:

```json
{
  "id": "uuid",
  "orderId": "uuid",
  "testCode": "CBC",
  "testName": "Complete Blood Count",
  "loincCode": "58410-2",
  "resultValue": "7.5",
  "resultUnit": "10^3/µL",
  "referenceRange": "4.5-11.0",
  "abnormalFlag": "NORMAL",
  "performedBy": "lab-tech-001",
  "performedAt": "2025-04-17T11:30:00Z",
  "status": "PRELIMINARY",
  "critical": false,
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

### 5.4 Submit a Critical Result

A result is flagged critical when `abnormalFlag` is `CRITICALLY_LOW` or `CRITICALLY_HIGH`.

```bash
curl -s -X POST http://localhost:8082/api/v1/lab/results \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{
    \"orderId\": \"$ORDER_ID\",
    \"testCode\": \"POTASSIUM\",
    \"testName\": \"Potassium\",
    \"loincCode\": \"2823-3\",
    \"resultValue\": \"6.8\",
    \"resultUnit\": \"mEq/L\",
    \"referenceRange\": \"3.5-5.0\",
    \"abnormalFlag\": \"CRITICALLY_HIGH\",
    \"performedBy\": \"lab-tech-001\",
    \"performedAt\": \"2025-04-17T11:45:00Z\"
  }" | jq .
```

Expected: `critical: true` in the response.

---

### 5.5 Get Lab Result by ID

**`GET /api/v1/lab/results/{id}`**

```bash
curl -s http://localhost:8082/api/v1/lab/results/$RESULT_ID \
  -H "$AUTH" | jq .
```

---

### 5.6 Get Results for an Order

**`GET /api/v1/lab/results/order/{orderId}`**

```bash
curl -s http://localhost:8082/api/v1/lab/results/order/$ORDER_ID \
  -H "$AUTH" | jq .
```

Expected: JSON array of `LabResultResponse` objects.

---

### 5.7 Get Critical Results

**`GET /api/v1/lab/results/critical?limit=<n>`**

- Default `limit`: 100
- Valid range: 1–500

```bash
# Default limit
curl -s http://localhost:8082/api/v1/lab/results/critical \
  -H "$AUTH" | jq .

# Custom limit
curl -s "http://localhost:8082/api/v1/lab/results/critical?limit=10" \
  -H "$AUTH" | jq .
```

Expected: JSON array where every entry has `"critical": true`.

---

### 5.8 AbnormalFlag Reference

| Value | Meaning | Critical? |
|-------|---------|-----------|
| `NORMAL` | Within reference range | No |
| `LOW` | Below normal | No |
| `HIGH` | Above normal | No |
| `CRITICALLY_LOW` | Dangerously low | **Yes** |
| `CRITICALLY_HIGH` | Dangerously high | **Yes** |
| `ABNORMAL` | Non-numeric abnormality | No |

---

### 5.9 ResultStatus Lifecycle

```
PRELIMINARY → FINAL → AMENDED
                    ↘ CORRECTED
```

Results start as `PRELIMINARY` when submitted.

---

## 6. Insurance Service

**Base URL:** `http://localhost:8083`

Uses the same JWT as the other services.

---

### 6.1 Create Insurance Policy

**`POST /api/v1/insurance/policies`**

```bash
POLICY=$(curl -s -X POST http://localhost:8083/api/v1/insurance/policies \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{
    \"patientId\": \"$PATIENT_ID\",
    \"policyNumber\": \"POL-2025-0001\",
    \"payerId\": \"BCBS-IL-001\",
    \"payerName\": \"Blue Cross Blue Shield of Illinois\",
    \"planName\": \"PPO Gold 80\",
    \"groupNumber\": \"GRP-44100\",
    \"subscriberId\": \"SUB-9900001\",
    \"subscriberName\": \"Jane Smith\",
    \"relationship\": \"SELF\",
    \"effectiveDate\": \"2025-01-01\",
    \"terminationDate\": \"2025-12-31\",
    \"copayAmount\": 30.00,
    \"deductibleAmount\": 1500.00,
    \"outOfPocketMax\": 5000.00
  }")

echo "$POLICY" | jq .
export POLICY_ID=$(echo "$POLICY" | jq -r '.policyId')
echo "Policy ID: $POLICY_ID"
```

Expected status: `201 Created`

Expected response shape:

```json
{
  "policyId": "uuid",
  "patientId": "uuid",
  "policyNumber": "POL-2025-0001",
  "payerId": "BCBS-IL-001",
  "payerName": "Blue Cross Blue Shield of Illinois",
  "planName": "PPO Gold 80",
  "groupNumber": "GRP-44100",
  "subscriberId": "SUB-9900001",
  "subscriberName": "Jane Smith",
  "relationship": "SELF",
  "effectiveDate": "2025-01-01",
  "terminationDate": "2025-12-31",
  "active": true,
  "copayAmount": 30.00,
  "deductibleAmount": 1500.00,
  "deductibleMet": 0.00,
  "outOfPocketMax": 5000.00,
  "outOfPocketMet": 0.00,
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

### 6.2 Get Policy by ID

**`GET /api/v1/insurance/policies/{policyId}`**

```bash
curl -s http://localhost:8083/api/v1/insurance/policies/$POLICY_ID \
  -H "$AUTH" | jq .
```

---

### 6.3 Get All Policies for a Patient

**`GET /api/v1/insurance/policies/patient/{patientId}`**

```bash
curl -s http://localhost:8083/api/v1/insurance/policies/patient/$PATIENT_ID \
  -H "$AUTH" | jq .
```

Expected: JSON array of `PolicyResponse` objects.

---

### 6.4 Relationship Values

| Value | Description |
|-------|-------------|
| `SELF` | Policyholder is the patient |
| `SPOUSE` | Policyholder is the patient's spouse |
| `CHILD` | Policyholder is a parent |
| `PARENT` | Policyholder is the patient's parent |
| `DOMESTIC_PARTNER` | Domestic partnership |
| `OTHER` | Other legal relationship |

---

### 6.5 Dependent Policy (CHILD relationship)

```bash
curl -s -X POST http://localhost:8083/api/v1/insurance/policies \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{
    \"patientId\": \"$PATIENT_ID\",
    \"policyNumber\": \"POL-2025-0002\",
    \"payerId\": \"AETNA-001\",
    \"payerName\": \"Aetna\",
    \"planName\": \"HMO Silver\",
    \"groupNumber\": \"GRP-22200\",
    \"subscriberId\": \"SUB-7700001\",
    \"subscriberName\": \"Robert Smith\",
    \"relationship\": \"CHILD\",
    \"effectiveDate\": \"2025-01-01\",
    \"terminationDate\": \"2025-12-31\",
    \"copayAmount\": 20.00,
    \"deductibleAmount\": 2000.00,
    \"outOfPocketMax\": 6000.00
  }" | jq .
```

---

## 7. Cross-Service Flows

### 7.1 Full Patient Onboarding Flow

Run steps in sequence. Each step depends on the previous.

```bash
# Step 1 — Authenticate
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"doctor1","password":"password123"}' | jq -r '.jwt')
AUTH="Authorization: Bearer $TOKEN"

# Step 2 — Register patient
PATIENT=$(curl -s -X POST http://localhost:8081/api/v1/patients \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "mrn":"MRN-100","ssn":"999-00-1234",
    "firstName":"Alice","lastName":"Wong",
    "dateOfBirth":"1978-03-22",
    "email":"alice.wong@example.com",
    "phoneNumber":"312-555-0200",
    "address":"500 Lake Shore Dr, Chicago, IL 60601",
    "insuranceProvider":"UnitedHealth","insurancePolicyNumber":"UHC-555001"
  }')
PATIENT_ID=$(echo "$PATIENT" | jq -r '.id')
PATIENT_SSN="999-00-1234"
echo "Patient: $PATIENT_ID"

# Step 3 — Add insurance policy
POLICY=$(curl -s -X POST http://localhost:8083/api/v1/insurance/policies \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{
    \"patientId\":\"$PATIENT_ID\",
    \"policyNumber\":\"UHC-555001\",
    \"payerId\":\"UHC-001\",\"payerName\":\"UnitedHealth\",
    \"planName\":\"Choice Plus\",\"groupNumber\":\"GRP-10001\",
    \"subscriberId\":\"SUB-555001\",\"subscriberName\":\"Alice Wong\",
    \"relationship\":\"SELF\",
    \"effectiveDate\":\"2025-01-01\",\"terminationDate\":\"2025-12-31\",
    \"copayAmount\":25.00,\"deductibleAmount\":1000.00,\"outOfPocketMax\":4000.00
  }")
echo "Policy: $(echo $POLICY | jq -r '.policyId')"

# Step 4 — Add a medical record
RECORD=$(curl -s -X POST http://localhost:8081/api/v1/medical-records \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{
    \"patientId\":\"$PATIENT_ID\",
    \"diagnosis\":\"Hypertension\",
    \"treatment\":\"Lisinopril 10mg daily\",
    \"date\":\"2025-04-17\"
  }")
echo "Record: $(echo $RECORD | jq -r '.recordId')"

# Step 5 — Order a lab test (triggers Kafka event → lab service)
curl -s -X POST "http://localhost:8081/api/v1/patients/$PATIENT_SSN/order-labs/" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"testCode":"BMP","priority":"ROUTINE","doctorId":"doctor-001","notes":"Check electrolytes"}'
echo "Lab order placed (async)"

# Step 6 — Wait for Kafka consumption, then query lab orders
sleep 5
curl -s "http://localhost:8082/api/v1/lab/results/critical" -H "$AUTH" | jq .
```

---

### 7.2 Kafka Event Verification

Verify that events flow between services using `kcat` (formerly `kafkacat`).

**Listen to `patient-events` topic (lab order trigger):**

```bash
kcat -b localhost:9092 -t patient-events -C -e -o beginning | jq .
```

**Listen to `lab-events` topic (results available):**

```bash
kcat -b localhost:9092 -t lab-events -C -e -o beginning | jq .
```

**List all topics:**

```bash
kcat -b localhost:9092 -L | grep topic
```

**Expected `LabTestOrderedEvent` on `patient-events`:**

```json
{
  "eventId": "uuid",
  "eventType": "lab.test.ordered.v1",
  "timestamp": 1713340800000,
  "source": "patient-service",
  "order": {
    "orderId": "uuid",
    "patientId": "patient-ssn",
    "doctorId": "doctor-001",
    "testCode": "BMP",
    "priority": "ROUTINE",
    "notes": "Check electrolytes"
  },
  "patientSnapshot": {
    "mrn": "MRN-100",
    "firstName": "Alice",
    "lastName": "Wong",
    "dateOfBirth": "1978-03-22"
  }
}
```

**Expected `LabResultsAvailableEvent` on `lab-events`:**

```json
{
  "eventId": "uuid",
  "eventType": "lab.results.available.v1",
  "timestamp": 1713344400000,
  "source": "lab-service",
  "order": {
    "orderId": "uuid",
    "patientId": "patient-ssn",
    "orderingPhysicianId": "doctor-001",
    "facilityId": "facility-001",
    "orderTimestamp": "2025-04-17T10:00:00Z"
  },
  "results": [
    {
      "resultId": "uuid",
      "testCode": "BMP",
      "testName": "Basic Metabolic Panel",
      "resultValue": "98",
      "resultUnit": "mg/dL",
      "referenceRange": "70-100",
      "abnormalFlag": "NORMAL",
      "status": "PRELIMINARY",
      "critical": false
    }
  ],
  "hasCriticalResults": false
}
```

---

## 8. Error & Validation Scenarios

All services return the same error envelope:

```json
{
  "timestamp": "2025-04-17T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Patient not found with id: <uuid>",
  "path": "/api/v1/patients/<uuid>"
}
```

---

### 8.1 404 — Resource Not Found

```bash
# Non-existent patient
curl -s http://localhost:8081/api/v1/patients/00000000-0000-0000-0000-000000000000 \
  -H "$AUTH" | jq .

# Non-existent medical record
curl -s http://localhost:8081/api/v1/medical-records/00000000-0000-0000-0000-000000000000 \
  -H "$AUTH" | jq .

# Non-existent lab result
curl -s http://localhost:8082/api/v1/lab/results/00000000-0000-0000-0000-000000000000 \
  -H "$AUTH" | jq .

# Non-existent insurance policy
curl -s http://localhost:8083/api/v1/insurance/policies/00000000-0000-0000-0000-000000000000 \
  -H "$AUTH" | jq .
```

Expected status: `404 Not Found`

---

### 8.2 400 — Validation Failures

**Missing required fields (patient):**

```bash
curl -s -X POST http://localhost:8081/api/v1/patients \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"lastName": "Smith"}' | jq .
```

Expected: `400 Bad Request` with message describing which field is missing.

**Invalid email:**

```bash
curl -s -X POST http://localhost:8081/api/v1/patients \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "mrn": "MRN-002",
    "firstName": "Bob",
    "lastName": "Jones",
    "dateOfBirth": "1990-01-01",
    "email": "not-an-email"
  }' | jq .
```

Expected: `400 Bad Request`

**Future date of birth:**

```bash
curl -s -X POST http://localhost:8081/api/v1/patients \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "mrn": "MRN-003",
    "firstName": "Future",
    "lastName": "Person",
    "dateOfBirth": "2099-01-01",
    "email": "future@example.com"
  }' | jq .
```

Expected: `400 Bad Request` (dateOfBirth must be in the past)

**Lab order with no tests:**

```bash
curl -s -X POST http://localhost:8082/api/v1/lab/orders \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "patientId": "patient-001",
    "facilityId": "fac-001",
    "orderingPhysicianId": "doc-001",
    "priority": "ROUTINE",
    "tests": []
  }' | jq .
```

Expected: `400 Bad Request` ("At least one test is required")

**Invalid priority enum:**

```bash
curl -s -X POST http://localhost:8082/api/v1/lab/orders \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "patientId": "patient-001",
    "facilityId": "fac-001",
    "orderingPhysicianId": "doc-001",
    "priority": "SUPER_URGENT",
    "tests": [{"testCode":"CBC","testName":"CBC","specimenType":"BLOOD"}]
  }' | jq .
```

Expected: `400 Bad Request`

**Insurance policy — missing required fields:**

```bash
curl -s -X POST http://localhost:8083/api/v1/insurance/policies \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "patientId": "00000000-0000-0000-0000-000000000001",
    "planName": "Gold Plan"
  }' | jq .
```

Expected: `400 Bad Request` (policyNumber, payerId, payerName, subscriberId, relationship, effectiveDate are required)

---

### 8.3 409 — Duplicate Resource

**Duplicate patient MRN:**

```bash
# Create a patient (first time should succeed)
curl -s -X POST http://localhost:8081/api/v1/patients \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "mrn":"MRN-DUP","firstName":"Dup","lastName":"Test",
    "dateOfBirth":"1990-01-01","email":"dup@example.com"
  }' | jq .

# Second request with same MRN → should 409
curl -s -X POST http://localhost:8081/api/v1/patients \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "mrn":"MRN-DUP","firstName":"Another","lastName":"Test",
    "dateOfBirth":"1991-01-01","email":"another@example.com"
  }' | jq .
```

Expected: `409 Conflict`

**Duplicate insurance policy:**

```bash
# Create same policy twice
BODY="{\"patientId\":\"$PATIENT_ID\",\"policyNumber\":\"POL-DUP-001\",
  \"payerId\":\"PAY-001\",\"payerName\":\"Test Payer\",
  \"subscriberId\":\"SUB-DUP\",\"relationship\":\"SELF\",
  \"effectiveDate\":\"2025-01-01\"}"

curl -s -X POST http://localhost:8083/api/v1/insurance/policies \
  -H "Content-Type: application/json" -H "$AUTH" -d "$BODY" | jq .

# Second identical request
curl -s -X POST http://localhost:8083/api/v1/insurance/policies \
  -H "Content-Type: application/json" -H "$AUTH" -d "$BODY" | jq .
```

Expected second response: `409 Conflict`

---

## 9. Security & Authorization

### 9.1 Role-Based Access Matrix

| Endpoint Pattern | ADMIN | DOCTOR | NURSE | LAB_TECH |
|-----------------|-------|--------|-------|----------|
| `POST /api/v1/patients/**` | ✅ | ✅ | ✅ | ❌ |
| `PUT /api/v1/patients/**` | ✅ | ✅ | ✅ | ❌ |
| `GET /api/v1/patients/**` | ✅ | ✅ | ✅ | ✅ |
| `DELETE /api/v1/patients/**` | ✅ | ❌ | ❌ | ❌ |
| `POST /api/v1/medical-records/**` | ✅ | ✅ | ✅ | ❌ |
| `PUT /api/v1/medical-records/**` | ✅ | ✅ | ✅ | ❌ |
| `GET /api/v1/medical-records/**` | ✅ | ✅ | ✅ | ✅ |
| `POST /api/v1/patients/{ssn}/order-labs/**` | ✅ | ✅ | ❌ | ❌ |

### 9.2 Test Insufficient Permissions

```bash
# Get a LAB_TECH token (if seeded)
LAB_TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"labtech1","password":"password123"}' | jq -r '.jwt')
LAB_AUTH="Authorization: Bearer $LAB_TOKEN"

# LAB_TECH trying to create a patient → should 403
curl -s -X POST http://localhost:8081/api/v1/patients \
  -H "Content-Type: application/json" -H "$LAB_AUTH" \
  -d '{"mrn":"X","firstName":"A","lastName":"B","dateOfBirth":"1990-01-01","email":"a@b.com"}' \
  -w "\nHTTP Status: %{http_code}\n"

# NURSE trying to delete a medical record → should 403
NURSE_TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"nurse1","password":"password123"}' | jq -r '.jwt')
curl -s -X DELETE http://localhost:8081/api/v1/medical-records/$RECORD_ID \
  -H "Authorization: Bearer $NURSE_TOKEN" \
  -w "\nHTTP Status: %{http_code}\n"
```

Expected status: `403 Forbidden`

### 9.3 Public Endpoints (no auth required)

```bash
# Health checks — always accessible
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8082/actuator/health
curl -s http://localhost:8083/actuator/health

# Auth endpoint — must not require a token
curl -s -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"x","password":"y"}'
```

---

## 10. Observability

### 10.1 Health & Info Endpoints

```bash
# Detailed health (all services)
for port in 8081 8082 8083; do
  echo "=== Port $port ==="
  curl -s http://localhost:$port/actuator/health | jq .
done

# App info
curl -s http://localhost:8081/actuator/info | jq .
```

### 10.2 Metrics

```bash
# Raw Prometheus metrics
curl -s http://localhost:8081/actuator/prometheus | grep http_server_requests | head -10

# Lab order counter
curl -s http://localhost:8082/actuator/prometheus | grep meditrack_lab_orders_created

# Metrics list
curl -s http://localhost:8081/actuator/metrics | jq '.names[]' | head -20
```

### 10.3 Distributed Tracing — Jaeger

1. Open **Jaeger UI**: [http://localhost:16686](http://localhost:16686)
2. Select service (e.g., `patient-service`) from the dropdown
3. Click **Find Traces**
4. Each trace shows the full call graph across services

### 10.4 Metrics Dashboard — Grafana

1. Open **Grafana**: [http://localhost:3000](http://localhost:3000)  
   Default credentials: `admin` / `admin`
2. Import or browse dashboards for `patient-service`, `lab-service`, `insurance-service`
3. Key panels: request rate, error rate, p99 latency, cache hit ratio

### 10.5 Prometheus

Direct query interface: [http://localhost:9090](http://localhost:9090)

Useful PromQL queries:

```promql
# HTTP request rate per service
rate(http_server_requests_seconds_count[1m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# Lab orders created
meditrack_lab_orders_created_total

# p99 latency
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

---

## 11. Automated Tests

### 11.1 Run All Tests

```bash
# Patient service
cd services/patient-service
./mvnw test

# Lab service
cd services/labrotary-service
./mvnw test

# Insurance service
cd services/insurance-service
./mvnw test
```

### 11.2 Run a Single Test Class

```bash
./mvnw test -Dtest=PatientControllerTest
./mvnw test -Dtest=LabOrderApplicationServiceTest
./mvnw test -Dtest=InsurancePolicyApplicationServiceTest
```

### 11.3 Test Patterns by Layer

**Controller Tests (`@WebMvcTest`)** — `PatientControllerTest`, `MedicalRecordControllerTest`

```java
// Security disabled for controller slice tests
@WebMvcTest(
  excludeAutoConfiguration = {
    SecurityAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class
  }
)
// Uses MockMvc, Mockito mocks for use-case layer
// Assertions: status().isCreated(), jsonPath("$.id").isNotEmpty()
```

**Service / Application Layer Tests (`@ExtendWith(MockitoExtension.class)`)** — `InsurancePolicyApplicationServiceTest`, `LabOrderApplicationServiceTest`

```java
// Pure unit tests — no Spring context loaded
// Uses Mockito mocks for repositories and mappers
// Uses ArgumentCaptor to verify outbox event payloads
// Uses AssertJ: assertThat(result).isNotNull()
//               assertThat(result.getStatus()).isEqualTo(OrderStatus.RECEIVED)
// Uses SimpleMeterRegistry to verify metric counters:
//   assertThat(meterRegistry.counter("meditrack_lab_orders_created_total").count())
//     .isEqualTo(1.0);
```

### 11.4 Test Coverage Report

```bash
cd services/patient-service
./mvnw test jacoco:report
# Open: target/site/jacoco/index.html
```

### 11.5 Integration Testing Checklist

When the full Docker stack is running, run through the following flow to verify end-to-end integration:

- [ ] `POST /api/v1/auth/authenticate` — returns a JWT
- [ ] `POST /api/v1/patients` — creates patient, returns 201 with UUID
- [ ] `GET /api/v1/patients/{id}` — returns same patient
- [ ] `POST /api/v1/medical-records` — creates record linked to patient
- [ ] `GET /api/v1/medical-records/patient/{patientId}` — returns the record
- [ ] `POST /api/v1/patients/{ssn}/order-labs/` — returns 200
- [ ] (wait 5 s) `GET /api/v1/lab/results/critical` — no error (Kafka consumed)
- [ ] `POST /api/v1/lab/orders` — direct order creation returns `{ "id": "uuid" }`
- [ ] `POST /api/v1/lab/results` — submits result, returns 201
- [ ] `GET /api/v1/lab/results/order/{orderId}` — returns the result
- [ ] `POST /api/v1/insurance/policies` — creates policy, returns 201
- [ ] `GET /api/v1/insurance/policies/patient/{patientId}` — returns the policy
- [ ] `GET /actuator/health` on all three services — all return `"status": "UP"`
- [ ] Jaeger UI shows traces for the requests made above

---

## Quick Reference

### Service URLs

| Service | Base URL | Auth endpoint |
|---------|---------|---------------|
| Patient | `http://localhost:8081` | `POST /api/v1/auth/authenticate` |
| Lab | `http://localhost:8082` | *(uses patient-service JWT)* |
| Insurance | `http://localhost:8083` | *(uses patient-service JWT)* |

### Error Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Created |
| 204 | No Content (delete success) |
| 400 | Validation error |
| 401 | Missing / invalid / expired JWT |
| 403 | Authenticated but insufficient role |
| 404 | Resource not found |
| 409 | Duplicate resource |
| 500 | Internal server error |

### One-liner Environment Setup

```bash
export TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"doctor1","password":"password123"}' | jq -r '.jwt')
export AUTH="Authorization: Bearer $TOKEN"
echo "Ready. Token expires in 24h."
```
