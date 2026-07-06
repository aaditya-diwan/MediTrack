"use client";

import { useEffect, useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { format } from "date-fns";
import { AlertTriangle, CalendarPlus, CalendarX2 } from "lucide-react";
import { AppointmentResponse, PatientResponse } from "@/lib/types";
import {
  APPOINTMENT_STATUS,
  Button,
  EmptyState,
  ListSkeleton,
  PageHeader,
  StatusBadge,
  statusOf,
} from "@/components/ui";
import { PatientPicker } from "@/components/PatientPicker";

function BookLink({ patientId }: { patientId?: string }) {
  const href = patientId
    ? `/appointments/book?patientId=${patientId}`
    : "/appointments/book";
  return (
    <Link
      href={href}
      className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-brand-strong"
    >
      <CalendarPlus className="size-4" aria-hidden />
      Book appointment
    </Link>
  );
}

function ErrorCard({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div
      role="alert"
      className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-danger/25 bg-danger-tint px-4 py-3"
    >
      <div className="flex items-center gap-2 text-sm text-danger">
        <AlertTriangle className="size-4 shrink-0" aria-hidden />
        {message}
      </div>
      <Button variant="secondary" size="sm" onClick={onRetry}>
        Retry
      </Button>
    </div>
  );
}

function AppointmentsList() {
  const searchParams = useSearchParams();
  const initialPatientId = searchParams.get("patientId");

  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [hydrating, setHydrating] = useState(Boolean(initialPatientId));
  const [reloadKey, setReloadKey] = useState(0);
  const [loadedKey, setLoadedKey] = useState<string | null>(null);
  const requestKey = patient ? `${patient.id}:${reloadKey}` : null;
  const loading = requestKey !== null && loadedKey !== requestKey;

  // Support /appointments?patientId=… deep links by hydrating the picker.
  useEffect(() => {
    if (!initialPatientId) return;
    let cancelled = false;
    fetch(`/api/patients/${initialPatientId}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((p: PatientResponse) => {
        if (!cancelled) setPatient(p);
      })
      .catch(() => {
        if (!cancelled) {
          setError("Could not load the patient from the link. Search for them below.");
        }
      })
      .finally(() => {
        if (!cancelled) setHydrating(false);
      });
    return () => {
      cancelled = true;
    };
  }, [initialPatientId]);

  useEffect(() => {
    if (!patient || !requestKey) return;
    let cancelled = false;
    fetch(`/api/appointments/patient/${patient.id}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((data) => {
        if (cancelled) return;
        setAppointments(Array.isArray(data) ? data : []);
        setError(null);
        setLoadedKey(requestKey);
      })
      .catch(() => {
        if (cancelled) return;
        setError("Failed to load appointments for this patient.");
        setAppointments([]);
        setLoadedKey(requestKey);
      });
    return () => {
      cancelled = true;
    };
  }, [patient, requestKey]);

  const retryLoad = () => {
    setError(null);
    setReloadKey((k) => k + 1);
  };

  return (
    <div className="space-y-6">
      <div className="max-w-md">
        <PatientPicker
          selected={patient}
          onSelect={(p) => {
            setPatient(p);
            setError(null);
            if (!p) setAppointments([]);
          }}
        />
      </div>

      {error && (
        <ErrorCard
          message={error}
          onRetry={() => (patient ? retryLoad() : setError(null))}
        />
      )}

      {(loading || hydrating) && <ListSkeleton rows={4} />}

      {!loading && !hydrating && !patient && !error && (
        <EmptyState
          icon={CalendarX2}
          title="Select a patient to view their appointments"
          hint="Search by name or MRN above, or book a brand-new appointment."
          action={<BookLink />}
        />
      )}

      {!loading && !hydrating && patient && !error && appointments.length === 0 && (
        <EmptyState
          icon={CalendarX2}
          title={`No appointments for ${patient.firstName} ${patient.lastName}`}
          hint="This patient has no appointments on file yet."
          action={<BookLink patientId={patient.id} />}
        />
      )}

      {!loading && appointments.length > 0 && (
        <ul className="space-y-3">
          {appointments
            .slice()
            .sort(
              (a, b) =>
                new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime(),
            )
            .map((a) => (
              <li key={a.id}>
                <Link
                  href={`/appointments/${a.id}`}
                  className="block rounded-xl border border-line bg-card p-4 transition-all hover:border-brand hover:shadow-sm"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-ink">
                        {format(new Date(a.scheduledAt), "MMM d, yyyy 'at' h:mm a")}
                      </p>
                      <p className="mt-0.5 text-sm capitalize text-ink-muted">
                        {a.type.replace(/_/g, " ").toLowerCase()}
                        {a.reasonForVisit ? ` · ${a.reasonForVisit}` : ""}
                      </p>
                    </div>
                    <StatusBadge status={statusOf(APPOINTMENT_STATUS, a.status)} />
                  </div>
                </Link>
              </li>
            ))}
        </ul>
      )}
    </div>
  );
}

export default function AppointmentsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Scheduling"
        title="Appointments"
        description="Look up a patient to review their visit history and upcoming appointments."
        actions={<BookLink />}
      />
      <Suspense fallback={<ListSkeleton rows={4} />}>
        <AppointmentsList />
      </Suspense>
    </div>
  );
}
