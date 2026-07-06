"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { FlaskConical, Plus, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { PatientResponse, MedicalRecordResponse, DoctorResponse } from "@/lib/types";
import MedicalRecordList from "@/components/MedicalRecordList";
import { DoctorPicker } from "@/components/DoctorPicker";
import {
  Button,
  Card,
  DetailLabel,
  DetailValue,
  EcgLoading,
  Field,
  PageHeader,
} from "@/components/ui";
import { format } from "date-fns";

type Tab = "overview" | "records" | "order-labs";

const TABS: { key: Tab; label: string }[] = [
  { key: "overview", label: "Overview" },
  { key: "records", label: "Medical Records" },
  { key: "order-labs", label: "Order Labs" },
];

export default function PatientDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [tab, setTab] = useState<Tab>("overview");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/patients/${id}`)
      .then((r) => {
        if (!r.ok) throw new Error(`Load failed (${r.status})`);
        return r.json();
      })
      .then((data: PatientResponse) => {
        if (!cancelled) setPatient(data);
      })
      .catch(() => {
        if (!cancelled)
          setError("Could not load this patient chart. Please try again.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id, attempt]);

  function retry() {
    setLoading(true);
    setError(null);
    setAttempt((a) => a + 1);
  }

  if (loading) return <EcgLoading label="Loading chart" />;

  if (error || !patient) {
    return (
      <div className="rounded-xl border border-danger/25 bg-danger-tint p-4">
        <p className="text-sm font-medium text-danger">
          {error ?? "Patient not found."}
        </p>
        <Button variant="secondary" size="sm" className="mt-3" onClick={retry}>
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        eyebrow="Patient chart"
        title={`${patient.firstName} ${patient.lastName}`}
        actions={
          <Link
            href={`/insurance?patientId=${patient.id}`}
            className="inline-flex items-center gap-2 rounded-lg border border-line-strong bg-card px-4 py-2 text-sm font-medium text-ink transition-colors hover:border-brand hover:text-brand-ink"
          >
            <ShieldCheck className="size-4" aria-hidden />
            View insurance
          </Link>
        }
      />

      <Card className="mb-6">
        <dl className="grid grid-cols-2 gap-x-6 gap-y-3 sm:grid-cols-4">
          <div>
            <DetailLabel>MRN</DetailLabel>
            <DetailValue mono>{patient.mrn}</DetailValue>
          </div>
          <div>
            <DetailLabel>Date of birth</DetailLabel>
            <DetailValue mono>
              {format(new Date(patient.dateOfBirth), "MMM d, yyyy")}
            </DetailValue>
          </div>
          <div>
            <DetailLabel>Phone</DetailLabel>
            <DetailValue mono>{patient.phoneNumber}</DetailValue>
          </div>
          <div>
            <DetailLabel>Email</DetailLabel>
            <DetailValue>{patient.email}</DetailValue>
          </div>
        </dl>
      </Card>

      <div
        role="tablist"
        aria-label="Patient chart sections"
        className="mb-6 flex gap-1 border-b border-line"
      >
        {TABS.map((t) => (
          <button
            key={t.key}
            role="tab"
            aria-selected={tab === t.key}
            onClick={() => setTab(t.key)}
            className={`-mb-px px-4 py-2 text-sm transition-colors ${
              tab === t.key
                ? "border-b-2 border-brand font-medium text-ink"
                : "border-b-2 border-transparent text-ink-muted hover:text-ink"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "overview" && <OverviewTab patient={patient} />}
      {tab === "records" && (
        <RecordsTab patientId={patient.id} records={patient.medicalHistory} />
      )}
      {tab === "order-labs" && <OrderLabsTab patient={patient} />}
    </div>
  );
}

function OverviewTab({ patient }: { patient: PatientResponse }) {
  return (
    <Card>
      <dl className="grid grid-cols-1 gap-x-8 gap-y-4 sm:grid-cols-2">
        <div>
          <DetailLabel>Email</DetailLabel>
          <DetailValue>{patient.email}</DetailValue>
        </div>
        <div>
          <DetailLabel>Phone</DetailLabel>
          <DetailValue mono>{patient.phoneNumber}</DetailValue>
        </div>
        <div className="sm:col-span-2">
          <DetailLabel>Address</DetailLabel>
          <DetailValue>{patient.address}</DetailValue>
        </div>
        <div>
          <DetailLabel>Insurance provider</DetailLabel>
          <DetailValue>{patient.insuranceProvider}</DetailValue>
        </div>
        <div>
          <DetailLabel>Policy #</DetailLabel>
          <DetailValue mono>{patient.insurancePolicyNumber}</DetailValue>
        </div>
        <div>
          <DetailLabel>SSN</DetailLabel>
          <DetailValue mono>{patient.ssn}</DetailValue>
        </div>
      </dl>
    </Card>
  );
}

function RecordsTab({
  patientId,
  records,
}: {
  patientId: string;
  records: MedicalRecordResponse[];
}) {
  return (
    <div>
      <div className="mb-4 flex justify-end">
        <Link
          href={`/patients/${patientId}/records/new`}
          className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-brand-strong"
        >
          <Plus className="size-4" aria-hidden />
          Add record
        </Link>
      </div>
      <MedicalRecordList records={records} />
    </div>
  );
}

function OrderLabsTab({ patient }: { patient: PatientResponse }) {
  const [doctor, setDoctor] = useState<DoctorResponse | null>(null);
  const [result, setResult] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setResult(null);
    if (!doctor) {
      setError("Select the ordering doctor.");
      return;
    }
    setLoading(true);
    const form = new FormData(e.currentTarget);
    const body = {
      testCode: form.get("testCode"),
      priority: form.get("priority"),
      doctorId: doctor.id,
      notes: form.get("notes") || undefined,
    };
    try {
      const res = await fetch(`/api/patients/${patient.ssn}/order-labs/`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        setError("Failed to place lab order.");
        return;
      }
      const text = await res.text();
      setResult(text || "Order placed successfully.");
      toast.success("Lab order placed.");
    } catch {
      setError("Could not reach the lab service. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="max-w-lg">
      <div className="mb-4 flex items-center gap-2">
        <FlaskConical className="size-4 text-brand-ink" aria-hidden />
        <h3 className="font-display text-sm font-semibold uppercase tracking-wide text-ink-muted">
          New lab order
        </h3>
      </div>
      <form onSubmit={handleSubmit} className="space-y-4">
        <Field label="Test code" required hint="e.g. CBC, BMP">
          {(ids) => (
            <input name="testCode" required className="input" {...ids} />
          )}
        </Field>
        <Field label="Priority" required>
          {(ids) => (
            <select name="priority" className="input" {...ids}>
              <option value="ROUTINE">Routine</option>
              <option value="URGENT">Urgent</option>
              <option value="STAT">STAT</option>
            </select>
          )}
        </Field>
        <DoctorPicker
          label="Ordering doctor"
          selectedId={doctor?.id ?? null}
          onSelect={setDoctor}
        />
        <Field label="Notes (optional)">
          {(ids) => (
            <textarea name="notes" rows={3} className="input" {...ids} />
          )}
        </Field>

        {error && (
          <div className="rounded-lg border border-danger/25 bg-danger-tint px-3 py-2">
            <p className="text-sm text-danger">{error}</p>
          </div>
        )}
        {result && (
          <div className="rounded-lg border border-ok/25 bg-ok-tint px-3 py-2">
            <p className="text-sm text-ok">{result}</p>
          </div>
        )}

        <Button type="submit" loading={loading}>
          Place order
        </Button>
      </form>
    </Card>
  );
}
