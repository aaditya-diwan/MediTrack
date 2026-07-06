"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import { format } from "date-fns";
import { toast } from "sonner";
import {
  AlertTriangle,
  Download,
  FlaskConical,
  Pill,
  Plus,
  ShieldAlert,
  ShieldCheck,
  ShieldOff,
  Trash2,
} from "lucide-react";
import {
  AppointmentResponse,
  AppointmentStatus,
  PatientResponse,
  PrescriptionResponse,
  MedicationDraft,
  PrescriptionLabOrderDraft,
} from "@/lib/types";
import {
  APPOINTMENT_STATUS,
  Badge,
  Button,
  Card,
  DetailLabel,
  DetailValue,
  EcgLoading,
  EmptyState,
  Field,
  LAB_PRIORITY,
  PRESCRIPTION_STATUS,
  SEVERITY,
  StatusBadge,
  statusOf,
} from "@/components/ui";

type Tab = "patient" | "prescribe" | "history";

// --- AI safety screen contract (POST /api/prescriptions/{id}/issue) ---

interface SafetyFinding {
  type: string;
  severity: string;
  description: string;
}

interface PrescriptionSafety {
  checked: boolean;
  severity: string;
  summary: string;
  requiresPharmacistReview: boolean;
  overridden: boolean;
  overrideReason: string | null;
  findings: SafetyFinding[] | null;
}

type RxWithSafety = PrescriptionResponse & { safety?: PrescriptionSafety | null };

/** 409 body when the safety screen blocks issuing. */
interface SafetyBlockBody {
  error: string;
  severity: string;
  summary: string;
  requiresPharmacistReview: boolean;
  findings: SafetyFinding[];
  overrideAllowed?: boolean;
}

const BLANK_MED: MedicationDraft = {
  medicationName: "", genericName: "", dosage: "", frequency: "",
  duration: "", route: "", instructions: "",
};
const BLANK_LAB: PrescriptionLabOrderDraft = {
  testCode: "", testName: "", clinicalIndication: "", urgency: "ROUTINE",
};

const MED_FIELDS: { label: string; key: keyof MedicationDraft; placeholder: string; required?: boolean }[] = [
  { label: "Name", key: "medicationName", placeholder: "e.g. Metformin", required: true },
  { label: "Generic name", key: "genericName", placeholder: "e.g. Metformin HCl" },
  { label: "Dosage", key: "dosage", placeholder: "e.g. 500mg", required: true },
  { label: "Frequency", key: "frequency", placeholder: "e.g. Twice daily", required: true },
  { label: "Duration", key: "duration", placeholder: "e.g. 30 days" },
  { label: "Route", key: "route", placeholder: "e.g. Oral" },
];

const LAB_FIELDS: { label: string; key: keyof PrescriptionLabOrderDraft; placeholder: string; required?: boolean }[] = [
  { label: "Test code", key: "testCode", placeholder: "e.g. CBC", required: true },
  { label: "Test name", key: "testName", placeholder: "e.g. Complete Blood Count", required: true },
  { label: "Clinical indication", key: "clinicalIndication", placeholder: "e.g. Suspected anaemia" },
];

function SectionHeading({ children }: { children: React.ReactNode }) {
  return (
    <h3 className="font-display text-sm font-semibold uppercase tracking-wide text-ink-muted">
      {children}
    </h3>
  );
}

function SafetyFindingsList({ findings }: { findings: SafetyFinding[] }) {
  return (
    <ul className="space-y-2">
      {findings.map((f, i) => (
        <li
          key={i}
          className="flex items-start gap-3 rounded-lg border border-line bg-card px-3 py-2"
        >
          <StatusBadge status={statusOf(SEVERITY, f.severity)} className="mt-0.5 shrink-0" />
          <div className="min-w-0 text-sm">
            <p className="text-ink">{f.description}</p>
            <p className="mt-0.5 text-xs capitalize text-ink-faint">
              {f.type.replace(/_/g, " ").toLowerCase()}
            </p>
          </div>
        </li>
      ))}
    </ul>
  );
}

