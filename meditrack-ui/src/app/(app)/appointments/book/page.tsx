"use client";

import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { AlertTriangle, ArrowLeft, Check, Stethoscope } from "lucide-react";
import {
  DoctorResponse,
  PatientResponse,
  AvailabilitySlotResponse,
  AppointmentType,
  SPECIALIZATIONS,
  formatSpecialization,
} from "@/lib/types";
import {
  Badge,
  Button,
  Card,
  DetailLabel,
  DetailValue,
  EmptyState,
  Field,
  ListSkeleton,
  PageHeader,
  Skeleton,
} from "@/components/ui";
import { PatientPicker } from "@/components/PatientPicker";

const DAY_ORDER = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

function generateTimeSlots(startTime: string, endTime: string, durationMins: number): string[] {
  const parse = (t: string) => {
    const [h, m] = t.split(":").map(Number);
    return h * 60 + m;
  };
  const slots: string[] = [];
  let cur = parse(startTime);
  const end = parse(endTime);
  while (cur + durationMins <= end) {
    slots.push(`${String(Math.floor(cur / 60)).padStart(2, "0")}:${String(cur % 60).padStart(2, "0")}`);
    cur += durationMins;
  }
  return slots;
}

function fmtTime(t: string) {
  const [h, m] = t.split(":").map(Number);
  return `${h % 12 || 12}:${String(m).padStart(2, "0")} ${h >= 12 ? "PM" : "AM"}`;
}

function getDayOfWeek(dateStr: string): string {
  const d = new Date(dateStr + "T00:00:00");
  const idx = d.getDay(); // 0=Sun
  return DAY_ORDER[idx === 0 ? 6 : idx - 1];
}

function minDate(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
}

const STEPS = ["Specialty", "Doctor", "Date & time", "Confirm"];

/** Numbered stepper — the booking flow is a strict sequence. */
function Stepper({ step }: { step: number }) {
  return (
    <ol className="mb-8 flex flex-wrap items-center gap-2" aria-label="Booking steps">
      {STEPS.map((label, i) => {
        const n = i + 1;
        const state = step > n ? "done" : step === n ? "current" : "todo";
        return (
          <li key={label} className="flex items-center gap-2">
            <span
              aria-current={state === "current" ? "step" : undefined}
              className={`flex size-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                state === "done"
                  ? "bg-brand-tint text-brand-ink"
                  : state === "current"
                    ? "bg-brand text-white shadow-sm"
                    : "bg-card-2 text-ink-faint"
              }`}
            >
              {state === "done" ? <Check className="size-4" aria-hidden /> : n}
            </span>
            <span
              className={`text-xs font-medium ${
                state === "current" ? "text-brand-ink" : "text-ink-faint"
              }`}
            >
              {label}
            </span>
            {i < STEPS.length - 1 && <span className="h-px w-8 bg-line-strong" aria-hidden />}
          </li>
        );
      })}
    </ol>
  );
}

function StepHeading({ title, onBack }: { title: string; onBack?: () => void }) {
  return (
    <div className="flex items-center justify-between">
      <h2 className="font-display text-lg font-semibold text-ink">{title}</h2>
      {onBack && (
        <Button variant="ghost" size="sm" onClick={onBack}>
          <ArrowLeft className="size-3.5" aria-hidden />
          Back
        </Button>
      )}
    </div>
  );
}

function InlineError({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div
      role="alert"
      className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-danger/25 bg-danger-tint px-4 py-3"
    >
      <div className="flex items-center gap-2 text-sm text-danger">
        <AlertTriangle className="size-4 shrink-0" aria-hidden />
        {message}
      </div>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Retry
        </Button>
      )}
    </div>
  );
}

function BookForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const preselectedDoctorId = searchParams.get("doctorId") ?? "";
  const preselectedPatientId = searchParams.get("patientId") ?? "";

  const [step, setStep] = useState<1 | 2 | 3 | 4>(preselectedDoctorId ? 3 : 1);
  const [specialization, setSpecialization] = useState("");
  const [doctors, setDoctors] = useState<DoctorResponse[]>([]);
  const [doctorsLoading, setDoctorsLoading] = useState(false);
  const [doctorsError, setDoctorsError] = useState(false);

  const [selectedDoctor, setSelectedDoctor] = useState<DoctorResponse | null>(null);
  const [slots, setSlots] = useState<AvailabilitySlotResponse[]>([]);
  const [preselectErrorId, setPreselectErrorId] = useState<string | null>(null);
  const preselectError = !!preselectedDoctorId && preselectErrorId === preselectedDoctorId;

  const [selectedDate, setSelectedDate] = useState("");
  const [selectedTime, setSelectedTime] = useState("");
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [visitType, setVisitType] = useState<AppointmentType>("FIRST_VISIT");
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [bookError, setBookError] = useState<string | null>(null);

  // Deep link: /appointments/book?doctorId=… jumps straight to date & time.
  useEffect(() => {
    if (!preselectedDoctorId) return;
    let cancelled = false;
    Promise.all([
      fetch(`/api/doctors/${preselectedDoctorId}`).then((r) =>
        r.ok ? r.json() : Promise.reject(new Error(String(r.status))),
      ),
      fetch(`/api/doctors/${preselectedDoctorId}/slots`).then((r) =>
        r.ok ? r.json() : Promise.reject(new Error(String(r.status))),
      ),
    ])
      .then(([doc, sl]) => {
        if (cancelled) return;
        setSelectedDoctor(doc);
        setSlots(Array.isArray(sl) ? sl : []);
      })
      .catch(() => {
        if (!cancelled) setPreselectErrorId(preselectedDoctorId);
      });
    return () => {
      cancelled = true;
    };
  }, [preselectedDoctorId]);

  // Deep link: keep supporting ?patientId=… by hydrating the picker.
  useEffect(() => {
    if (!preselectedPatientId) return;
    let cancelled = false;
    fetch(`/api/patients/${preselectedPatientId}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((p: PatientResponse) => {
        if (!cancelled) setPatient(p);
      })
      .catch(() => {
        // Fall back to manual patient search in the confirm step.
      });
    return () => {
      cancelled = true;
    };
  }, [preselectedPatientId]);

  async function searchDoctors(spec: string) {
    setDoctorsLoading(true);
    setDoctorsError(false);
    try {
      const url = spec ? `/api/doctors?specialization=${spec}` : `/api/doctors`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(String(res.status));
      const data = await res.json();
      setDoctors(Array.isArray(data) ? data : []);
    } catch {
      setDoctorsError(true);
      setDoctors([]);
    } finally {
      setDoctorsLoading(false);
    }
  }

  function chooseSpecialty(spec: string) {
    setSpecialization(spec);
    setStep(2);
    searchDoctors(spec);
  }

  async function selectDoctor(doctor: DoctorResponse) {
    setSelectedDoctor(doctor);
    setSelectedDate("");
    setSelectedTime("");
    setStep(3);
    try {
      const res = await fetch(`/api/doctors/${doctor.id}/slots`);
      if (!res.ok) throw new Error(String(res.status));
      const sl = await res.json();
      setSlots(Array.isArray(sl) ? sl : []);
    } catch {
      setSlots([]);
      toast.error("Could not load this doctor's availability.");
    }
  }

  const availableDays = slots.filter((s) => s.available).map((s) => s.dayOfWeek);
  const slotForDate = slots.find(
    (s) => selectedDate && s.dayOfWeek === getDayOfWeek(selectedDate) && s.available,
  );
  const timeSlots = slotForDate
    ? generateTimeSlots(slotForDate.startTime, slotForDate.endTime, slotForDate.slotDurationMinutes)
    : [];

  async function submit() {
    if (!selectedDoctor || !selectedDate || !selectedTime || !patient) {
      toast.error("Please fill all required fields.");
      return;
    }
    setSubmitting(true);
    setBookError(null);
    try {
      const res = await fetch("/api/appointments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          patientId: patient.id,
          doctorId: selectedDoctor.id,
          type: visitType,
          reasonForVisit: reason || undefined,
          scheduledAt: `${selectedDate}T${selectedTime}:00`,
        }),
      });
      if (!res.ok) {
        // 422 (doctor not found / inactive) and 409 (outside availability /
        // double booking) return {"error": "…"} — surface it verbatim.
        let message = "Failed to book appointment. Please try again.";
        try {
          const body = await res.json();
          if (body && typeof body.error === "string" && body.error.trim()) {
            message = body.error;
          }
        } catch {
          // Non-JSON body — keep the generic message.
        }
        setBookError(message);
        return;
      }
      const appt = await res.json();
      toast.success("Appointment booked!");
      router.push(`/appointments/${appt.id}`);
    } catch {
      setBookError("Network error — the appointment was not booked. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="max-w-2xl">
      <Stepper step={step} />

      {/* Step 1 — Specialty grid */}
      {step === 1 && (
        <div className="space-y-4">
          <StepHeading title="Choose a specialty" />
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
            <button
              type="button"
              onClick={() => chooseSpecialty("")}
              className="rounded-xl border border-line bg-card px-3 py-3 text-left text-sm font-medium text-ink transition-colors hover:border-brand hover:bg-brand-tint"
            >
              All specialties
            </button>
            {SPECIALIZATIONS.map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => chooseSpecialty(s)}
                className="rounded-xl border border-line bg-card px-3 py-3 text-left text-sm font-medium text-ink transition-colors hover:border-brand hover:bg-brand-tint"
              >
                {formatSpecialization(s)}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Step 2 — Doctor cards */}
      {step === 2 && (
        <div className="space-y-4">
          <StepHeading title="Choose a doctor" onBack={() => setStep(1)} />
          {doctorsLoading && <ListSkeleton rows={3} />}
          {doctorsError && (
            <InlineError
              message="Failed to load doctors."
              onRetry={() => searchDoctors(specialization)}
            />
          )}
          {!doctorsLoading && !doctorsError && doctors.length === 0 && (
            <EmptyState
              icon={Stethoscope}
              title="No doctors found"
              hint="Try a different specialty."
              action={
                <Button variant="secondary" size="sm" onClick={() => setStep(1)}>
                  Change specialty
                </Button>
              }
            />
          )}
          <div className="space-y-2">
            {doctors.map((d) => (
              <button
                key={d.id}
                type="button"
                onClick={() => selectDoctor(d)}
                className="flex w-full items-center gap-4 rounded-xl border border-line bg-card p-4 text-left transition-all hover:border-brand hover:shadow-sm"
              >
                <span
                  aria-hidden
                  className="flex size-10 shrink-0 items-center justify-center rounded-full bg-brand-tint text-sm font-semibold text-brand-ink"
                >
                  {d.firstName?.[0]}
                  {d.lastName?.[0]}
                </span>
                <span className="min-w-0">
                  <span className="block truncate text-sm font-semibold text-ink">
                    {d.fullName}
                  </span>
                  <span className="mt-0.5 flex flex-wrap items-center gap-2">
                    <Badge tone="brand">{formatSpecialization(d.specialization)}</Badge>
                    <span className="text-xs text-ink-muted">
                      {d.yearsOfExperience} yrs experience
                    </span>
                  </span>
                </span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Step 3 — Date & time */}
      {step === 3 && (
        <div className="space-y-4">
          <StepHeading
            title="Pick date & time"
            onBack={!preselectedDoctorId ? () => setStep(2) : undefined}
          />

          {preselectError && (
            <InlineError message="Could not load the selected doctor. Try again from the doctors directory." />
          )}

          {!selectedDoctor && !preselectError && (
            <div className="space-y-3" role="status" aria-label="Loading doctor">
              <Skeleton className="h-16 w-full" />
              <Skeleton className="h-10 w-64" />
            </div>
          )}

          {selectedDoctor && (
            <>
              <div className="rounded-xl border border-line bg-card-2 px-4 py-3 text-sm">
                <span className="font-medium text-ink">{selectedDoctor.fullName}</span>
                {availableDays.length > 0 && (
                  <span className="ml-2 text-ink-muted">
                    Available:{" "}
                    {availableDays
                      .map((d) => d.charAt(0) + d.slice(1).toLowerCase())
                      .join(", ")}
                  </span>
                )}
              </div>

              <Field label="Date" required className="max-w-xs">
                {(ids) => (
                  <input
                    {...ids}
                    type="date"
                    min={minDate()}
                    value={selectedDate}
                    onChange={(e) => {
                      setSelectedDate(e.target.value);
                      setSelectedTime("");
                    }}
                    className="input"
                  />
                )}
              </Field>
              {selectedDate && !slotForDate && (
                <p className="text-xs text-warn">
                  Dr. {selectedDoctor.lastName} is not available on{" "}
                  {getDayOfWeek(selectedDate).charAt(0) +
                    getDayOfWeek(selectedDate).slice(1).toLowerCase()}
                  s.
                </p>
              )}

              {timeSlots.length > 0 && (
                <fieldset>
                  <legend className="mb-2 block text-xs font-medium text-ink-muted">
                    Time slot <span className="text-danger">*</span>
                  </legend>
                  <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
                    {timeSlots.map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setSelectedTime(t)}
                        aria-pressed={selectedTime === t}
                        className={`tabular rounded-lg border py-2 text-sm transition-colors ${
                          selectedTime === t
                            ? "border-brand bg-brand text-white"
                            : "border-line-strong bg-card text-ink hover:border-brand hover:text-brand-ink"
                        }`}
                      >
                        {fmtTime(t)}
                      </button>
                    ))}
                  </div>
                </fieldset>
              )}

              <Button onClick={() => setStep(4)} disabled={!selectedDate || !selectedTime}>
                Continue
              </Button>
            </>
          )}
        </div>
      )}

      {/* Step 4 — Confirm */}
      {step === 4 && selectedDoctor && (
        <div className="space-y-4">
          <StepHeading title="Confirm appointment" onBack={() => setStep(3)} />

          <Card>
            <dl className="grid grid-cols-2 gap-x-8 gap-y-3">
              <div>
                <DetailLabel>Doctor</DetailLabel>
                <DetailValue>{selectedDoctor.fullName}</DetailValue>
              </div>
              <div>
                <DetailLabel>Specialty</DetailLabel>
                <DetailValue>{formatSpecialization(selectedDoctor.specialization)}</DetailValue>
              </div>
              <div>
                <DetailLabel>Date</DetailLabel>
                <DetailValue>{selectedDate}</DetailValue>
              </div>
              <div>
                <DetailLabel>Time</DetailLabel>
                <DetailValue mono>{fmtTime(selectedTime)}</DetailValue>
              </div>
            </dl>
          </Card>

          <PatientPicker selected={patient} onSelect={setPatient} />

          <Field label="Visit type">
            {(ids) => (
              <select
                {...ids}
                value={visitType}
                onChange={(e) => setVisitType(e.target.value as AppointmentType)}
                className="input"
              >
                <option value="FIRST_VISIT">First visit</option>
                <option value="FOLLOW_UP">Follow-up</option>
                <option value="EMERGENCY">Emergency</option>
                <option value="TELECONSULTATION">Teleconsultation</option>
              </select>
            )}
          </Field>

          <Field label="Reason for visit" hint="Optional — briefly describe symptoms.">
            {(ids) => (
              <textarea
                {...ids}
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Briefly describe your symptoms…"
                className="input"
              />
            )}
          </Field>

          {bookError && (
            <div
              role="alert"
              className="flex items-start gap-2 rounded-xl border border-danger/25 bg-danger-tint px-4 py-3 text-sm text-danger"
            >
              <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
              <p>{bookError}</p>
            </div>
          )}

          <Button
            onClick={submit}
            loading={submitting}
            disabled={!patient}
            className="w-full"
          >
            Confirm appointment
          </Button>
        </div>
      )}
    </div>
  );
}

export default function BookAppointmentPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Scheduling"
        title="Book appointment"
        description="Pick a specialty, choose a doctor, and lock in a time slot."
      />
      <Suspense fallback={<ListSkeleton rows={3} />}>
        <BookForm />
      </Suspense>
    </div>
  );
}
