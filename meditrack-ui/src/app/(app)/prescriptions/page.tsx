"use client";

import { useEffect, useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { format } from "date-fns";
import { AlertTriangle, Pill, RefreshCw } from "lucide-react";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ListSkeleton,
  PageHeader,
  PRESCRIPTION_STATUS,
  StatusBadge,
  statusOf,
} from "@/components/ui";
import { PatientPicker } from "@/components/PatientPicker";
import type { PatientResponse, PrescriptionResponse } from "@/lib/types";

function PrescriptionList() {
  const searchParams = useSearchParams();
  const paramPatientId = searchParams.get("patientId");
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [prescriptions, setPrescriptions] = useState<PrescriptionResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [loadedKey, setLoadedKey] = useState<string | null>(null);

  const activePatientId = patient?.id ?? paramPatientId;
  const requestKey =
    activePatientId && activePatientId.trim()
      ? `${activePatientId}:${reloadKey}`
      : null;
  const loading = requestKey !== null && loadedKey !== requestKey;
  const searched = requestKey !== null;

  useEffect(() => {
    if (!activePatientId || !requestKey) return;
    let cancelled = false;
    fetch(`/api/prescriptions/patient/${activePatientId}`)
      .then((r) => {
        if (!r.ok) throw new Error(String(r.status));
        return r.json();
      })
      .then((data) => {
        if (cancelled) return;
        setPrescriptions(Array.isArray(data) ? data : []);
        setError(null);
        setLoadedKey(requestKey);
      })
      .catch(() => {
        if (cancelled) return;
        setError("Could not load prescriptions for this patient.");
        setLoadedKey(requestKey);
      });
    return () => {
      cancelled = true;
    };
  }, [activePatientId, requestKey]);

  const retryLoad = () => {
    setError(null);
    setReloadKey((k) => k + 1);
  };

  return (
    <div>
      <div className="mb-6 max-w-md">
        <PatientPicker
          selected={patient}
          onSelect={(p) => {
            setPatient(p);
            setError(null);
            if (!p) {
              setPrescriptions([]);
              setReloadKey((k) => k + 1);
            }
          }}
        />
        {!patient && paramPatientId && (
          <p className="tabular mt-1 text-xs text-ink-faint">
            Showing prescriptions for patient {paramPatientId}
          </p>
        )}
      </div>

      {loading && <ListSkeleton rows={4} />}

      {!loading && error && activePatientId && (
        <div
          role="alert"
          className="flex items-start justify-between gap-4 rounded-xl border border-danger/25 bg-danger-tint p-4"
        >
          <div className="flex items-start gap-2">
            <AlertTriangle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden />
            <p className="text-sm text-danger">{error}</p>
          </div>
          <Button
            variant="secondary"
            size="sm"
            onClick={retryLoad}
          >
            <RefreshCw className="size-3.5" aria-hidden />
            Retry
          </Button>
        </div>
      )}

      {!loading && !error && !searched && (
        <EmptyState
          icon={Pill}
          title="Select a patient"
          hint="Search for a patient by name or MRN to see their prescriptions."
        />
      )}

      {!loading && !error && searched && prescriptions.length === 0 && (
        <EmptyState
          icon={Pill}
          title="No prescriptions found"
          hint="This patient has no prescriptions on record yet."
        />
      )}

      <ul className="space-y-3">
        {prescriptions
          .slice()
          .sort(
            (a, b) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          )
          .map((rx) => (
            <Card key={rx.id} as="li" className="p-0">
              <Link
                href={`/prescriptions/${rx.id}`}
                className="block rounded-xl p-4 transition-colors hover:bg-card-2/50"
              >
                <div className="mb-2 flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-medium text-ink">
                      {format(new Date(rx.createdAt), "MMM d, yyyy")}
                    </p>
                    <p className="mt-0.5 text-xs text-ink-faint">
                      {rx.issuedAt
                        ? `Issued ${format(new Date(rx.issuedAt), "MMM d, yyyy")}`
                        : "Not yet issued"}
                      {rx.validUntil ? ` · Valid until ${rx.validUntil}` : ""}
                    </p>
                    {rx.diagnosisCodes && (
                      <p className="tabular mt-0.5 text-xs text-ink-muted">
                        {rx.diagnosisCodes}
                      </p>
                    )}
                  </div>
                  <StatusBadge status={statusOf(PRESCRIPTION_STATUS, rx.status)} />
                </div>
                {rx.medications && rx.medications.length > 0 && (
                  <div className="flex flex-wrap gap-1">
                    {rx.medications.slice(0, 3).map((m) => (
                      <Badge key={m.id} tone="neutral">
                        {m.medicationName} {m.dosage}
                      </Badge>
                    ))}
                    {rx.medications.length > 3 && (
                      <span className="text-xs text-ink-faint">
                        +{rx.medications.length - 3} more
                      </span>
                    )}
                  </div>
                )}
              </Link>
            </Card>
          ))}
      </ul>
    </div>
  );
}

export default function PrescriptionsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Pharmacy"
        title="Prescriptions"
        description="Look up a patient's prescriptions, then open one to issue or dispatch it."
      />
      <Suspense fallback={<ListSkeleton rows={4} />}>
        <PrescriptionList />
      </Suspense>
    </div>
  );
}