/** Outcome of the AI safety screen after a successful issue (200). */
function SafetyOutcomePanel({ safety }: { safety: PrescriptionSafety }) {
  if (!safety.checked) {
    return (
      <p className="flex items-center gap-2 rounded-lg border border-line bg-card-2 px-3 py-2 text-xs text-ink-muted">
        <ShieldOff className="size-4 shrink-0 text-ink-faint" aria-hidden />
        AI safety screen unavailable — issued without screening.
      </p>
    );
  }

  if (safety.severity === "NONE") {
    return (
      <p className="flex items-center gap-2 rounded-lg border border-ok/20 bg-ok-tint px-3 py-2 text-xs text-ok">
        <ShieldCheck className="size-4 shrink-0" aria-hidden />
        AI safety screen passed — no interactions found.
      </p>
    );
  }

  return (
    <section
      role="alert"
      className="space-y-3 rounded-xl border border-warn/25 bg-warn-tint p-4"
    >
      <div className="flex flex-wrap items-center gap-2">
        <AlertTriangle className="size-5 shrink-0 text-warn" aria-hidden />
        <h3 className="font-display text-sm font-semibold text-ink">
          AI safety screen flagged this prescription
        </h3>
        <StatusBadge status={statusOf(SEVERITY, safety.severity)} />
        {safety.requiresPharmacistReview && (
          <Badge tone="warn">Pharmacist review required</Badge>
        )}
        {safety.overridden && <Badge tone="danger">Issued via override</Badge>}
      </div>
      {safety.summary && <p className="text-sm text-ink">{safety.summary}</p>}
      {safety.findings && safety.findings.length > 0 && (
        <SafetyFindingsList findings={safety.findings} />
      )}
      {safety.overridden && safety.overrideReason && (
        <p className="text-xs text-ink-muted">
          Override reason: <span className="text-ink">{safety.overrideReason}</span>
        </p>
      )}
    </section>
  );
}

/** 409 block panel with the override-and-reissue flow. Clinical safety UI — severity must be unmissable. */
function SafetyBlockPanel({
  blocked,
  overrideReason,
  onReasonChange,
  onOverride,
  saving,
}: {
  blocked: SafetyBlockBody;
  overrideReason: string;
  onReasonChange: (v: string) => void;
  onOverride: () => void;
  saving: boolean;
}) {
  return (
    <section
      role="alert"
      className="space-y-4 rounded-xl border-2 border-danger/40 bg-danger-tint p-5"
    >
      <div className="flex items-start gap-3">
        <ShieldAlert className="size-8 shrink-0 text-danger" aria-hidden />
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="font-display text-base font-semibold text-danger">
              Safety screen blocked this prescription
            </h3>
            <StatusBadge status={statusOf(SEVERITY, blocked.severity)} />
            {blocked.requiresPharmacistReview && (
              <Badge tone="danger">Pharmacist review required</Badge>
            )}
          </div>
          <p className="mt-1 text-sm text-ink">{blocked.summary || blocked.error}</p>
        </div>
      </div>

      {blocked.findings && blocked.findings.length > 0 && (
        <SafetyFindingsList findings={blocked.findings} />
      )}

      {blocked.overrideAllowed && (
        <div className="space-y-3 border-t border-danger/20 pt-4">
          <Field
            label="Clinical justification for override"
            required
            hint="Documented on the prescription record. Required to issue against the safety screen."
          >
            {(ids) => (
              <textarea
                {...ids}
                rows={3}
                value={overrideReason}
                onChange={(e) => onReasonChange(e.target.value)}
                placeholder="e.g. Benefit outweighs interaction risk; patient monitored weekly…"
                className="input"
              />
            )}
          </Field>
          <Button
            variant="danger"
            onClick={onOverride}
            loading={saving}
            disabled={!overrideReason.trim()}
          >
            <ShieldAlert className="size-4" aria-hidden />
            Override and issue anyway
          </Button>
        </div>
      )}
    </section>
  );
}

