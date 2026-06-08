// --- Patient Service ---
export interface PatientResponse {
  id: string;
  mrn: string;
  ssn: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  email: string;
  phoneNumber: string;
  address: string;
  insuranceProvider: string;
  insurancePolicyNumber: string;
  medicalHistory: MedicalRecordResponse[];
}

export interface CreatePatientRequest {
  mrn: string;
  ssn: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  email: string;
  phoneNumber: string;
  address: string;
  insuranceProvider: string;
  insurancePolicyNumber: string;
}

export interface UpdatePatientRequest extends CreatePatientRequest {}

export interface MedicalRecordResponse {
  recordId: string;
  diagnosis: string;
  treatment: string;
  date: string;
}

export interface CreateMedicalRecordRequest {
  patientId: string;
  diagnosis: string;
  treatment: string;
  date: string;
}

export interface UpdateMedicalRecordRequest {
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
export type AbnormalFlag =
  | "NORMAL"
  | "LOW"
  | "HIGH"
  | "CRITICALLY_LOW"
  | "CRITICALLY_HIGH"
  | "ABNORMAL";
export type ResultStatus = "PRELIMINARY" | "FINAL" | "CORRECTED" | "AMENDED";

export interface LabOrderRequest {
  patientId: string;
  facilityId: string;
  orderingPhysicianId: string;
  preAuthorizationId?: string;
  orderTimestamp: string;
  priority: Priority;
  diagnosisCodes: DiagnosisCodeDto[];
  tests: TestInfoDto[];
}

export interface DiagnosisCodeDto {
  system: string;
  code: string;
  description: string;
}

export interface TestInfoDto {
  testCode: string;
  testName: string;
  specimenType: string;
  clinicalNotes?: string;
}

export interface LabOrderResponse {
  id: string;
}

export interface LabResultRequest {
  orderId: string;
  testCode: string;
  testName: string;
  loincCode: string;
  resultValue: string;
  resultUnit: string;
  referenceRange: string;
  abnormalFlag: AbnormalFlag;
  performedBy: string;
  performedAt: string;
  notes?: string;
}

export interface LabResultResponse {
  id: string;
  orderId: string;
  testCode: string;
  testName: string;
  loincCode: string;
  resultValue: string;
  resultUnit: string;
  referenceRange: string;
  abnormalFlag: AbnormalFlag;
  performedBy: string;
  performedAt: string;
  verifiedBy?: string;
  verifiedAt?: string;
  status: ResultStatus;
  notes?: string;
  critical: boolean;
  createdAt: string;
  updatedAt: string;
}

// --- Doctor Service ---
export type Specialization =
  | "GENERAL_MEDICINE" | "CARDIOLOGY" | "NEUROLOGY" | "ORTHOPEDICS"
  | "PEDIATRICS" | "DERMATOLOGY" | "GYNECOLOGY" | "OPHTHALMOLOGY"
  | "ENT" | "PSYCHIATRY" | "ONCOLOGY" | "ENDOCRINOLOGY"
  | "GASTROENTEROLOGY" | "NEPHROLOGY" | "PULMONOLOGY" | "UROLOGY"
  | "RADIOLOGY" | "ANESTHESIOLOGY" | "EMERGENCY_MEDICINE" | "SURGERY";

export const SPECIALIZATIONS: Specialization[] = [
  "GENERAL_MEDICINE", "CARDIOLOGY", "NEUROLOGY", "ORTHOPEDICS",
  "PEDIATRICS", "DERMATOLOGY", "GYNECOLOGY", "OPHTHALMOLOGY",
  "ENT", "PSYCHIATRY", "ONCOLOGY", "ENDOCRINOLOGY",
  "GASTROENTEROLOGY", "NEPHROLOGY", "PULMONOLOGY", "UROLOGY",
  "RADIOLOGY", "ANESTHESIOLOGY", "EMERGENCY_MEDICINE", "SURGERY",
];

export function formatSpecialization(s: string): string {
  return s.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

export interface DoctorResponse {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone: string;
  specialization: Specialization;
  qualifications: string;
  yearsOfExperience: number;
  bio: string;
  active: boolean;
}

export interface AvailabilitySlotResponse {
  id: string;
  doctorId: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  available: boolean;
}

// --- Appointment Service ---
export type AppointmentStatus =
  | "PENDING" | "CONFIRMED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED" | "NO_SHOW";
export type AppointmentType =
  | "FIRST_VISIT" | "FOLLOW_UP" | "EMERGENCY" | "TELECONSULTATION";

export interface AppointmentResponse {
  id: string;
  patientId: string;
  doctorId: string;
  status: AppointmentStatus;
  type: AppointmentType;
  reasonForVisit: string;
  notes: string;
  scheduledAt: string;
  actualStartAt: string | null;
  actualEndAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BookAppointmentRequest {
  patientId: string;
  doctorId: string;
  type: AppointmentType;
  reasonForVisit?: string;
  scheduledAt: string;
}

// --- Prescription Service ---
export interface PrescriptionMedicationResponse {
  id: string;
  medicationName: string;
  genericName: string;
  dosage: string;
  frequency: string;
  duration: string;
  route: string;
  instructions: string;
}

export interface PrescriptionLabOrderResponse {
  id: string;
  testCode: string;
  testName: string;
  clinicalIndication: string;
  urgency: string;
}

export interface PrescriptionResponse {
  id: string;
  patientId: string;
  doctorId: string;
  appointmentId: string | null;
  status: string;
  consultationNotes: string;
  diagnosisCodes: string;
  medications: PrescriptionMedicationResponse[];
  labOrders: PrescriptionLabOrderResponse[];
  issuedAt: string | null;
  validUntil: string | null;
  createdAt: string;
}

export interface MedicationDraft {
  medicationName: string;
  genericName: string;
  dosage: string;
  frequency: string;
  duration: string;
  route: string;
  instructions: string;
}

export interface PrescriptionLabOrderDraft {
  testCode: string;
  testName: string;
  clinicalIndication: string;
  urgency: string;
}

export interface CreatePrescriptionRequest {
  patientId: string;
  doctorId: string;
  appointmentId?: string;
  consultationNotes?: string;
  diagnosisCodes?: string;
  medications?: MedicationDraft[];
  labOrders?: PrescriptionLabOrderDraft[];
}

// --- Insurance Service ---
export type Relationship =
  | "SELF"
  | "SPOUSE"
  | "CHILD"
  | "PARENT"
  | "DOMESTIC_PARTNER"
  | "OTHER";

export interface CreatePolicyRequest {
  patientId: string;
  policyNumber: string;
  payerId: string;
  payerName: string;
  planName: string;
  groupNumber: string;
  subscriberId: string;
  subscriberName: string;
  relationship: Relationship;
  effectiveDate: string;
  terminationDate: string;
  copayAmount: number;
  deductibleAmount: number;
  outOfPocketMax: number;
}

export interface PolicyResponse {
  policyId: string;
  patientId: string;
  policyNumber: string;
  payerId: string;
  payerName: string;
  planName: string;
  groupNumber: string;
  subscriberId: string;
  subscriberName: string;
  relationship: Relationship;
  effectiveDate: string;
  terminationDate?: string;
  active: boolean;
  copayAmount: number;
  deductibleAmount: number;
  deductibleMet: number;
  outOfPocketMax: number;
  outOfPocketMet: number;
  createdAt: string;
  updatedAt: string;
}
