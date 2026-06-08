# MediTrack Next.js UI — Implementation Guide

## Overview

A Next.js 14+ frontend (App Router) that talks directly to the three backend microservices over HTTP. Auth is handled by the patient-service JWT endpoint; tokens are stored in an `httpOnly` cookie via a Next.js API route proxy to avoid CORS and avoid exposing tokens to JavaScript.

---

## Project Setup

```bash
npx create-next-app@latest meditrack-ui --typescript --tailwind --eslint --app --src-dir
cd meditrack-ui
npm install axios react-hook-form zod @hookform/resolvers
npm install @radix-ui/react-dialog @radix-ui/react-select @radix-ui/react-toast lucide-react
npm install date-fns
```

### Directory structure

```
meditrack-ui/
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx                        # redirect to /patients or /login
│   │   ├── login/
│   │   │   └── page.tsx
│   │   ├── patients/
│   │   │   ├── page.tsx                    # search + list
│   │   │   ├── new/page.tsx
│   │   │   └── [id]/
│   │   │       ├── page.tsx                # patient detail + timeline
│   │   │       ├── records/
│   │   │       │   ├── new/page.tsx
│   │   │       │   └── [recordId]/page.tsx
│   │   │       └── order-labs/page.tsx
│   │   ├── lab/
│   │   │   ├── orders/
│   │   │   │   ├── page.tsx                # list/create orders
│   │   │   │   └── [orderId]/
│   │   │   │       └── page.tsx            # order results
│   │   │   └── results/
│   │   │       ├── critical/page.tsx
│   │   │       └── [id]/page.tsx
│   │   ├── insurance/
│   │   │   ├── page.tsx                    # look up by patientId
│   │   │   └── new/page.tsx
│   │   └── api/
│   │       ├── auth/
│   │       │   ├── login/route.ts          # proxies POST /api/v1/auth/authenticate
│   │       │   └── logout/route.ts         # clears cookie
│   │       ├── patients/
│   │       │   └── [...path]/route.ts      # proxy for patient-service
│   │       ├── lab/
│   │       │   └── [...path]/route.ts      # proxy for labrotary-service
│   │       └── insurance/
│   │           └── [...path]/route.ts      # proxy for insurance-service
│   ├── lib/
│   │   ├── api.ts                          # axios instances per service
│   │   ├── auth.ts                         # token helpers
│   │   └── types.ts                        # TS types matching backend DTOs
│   ├── components/
│   │   ├── ui/                             # shadcn-style primitives
│   │   ├── PatientCard.tsx
│   │   ├── MedicalRecordList.tsx
│   │   ├── LabResultBadge.tsx
│   │   └── InsurancePolicyCard.tsx
│   └── middleware.ts                       # redirect unauthenticated requests
```

---

## Environment Variables

Create `.env.local` in the Next.js project root:

```env
# Backend service base URLs (change for docker / prod)
PATIENT_SERVICE_URL=http://localhost:8081
LAB_SERVICE_URL=http://localhost:8082
INSURANCE_SERVICE_URL=http://localhost:8083

# Used by middleware to sign/verify the session cookie
JWT_SECRET=your-jwt-secret-here          # must match backend JWT_SECRET
```

> In Docker Compose the service names resolve as hostnames, so use
> `http://patient-service:8081`, `http://labrotary-service:8082`, `http://insurance-service:8083`.

---

## API Proxy Routes

Each API route in `src/app/api/` strips the Next.js prefix and forwards the request to the relevant service, attaching the JWT from the cookie as `Authorization: Bearer <token>`.

**Pattern — `src/app/api/patients/[...path]/route.ts`:**

```ts
import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const BASE = process.env.PATIENT_SERVICE_URL;

async function proxy(req: NextRequest, path: string[]) {
  const token = cookies().get("token")?.value;
  const url = `${BASE}/api/v1/patients/${path.join("/")}${req.nextUrl.search}`;
  const res = await fetch(url, {
    method: req.method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: req.method !== "GET" && req.method !== "DELETE"
      ? await req.text()
      : undefined,
  });
  const data = await res.text();
  return new NextResponse(data, { status: res.status, headers: { "Content-Type": "application/json" } });
}

export const GET = (req: NextRequest, { params }: { params: { path: string[] } }) => proxy(req, params.path);
export const POST = (req: NextRequest, { params }: { params: { path: string[] } }) => proxy(req, params.path);
export const PUT = (req: NextRequest, { params }: { params: { path: string[] } }) => proxy(req, params.path);
export const DELETE = (req: NextRequest, { params }: { params: { path: string[] } }) => proxy(req, params.path);
```