function PatientTab({ patient }: { patient: PatientResponse | null }) {
  if (!patient) {
    return (
      <EmptyState
        title="Patient data unavailable"
        hint="The patient record could not be loaded for this appointment."
      />
    );
  }
  return (
    <div className="max-w-2xl space-y-5">
      <Card>
        <dl className="grid grid-cols-2 gap-x-8 gap-y-4">
          <div>
            <DetailLabel>MRN</DetailLabel>
            <DetailValue mono>{patient.mrn}</DetailValue>
          </div>
          <div>
            <DetailLabel>Date of birth</DetailLabel>
            <DetailValue>{format(new Date(patient.dateOfBirth), "MMM d, yyyy")}</DetailValue>
          </div>
          <div>
            <DetailLabel>Email</DetailLabel>
            <DetailValue>{patient.email}</DetailValue>
          </div>
          <div>
            <DetailLabel>Phone</DetailLabel>
            <DetailValue mono>{patient.phoneNumber}</DetailValue>
          </div>
          <div>
            <DetailLabel>Address</DetailLabel>
            <DetailValue>{patient.address}</DetailValue>
          </div>
          <div>
            <DetailLabel>Insurance</DetailLabel>
            <DetailValue>{patient.insuranceProvider}</DetailValue>
          </div>
        </dl>
      </Card>
      {patient.medicalHistory && patient.medicalHistory.length > 0 && (
        <section className="space-y-2">
          <SectionHeading>Medical history</SectionHeading>
          {patient.medicalHistory.slice(0, 5).map((r) => (
            <div key={r.recordId} className="rounded-lg border border-line bg-card px-3 py-2 text-sm">
              <span className="font-medium text-ink">{r.diagnosis}</span>
              <span className="tabular ml-2 text-xs text-ink-faint">{r.date}</span>
              {r.treatment && <p className="mt-0.5 text-xs text-ink-muted">{r.treatment}</p>}
            </div>
          ))}
        </section>
      )}
    </div>
  );
}

function HistoryTab({ prescriptions }: { prescriptions: RxWithSafety[] }) {
  if (prescriptions.length === 0) {
    return (
      <EmptyState
        icon={Pill}
        title="No prescriptions on file"
        hint="Prescriptions written for this patient will appear here."
      />
    );
  }
  return (
    <div className="max-w-2xl space-y-3">
      {prescriptions
        .slice()
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        .map((rx) => (
          <Card key={rx.id} className="text-sm">
            <div className="mb-2 flex items-center justify-between">
              <span className="tabular text-xs text-ink-faint">{rx.id.slice(0, 8)}…</span>
              <StatusBadge status={statusOf(PRESCRIPTION_STATUS, rx.status)} />
            </div>
            {rx.consultationNotes && (
              <p className="mb-2 line-clamp-2 text-xs text-ink-muted">{rx.consultationNotes}</p>
            )}
            {rx.medications && rx.medications.length > 0 && (
              <div className="flex flex-wrap gap-1">
                {rx.medications.map((m) => (
                  <Badge key={m.id} tone="neutral">
                    {m.medicationName} {m.dosage}
                  </Badge>
                ))}
              </div>
            )}
            <p className="mt-2 text-xs text-ink-faint">
              {format(new Date(rx.createdAt), "MMM d, yyyy")}
            </p>
          </Card>
        ))}
    </div>
  );
}

