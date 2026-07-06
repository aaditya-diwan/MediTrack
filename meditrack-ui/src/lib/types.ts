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

export type UpdatePatientRequest = CreatePatientRequest;

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

// --- AI Service ---
export type AiSeverity = "NONE" | "MINOR" | "MODERATE" | "MAJOR" | "CONTRAINDICATED";
export type AiUrgency = "ROUTINE" | "SOON" | "URGENT" | "EMERGENCY";
export type IcdConfidence = "HIGH" | "MODERATE" | "LOW";

export interface AiMedicationInput {
  name: string;
  dosage?: string;
  frequency?: string;
}

export interface DrugInteraction {
  drugA: string;
  drugB: string;
  severity: string;
  mechanism: string;
  clinicalConsequence: string;
  management: string;
}

export interface AllergyConflict {
  medication: string;
  allergen: string;
  severity: string;
  note: string;
}

export interface PrescriptionSafetyResponse {
  overallRisk: string;
  requiresPharmacistReview: boolean;
  summary: string;
  recommendation: string;
  interactions: DrugInteraction[];
  allergyConflicts: AllergyConflict[];
  modelUsed: string;
  disclaimer: string;
}

export interface LabExplanationDetail {
  testName: string;
  interpretation: string;
  explanation: string;
  clinicalSignificance: string;
}

export interface LabExplanationResponse {
  urgency: string;
  overallSummary: string;
  patientFriendlySummary: string;
  suggestedFollowUp: string;
  results: LabExplanationDetail[];
  modelUsed: string;
  disclaimer: string;
}

export interface SymptomTriageRequest {
  symptoms: string;
  duration?: string;
  patientAgeYears?: number;
  patientSex?: string;
  knownConditions?: string[];
  currentMedications?: string[];
  knownAllergies?: string[];
}

export interface SymptomTriageResponse {
  urgency: AiUrgency;
  emergency: boolean;
  recommendedSpecialty: string;
  redFlags: string[];
  rationale: string;
  selfCareAdvice: string | null;
  disclaimer: string;
  modelUsed: string;
  generatedAt: string;
}

export interface SoapNoteRequest {
  consultationNotes: string;
  patientAgeYears?: number;
  patientSex?: string;
  knownConditions?: string[];
  vitals?: Record<string, string>;
}

export interface SoapNoteResponse {
  subjective: string;
  objective: string;
  assessment: string;
  plan: string;
  assessmentProblems: string[];
  followUp: string | null;
  disclaimer: string;
  modelUsed?: string;
  generatedAt?: string;
}

export interface IcdCodesRequest {
  clinicalNotes: string;
  existingDiagnosis?: string;
}

export interface IcdCodeSuggestion {
  code: string;
  description: string;
  confidence: IcdConfidence;
  rationale: string;
}

export interface IcdCodesResponse {
  suggestions: IcdCodeSuggestion[];
  caveat: string;
  modelUsed?: string;
  generatedAt?: string;
}

export interface HistorySummaryRequest {
  patientAgeYears?: number;
  patientSex?: string;
  conditions?: string[];
  medications?: string[];
  allergies?: string[];
  recentLabResults?: { name: string; value?: string; flag?: string }[];
  pastVisits?: { date?: string; note: string }[];
}

export interface HistorySummaryResponse {
  keyConditions: string[];
  activeMedications: string[];
  criticalAllergies: string[];
  recentAbnormalFindings: string[];
  redFlags: string[];
  narrativeSummary: string;
  suggestedFollowUps: string[];
  disclaimer: string;
  modelUsed?: string;
  generatedAt?: string;
}

/** Safety-check block returned by prescription issue (409 body or "safety" on 200). */
export interface SafetyFinding {
  type: string;
  severity: string;
  description: string;
}

export interface SafetyBlockResponse {
  error: string;
  severity: string;
  summary: string;
  findings: SafetyFinding[];
  overrideAllowed: boolean;
}

export interface PrescriptionSafetyInfo {
  checked: boolean;
  severity: string;
  summary: string;
  requiresPharmacistReview: boolean;
  overridden: boolean;
  findings: SafetyFinding[] | null;
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