Repeat the same pattern for `/api/lab/[...path]` (pointing to `LAB_SERVICE_URL/api/v1/lab/`) and `/api/insurance/[...path]` (pointing to `INSURANCE_SERVICE_URL/api/v1/insurance/`).

**Login route — `src/app/api/auth/login/route.ts`:**

```ts
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  const body = await req.json();
  const res = await fetch(`${process.env.PATIENT_SERVICE_URL}/api/v1/auth/authenticate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) return NextResponse.json({ error: "Invalid credentials" }, { status: 401 });
  const { jwt } = await res.json();
  const response = NextResponse.json({ ok: true });
  response.cookies.set("token", jwt, { httpOnly: true, path: "/", sameSite: "lax", maxAge: 86400 });
  return response;
}
```

---

## Middleware (auth guard)

**`src/middleware.ts`:**

```ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(req: NextRequest) {
  const token = req.cookies.get("token");
  if (!token && !req.nextUrl.pathname.startsWith("/login")) {
    return NextResponse.redirect(new URL("/login", req.url));
  }
  return NextResponse.next();
}

export const config = { matcher: ["/((?!_next|favicon.ico|api/auth).*)"] };
```

---

## TypeScript Types (`src/lib/types.ts`)

```ts
// --- Patient Service ---
export interface PatientResponse {
  id: string;
  mrn: string;
  ssn: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;       // ISO date string
  email: string;
  phoneNumber: string;
  address: string;
  insuranceProvider: string;
  insurancePolicyNumber: string;
  medicalHistory: MedicalRecordResponse[];
}

export interface CreatePatientRequest {
  mrn: string; ssn: string; firstName: string; lastName: string;
  dateOfBirth: string; email: string; phoneNumber: string; address: string;
  insuranceProvider: string; insurancePolicyNumber: string;
}

export interface MedicalRecordResponse {
  recordId: string;
  diagnosis: string;
  treatment: string;
  date: string;
}

export interface PatientTimelineResponse {
  patientId: string;
  timeline: MedicalRecordResponse[];
}

export interface OrderLabTestRequest {
  testCode: string;
  priority: "ROUTINE" | "URGENT" | "STAT";
  doctorId: string;
  notes?: string;
}

// --- Lab Service ---
export type Priority = "ROUTINE" | "URGENT" | "STAT";
export type AbnormalFlag = "NORMAL" | "LOW" | "HIGH" | "CRITICALLY_LOW" | "CRITICALLY_HIGH" | "ABNORMAL";
export type ResultStatus = "PRELIMINARY" | "FINAL" | "CORRECTED" | "AMENDED";

export interface LabOrderRequest {
  patientId: string;
  facilityId: string;
  orderingPhysicianId: string;
  preAuthorizationId?: string;
  orderTimestamp: string;     // ISO OffsetDateTime
  priority: Priority;
  diagnosisCodes: DiagnosisCodeDto[];
  tests: TestInfoDto[];
}

export interface DiagnosisCodeDto { system: string; code: string; description: string; }
export interface TestInfoDto { testCode: string; testName: string; specimenType: string; clinicalNotes?: string; }

export interface LabOrderResponse { id: string; }

export interface LabResultResponse {
  id: string; orderId: string; testCode: string; testName: string;
  loincCode: string; resultValue: string; resultUnit: string;
  referenceRange: string; abnormalFlag: AbnormalFlag;
  performedBy: string; performedAt: string;
  verifiedBy?: string; verifiedAt?: string;
  status: ResultStatus; notes?: string; critical: boolean;
  createdAt: string; updatedAt: string;
}

// --- Insurance Service ---
export type Relationship = "SELF" | "SPOUSE" | "CHILD" | "PARENT" | "DOMESTIC_PARTNER" | "OTHER";

export interface CreatePolicyRequest {
  patientId: string; policyNumber: string; payerId: string; payerName: string;
  planName: string; groupNumber: string; subscriberId: string; subscriberName: string;
  relationship: Relationship; effectiveDate: string; terminationDate: string;
  copayAmount: number; deductibleAmount: number; outOfPocketMax: number;
}