export default function ConsultationPage({
  params,
}: {
  params: Promise<{ appointmentId: string }>;
}) {
  const { appointmentId } = use(params);
  const [appt, setAppt] = useState<AppointmentResponse | null>(null);
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [prescriptions, setPrescriptions] = useState<RxWithSafety[]>([]);
  const [tab, setTab] = useState<Tab>("patient");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  const [consultationNotes, setConsultationNotes] = useState("");
  const [diagnosisCodes, setDiagnosisCodes] = useState("");
  const [medications, setMedications] = useState<MedicationDraft[]>([]);
  const [labOrders, setLabOrders] = useState<PrescriptionLabOrderDraft[]>([]);
  const [newMed, setNewMed] = useState<MedicationDraft>({ ...BLANK_MED });
  const [newLab, setNewLab] = useState<PrescriptionLabOrderDraft>({ ...BLANK_LAB });
  const [showMedForm, setShowMedForm] = useState(false);
  const [showLabForm, setShowLabForm] = useState(false);
  const [activePrescription, setActivePrescription] = useState<RxWithSafety | null>(null);
  const [saving, setSaving] = useState(false);

  // Contract B state: outcome of the safety screen and the 409 block payload.
  const [safety, setSafety] = useState<PrescriptionSafety | null>(null);
  const [blocked, setBlocked] = useState<SafetyBlockBody | null>(null);
  const [overrideReason, setOverrideReason] = useState("");

  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/appointments/${appointmentId}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then(async (a: AppointmentResponse) => {
        if (cancelled) return;
        setAppt(a);
        const [patRes, rxRes] = await Promise.all([
          fetch(`/api/patients/${a.patientId}`).catch(() => null),
          fetch(`/api/prescriptions/patient/${a.patientId}`).catch(() => null),
        ]);
        if (cancelled) return;
        if (patRes?.ok) {
          const patData = await patRes.json();
          if (!cancelled) setPatient(patData);
        }
        if (rxRes?.ok) {
          const rxData = await rxRes.json();
          if (!cancelled) setPrescriptions(Array.isArray(rxData) ? rxData : []);
        }
      })
      .catch(() => {
        if (!cancelled) setLoadError(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [appointmentId, reloadKey]);

  const load = () => {
    setLoading(true);
    setLoadError(false);
    setReloadKey((k) => k + 1);
  };

  async function updateApptStatus(status: AppointmentStatus) {
    try {
      const res = await fetch(
        `/api/appointments/${appointmentId}/status?status=${status}`,
        { method: "PUT" },
      );
      if (!res.ok) {
        toast.error("Failed to update status.");
        return;
      }
      setAppt(await res.json());
      toast.success(`Marked as ${status.replace(/_/g, " ").toLowerCase()}`);
    } catch {
      toast.error("Network error — status not updated.");
    }
  }

  function addMedication() {
    if (!newMed.medicationName || !newMed.dosage || !newMed.frequency) {
      toast.error("Name, dosage, and frequency are required.");
      return;
    }
    setMedications((m) => [...m, { ...newMed }]);
    setNewMed({ ...BLANK_MED });
    setShowMedForm(false);
  }

  function addLabOrder() {
    if (!newLab.testCode || !newLab.testName) {
      toast.error("Test code and name are required.");
      return;
    }
    setLabOrders((l) => [...l, { ...newLab }]);
    setNewLab({ ...BLANK_LAB });
    setShowLabForm(false);
  }

  async function createDraft() {
    if (!appt) return;
    setSaving(true);
    try {
      const res = await fetch("/api/prescriptions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          patientId: appt.patientId,
          doctorId: appt.doctorId,
          appointmentId: appt.id,
          consultationNotes,
          diagnosisCodes,
          medications,
          labOrders,
        }),
      });
      if (!res.ok) {
        toast.error("Failed to create prescription.");
        return;
      }
      setActivePrescription(await res.json());
      setSafety(null);
      setBlocked(null);
      toast.success("Draft created.");
    } catch {
      toast.error("Network error — draft not created.");
    } finally {
      setSaving(false);
    }
  }

  /**
   * Issue the draft. The backend runs an AI safety screen:
   * - 200 → PrescriptionResponse.safety carries the outcome (shown below).
   * - 409 → blocked; the payload lists findings and whether an override
   *   (with a documented reason) is allowed. Re-POSTs with {override:true}.
   */
  async function issuePrescription(override?: { reason: string }) {
    if (!activePrescription) return;
    setSaving(true);
    try {
      const res = await fetch(`/api/prescriptions/${activePrescription.id}/issue`, {
        method: "POST",
        ...(override
          ? {
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                override: true,
                overrideReason: override.reason,
              }),
            }
          : {}),
      });

      if (res.status === 409) {
        let body: SafetyBlockBody | null = null;
        try {
          body = await res.json();
        } catch {
          // Non-JSON body — fall through to generic handling.
        }
        if (body && typeof body.severity === "string") {
          setBlocked(body);
        } else {
          toast.error(body?.error || "Issuing was blocked. Please review the draft.");
        }
        return;
      }

      if (!res.ok) {
        toast.error("Failed to issue.");
        return;
      }

      const rx: RxWithSafety = await res.json();
      setActivePrescription(rx);
      setSafety(rx.safety ?? null);
      setBlocked(null);
      setOverrideReason("");
      setPrescriptions((prev) => [rx, ...prev.filter((p) => p.id !== rx.id)]);
      toast.success(
        rx.safety?.overridden
          ? "Prescription issued with safety override."
          : "Prescription issued!",
      );
    } catch {
      toast.error("Network error — prescription not issued.");
    } finally {
      setSaving(false);
    }
  }

  async function sendToPharmacy() {
    if (!activePrescription) return;
    try {
      const res = await fetch(
        `/api/prescriptions/${activePrescription.id}/send-pharmacy`,
        { method: "POST" },
      );
      if (!res.ok) {
        toast.error("Failed to send to pharmacy.");
        return;
      }
      setActivePrescription(await res.json());
      toast.success("Sent to pharmacy.");
    } catch {
      toast.error("Network error — not sent to pharmacy.");
    }
  }

  async function sendToLab() {
    if (!activePrescription) return;
    try {
      const res = await fetch(`/api/prescriptions/${activePrescription.id}/send-lab`, {
        method: "POST",
      });
      if (!res.ok) {
        toast.error("Failed to send to lab.");
        return;
      }
      setActivePrescription(await res.json());
      toast.success("Sent to lab.");
    } catch {
      toast.error("Network error — not sent to lab.");
    }
  }

  function downloadPdf() {
    if (!activePrescription) return;
    const a = document.createElement("a");
    a.href = `/api/prescriptions/${activePrescription.id}/pdf`;
    a.download = `prescription-${activePrescription.id.slice(0, 8)}.pdf`;
    a.click();
  }

  if (loading) return <EcgLoading label="Loading consultation" />;

  if (loadError || !appt) {
    return (
      <div
        role="alert"
        className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-danger/25 bg-danger-tint px-4 py-3"
      >
        <div className="flex items-center gap-2 text-sm text-danger">
          <AlertTriangle className="size-4 shrink-0" aria-hidden />
          Failed to load this consultation.
        </div>
        <Button variant="secondary" size="sm" onClick={load}>
          Retry
        </Button>
      </div>
    );
  }

  const isIssued = activePrescription && activePrescription.status !== "DRAFT";

  return (
    <div>
      <header className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="mb-1 text-[11px] font-medium uppercase tracking-[0.14em] text-brand-ink">
            Consultation
          </p>
          <h1 className="font-display text-xl font-semibold tracking-tight text-ink">
            {patient
              ? `${patient.firstName} ${patient.lastName}`
              : `Patient …${appt.patientId.slice(-6)}`}
          </h1>
          <p className="mt-0.5 text-sm text-ink-muted">
            {format(new Date(appt.scheduledAt), "MMM d, yyyy 'at' h:mm a")} ·{" "}
            {appt.type.replace(/_/g, " ").toLowerCase()}
            {appt.reasonForVisit ? ` · ${appt.reasonForVisit}` : ""}
          </p>
        </div>
        <StatusBadge status={statusOf(APPOINTMENT_STATUS, appt.status)} />
      </header>

      <div className="mb-5 flex gap-2">
        {appt.status === "CONFIRMED" && (
          <Button size="sm" onClick={() => updateApptStatus("IN_PROGRESS")}>
            Start consultation
          </Button>
        )}
        {appt.status === "IN_PROGRESS" && (
          <Button size="sm" onClick={() => updateApptStatus("COMPLETED")}>
            Mark completed
          </Button>
        )}
      </div>

      <div role="tablist" aria-label="Consultation sections" className="mb-5 flex gap-1 border-b border-line">
        {(["patient", "prescribe", "history"] as Tab[]).map((t) => (
          <button
            key={t}
            role="tab"
            aria-selected={tab === t}
            onClick={() => setTab(t)}
            className={`-mb-px px-4 py-2 text-sm transition-colors ${
              tab === t
                ? "border-b-2 border-brand font-medium text-brand-ink"
                : "text-ink-muted hover:text-ink"
            }`}
          >
            {t === "prescribe" ? "Write Prescription" : t === "history" ? "Rx History" : "Patient"}
          </button>
        ))}
      </div>

      {tab === "patient" && <PatientTab patient={patient} />}

      {tab === "prescribe" && (
        <div className="max-w-2xl space-y-5">
          {activePrescription && (
            <div
              className={`flex items-center gap-2 rounded-lg border px-4 py-2 text-sm ${
                activePrescription.status === "DRAFT"
                  ? "border-warn/25 bg-warn-tint text-ink"
                  : "border-ok/20 bg-ok-tint text-ink"
              }`}
            >
              <span>
                Prescription{" "}
                <span className="tabular text-xs">{activePrescription.id.slice(0, 8)}…</span>
              </span>
              <StatusBadge
                status={statusOf(PRESCRIPTION_STATUS, activePrescription.status)}
              />
            </div>
          )}

          {!activePrescription && (
            <>
              <Field label="Consultation notes">
                {(ids) => (
                  <textarea
                    {...ids}
                    rows={4}
                    value={consultationNotes}
                    onChange={(e) => setConsultationNotes(e.target.value)}
                    placeholder="SOAP notes, clinical observations…"
                    className="input"
                  />
                )}
              </Field>
              <Field label="Diagnosis codes (ICD-10)">
                {(ids) => (
                  <input
                    {...ids}
                    value={diagnosisCodes}
                    onChange={(e) => setDiagnosisCodes(e.target.value)}
                    placeholder="e.g. J06.9, E11.9"
                    className="input tabular"
                  />
                )}
              </Field>

              {/* Medications */}
              <section>
                <div className="mb-2 flex items-center justify-between">
                  <SectionHeading>Medications</SectionHeading>
                  <Button variant="secondary" size="sm" onClick={() => setShowMedForm(!showMedForm)}>
                    <Plus className="size-3.5" aria-hidden />
                    Add medication
                  </Button>
                </div>
                {showMedForm && (
                  <div className="mb-3 space-y-3 rounded-xl border border-line bg-card-2 p-4">
                    <div className="grid grid-cols-2 gap-3">
                      {MED_FIELDS.map(({ label, key, placeholder, required }) => (
                        <Field key={key} label={label} required={required}>
                          {(ids) => (
                            <input
                              {...ids}
                              value={newMed[key]}
                              onChange={(e) => setNewMed({ ...newMed, [key]: e.target.value })}
                              placeholder={placeholder}
                              className="input text-xs"
                            />
                          )}
                        </Field>
                      ))}
                    </div>
                    <Field label="Instructions">
                      {(ids) => (
                        <input
                          {...ids}
                          value={newMed.instructions}
                          onChange={(e) =>
                            setNewMed({ ...newMed, instructions: e.target.value })
                          }
                          placeholder="e.g. Take with food"
                          className="input text-xs"
                        />
                      )}
                    </Field>
                    <div className="flex gap-2">
                      <Button size="sm" onClick={addMedication}>
                        Add
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setShowMedForm(false)}>
                        Cancel
                      </Button>
                    </div>
                  </div>
                )}
                {medications.length === 0 ? (
                  <p className="text-xs text-ink-faint">No medications added.</p>
                ) : (
                  <ul className="space-y-1">
                    {medications.map((m, i) => (
                      <li
                        key={i}
                        className="flex items-center justify-between rounded-lg border border-line bg-card px-3 py-2 text-sm"
                      >
                        <div className="flex min-w-0 items-center gap-2">
                          <Pill className="size-4 shrink-0 text-brand-ink" aria-hidden />
                          <span className="font-medium text-ink">{m.medicationName}</span>
                          <span className="tabular truncate text-xs text-ink-muted">
                            {m.dosage} · {m.frequency}
                            {m.duration ? ` · ${m.duration}` : ""}
                          </span>
                        </div>
                        <button
                          type="button"
                          onClick={() => setMedications((p) => p.filter((_, j) => j !== i))}
                          aria-label={`Remove ${m.medicationName}`}
                          className="ml-4 rounded p-1 text-ink-faint transition-colors hover:text-danger"
                        >
                          <Trash2 className="size-4" aria-hidden />
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              {/* Lab orders */}
              <section>
                <div className="mb-2 flex items-center justify-between">
                  <SectionHeading>Lab orders</SectionHeading>
                  <Button variant="secondary" size="sm" onClick={() => setShowLabForm(!showLabForm)}>
                    <Plus className="size-3.5" aria-hidden />
                    Add lab order
                  </Button>
                </div>
                {showLabForm && (
                  <div className="mb-3 space-y-3 rounded-xl border border-line bg-card-2 p-4">
                    <div className="grid grid-cols-2 gap-3">
                      {LAB_FIELDS.map(({ label, key, placeholder, required }) => (
                        <Field key={key} label={label} required={required}>
                          {(ids) => (
                            <input
                              {...ids}
                              value={newLab[key]}
                              onChange={(e) => setNewLab({ ...newLab, [key]: e.target.value })}
                              placeholder={placeholder}
                              className="input text-xs"
                            />
                          )}
                        </Field>
                      ))}
                      <Field label="Urgency">
                        {(ids) => (
                          <select
                            {...ids}
                            value={newLab.urgency}
                            onChange={(e) => setNewLab({ ...newLab, urgency: e.target.value })}
                            className="input text-xs"
                          >
                            <option value="ROUTINE">Routine</option>
                            <option value="URGENT">Urgent</option>
                            <option value="STAT">STAT</option>
                          </select>
                        )}
                      </Field>
                    </div>
                    <div className="flex gap-2">
                      <Button size="sm" onClick={addLabOrder}>
                        Add
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setShowLabForm(false)}>
                        Cancel
                      </Button>
                    </div>
                  </div>
                )}
                {labOrders.length === 0 ? (
                  <p className="text-xs text-ink-faint">No lab orders added.</p>
                ) : (
                  <ul className="space-y-1">
                    {labOrders.map((l, i) => (
                      <li
                        key={i}
                        className="flex items-center justify-between rounded-lg border border-line bg-card px-3 py-2 text-sm"
                      >
                        <div className="flex min-w-0 items-center gap-2">
                          <FlaskConical className="size-4 shrink-0 text-brand-ink" aria-hidden />
                          <span className="font-medium text-ink">{l.testName}</span>
                          <span className="tabular text-xs text-ink-muted">{l.testCode}</span>
                          <StatusBadge status={statusOf(LAB_PRIORITY, l.urgency)} />
                        </div>
                        <button
                          type="button"
                          onClick={() => setLabOrders((p) => p.filter((_, j) => j !== i))}
                          aria-label={`Remove ${l.testName}`}
                          className="ml-4 rounded p-1 text-ink-faint transition-colors hover:text-danger"
                        >
                          <Trash2 className="size-4" aria-hidden />
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <Button onClick={createDraft} loading={saving}>
                Create prescription draft
              </Button>
            </>
          )}

          {activePrescription && (
            <>
              {blocked && (
                <SafetyBlockPanel
                  blocked={blocked}
                  overrideReason={overrideReason}
                  onReasonChange={setOverrideReason}
                  onOverride={() => issuePrescription({ reason: overrideReason.trim() })}
                  saving={saving}
                />
              )}

              {!blocked && safety && <SafetyOutcomePanel safety={safety} />}

              <div className="flex flex-wrap gap-3">
                {activePrescription.status === "DRAFT" && !blocked && (
                  <Button onClick={() => issuePrescription()} loading={saving}>
                    Issue prescription
                  </Button>
                )}
                {isIssued && (
                  <>
                    <Button variant="secondary" onClick={sendToPharmacy}>
                      Send to pharmacy
                    </Button>
                    {activePrescription.labOrders &&
                      activePrescription.labOrders.length > 0 && (
                        <Button variant="secondary" onClick={sendToLab}>
                          Send to lab
                        </Button>
                      )}
                    <Button variant="secondary" onClick={downloadPdf}>
                      <Download className="size-4" aria-hidden />
                      Download PDF
                    </Button>
                  </>
                )}
              </div>
            </>
          )}
        </div>
      )}

      {tab === "history" && <HistoryTab prescriptions={prescriptions} />}
    </div>
  );
}