export interface PolicyResponse {
  policyId: string; patientId: string; policyNumber: string;
  payerId: string; payerName: string; planName: string;
  groupNumber: string; subscriberId: string; subscriberName: string;
  relationship: Relationship; effectiveDate: string; terminationDate?: string;
  active: boolean; copayAmount: number; deductibleAmount: number;
  deductibleMet: number; outOfPocketMax: number; outOfPocketMet: number;
  createdAt: string; updatedAt: string;
}
```

---

## Pages to Build

### `/login`
- Form: `username` + `password`
- `POST /api/auth/login` (Next.js proxy)
- On success → redirect to `/patients`

### `/patients`
- Search bar → `GET /api/patients/search?query=...`
- List of `PatientCard` components
- Button → `/patients/new`

### `/patients/new`
- `react-hook-form` + `zod` form for `CreatePatientRequest`
- `POST /api/patients/`

### `/patients/[id]`
- `GET /api/patients/{id}` for demographics
- `GET /api/patients/search/{id}/timeline` for the timeline
- Tabs: **Overview** | **Medical Records** | **Order Labs**
- Link to `/insurance?patientId={id}` to view policies

### `/patients/[id]/records/new`
- Form for `CreateMedicalRecordRequest`
- `POST /api/patients/medical-records/`  
  *(Note: the actual path is `/api/v1/medical-records/` on patient-service — proxy accordingly)*

### `/patients/[id]/order-labs`
- Form for `OrderLabTestRequest` (testCode, priority, doctorId, notes)
- `POST /api/patients/{ssn}/order-labs/`
- Requires patient SSN — fetch from patient detail first

### `/lab/orders`
- Create full `LabOrderRequest` (multi-step form recommended)
- `POST /api/lab/orders/`

### `/lab/orders/[orderId]`
- `GET /api/lab/results/order/{orderId}`
- Render each result with `LabResultBadge` (color-coded by `abnormalFlag`)

### `/lab/results/critical`
- `GET /api/lab/results/critical?limit=100`
- Table sorted by `performedAt`, highlight `critical: true` rows in red

### `/insurance`
- Search by `patientId` query param
- `GET /api/insurance/policies/patient/{patientId}`
- Button → `/insurance/new`

### `/insurance/new`
- Form for `CreatePolicyRequest`
- `POST /api/insurance/policies/`

---

## Key Implementation Notes

### CORS
The proxy routes in `src/app/api/` mean the browser never calls the backend directly — no CORS config needed on the services.

### Token expiry
The JWT TTL is 24 hours (matches the `httpOnly` cookie `maxAge`). Add a 401 interceptor in your fetch wrapper that redirects to `/login` on expiry.

### Medical records base path
The `MedicalRecordController` lives at `/api/v1/medical-records/` (not nested under `/patients/`). Your lab proxy catches `/api/v1/lab/…`; add a separate `/api/medical-records/[...path]/route.ts` proxy for this controller.

### SSN for lab ordering
`PatientLabOrderController` takes `ssn` as a path variable, not `id`. When building the order-labs form, ensure you fetch (or already have) the patient's SSN from the `PatientResponse`.

### Kafka-triggered lab results
When a lab order is created via `POST /api/patients/{ssn}/order-labs`, the actual `LabOrder` is created asynchronously by the lab service after consuming the Kafka event. Poll or add a short delay before navigating to the order detail page.

### Date formatting
Backend uses ISO dates (`LocalDate` → `"2025-04-17"`) and `OffsetDateTime` strings. Use `date-fns/format` for display.

---

## Running Locally

1. Start the backend stack: `docker compose up -d` from the repo root.
2. In `meditrack-ui/`:
   ```bash
   cp .env.local.example .env.local   # fill in secrets
   npm run dev
   ```
3. The UI runs on `http://localhost:3000`.
4. Log in with a user registered in the patient-service (the auth endpoint is `/api/v1/auth/authenticate`).

---

## Recommended Libraries

| Purpose | Package |
|---------|---------|
| Forms + validation | `react-hook-form` + `zod` + `@hookform/resolvers` |
| UI components | `shadcn/ui` (Tailwind-based, copy-paste) |
| Icons | `lucide-react` |
| Date helpers | `date-fns` |
| HTTP (server components) | native `fetch` |
| HTTP (client components) | `axios` or native `fetch` |
| Toast notifications | `sonner` or `@radix-ui/react-toast` |
