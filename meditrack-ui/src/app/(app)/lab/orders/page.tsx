"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import {
  Button,
  Card,
  CardTitle,
  Field,
  PageHeader,
} from "@/components/ui";
import { PatientPicker } from "@/components/PatientPicker";
import { DoctorPicker } from "@/components/DoctorPicker";
import type {
  DiagnosisCodeDto,
  DoctorResponse,
  LabOrderRequest,
  PatientResponse,
  Priority,
  TestInfoDto,
} from "@/lib/types";

const PRIORITIES: Priority[] = ["ROUTINE", "URGENT", "STAT"];

function sectionHeadingCls() {
  return "font-display text-sm font-semibold uppercase tracking-wide text-ink-muted";
}

export default function LabOrdersPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [physician, setPhysician] = useState<DoctorResponse | null>(null);
  const [facilityId, setFacilityId] = useState("");
  const [preAuthId, setPreAuthId] = useState("");
  const [priority, setPriority] = useState<Priority>("ROUTINE");
  const [diagCodes, setDiagCodes] = useState<DiagnosisCodeDto[]>([
    { system: "", code: "", description: "" },
  ]);
  const [tests, setTests] = useState<TestInfoDto[]>([
    { testCode: "", testName: "", specimenType: "", clinicalNotes: "" },
  ]);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!patient) {
      setError("Select a patient before submitting the order.");
      return;
    }
    if (!physician) {
      setError("Select the ordering physician before submitting the order.");
      return;
    }
    setError("");
    setLoading(true);
    const body: LabOrderRequest = {
      patientId: patient.id,
      facilityId: facilityId.trim(),
      orderingPhysicianId: physician.id,
      preAuthorizationId: preAuthId.trim() || undefined,
      orderTimestamp: new Date().toISOString(),
      priority,
      diagnosisCodes: diagCodes.filter((d) => d.code),
      tests: tests.filter((t) => t.testCode),
    };
    try {
      const res = await fetch("/api/lab/orders/", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        setError(`Failed to create lab order (HTTP ${res.status}).`);
        return;
      }
      const { id } = await res.json();
      toast.success("Lab order created.");
      router.push(`/lab/orders/${id}`);
    } catch {
      setError("Network error creating the lab order. Check your connection and try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-3xl">
      <PageHeader
        eyebrow="Laboratory"
        title="New lab order"
        description="Order one or more tests for a patient. Results are processed asynchronously and appear on the order page."
      />
      <form onSubmit={handleSubmit} className="space-y-6">
        <Card as="section">
          <CardTitle className="mb-4">Order details</CardTitle>
          <div className="grid gap-4 sm:grid-cols-2">
            <PatientPicker onSelect={setPatient} selected={patient} />
            <DoctorPicker
              label="Ordering physician"
              selectedId={physician?.id ?? null}
              onSelect={setPhysician}
            />
            <Field label="Facility ID" required>
              {(ids) => (
                <input
                  {...ids}
                  required
                  className="input"
                  value={facilityId}
                  onChange={(e) => setFacilityId(e.target.value)}
                />
              )}
            </Field>
            <Field label="Pre-authorization ID" hint="Optional insurance pre-auth reference">
              {(ids) => (
                <input
                  {...ids}
                  className="input tabular"
                  value={preAuthId}
                  onChange={(e) => setPreAuthId(e.target.value)}
                />
              )}
            </Field>
            <Field label="Priority">
              {(ids) => (
                <select
                  {...ids}
                  className="input"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as Priority)}
                >
                  {PRIORITIES.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          </div>
        </Card>

        <Card as="section">
          <div className="mb-4 flex items-center justify-between">
            <h2 className={sectionHeadingCls()}>Diagnosis codes</h2>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() =>
                setDiagCodes([...diagCodes, { system: "", code: "", description: "" }])
              }
            >
              <Plus className="size-3.5" aria-hidden />
              Add code
            </Button>
          </div>
          <div className="space-y-2">
            {diagCodes.map((d, i) => (
              <div key={i} className="grid grid-cols-[1fr_1fr_2fr_auto] gap-2">
                <input
                  aria-label={`Diagnosis ${i + 1} coding system`}
                  placeholder="System (e.g. ICD-10)"
                  value={d.system}
                  onChange={(e) => {
                    const next = [...diagCodes];
                    next[i] = { ...next[i], system: e.target.value };
                    setDiagCodes(next);
                  }}
                  className="input"
                />
                <input
                  aria-label={`Diagnosis ${i + 1} code`}
                  placeholder="Code"
                  value={d.code}
                  onChange={(e) => {
                    const next = [...diagCodes];
                    next[i] = { ...next[i], code: e.target.value };
                    setDiagCodes(next);
                  }}
                  className="input tabular"
                />
                <input
                  aria-label={`Diagnosis ${i + 1} description`}
                  placeholder="Description"
                  value={d.description}
                  onChange={(e) => {
                    const next = [...diagCodes];
                    next[i] = { ...next[i], description: e.target.value };
                    setDiagCodes(next);
                  }}
                  className="input"
                />
                <button
                  type="button"
                  onClick={() => setDiagCodes(diagCodes.filter((_, j) => j !== i))}
                  disabled={diagCodes.length === 1}
                  aria-label={`Remove diagnosis code ${i + 1}`}
                  className="rounded p-2 text-ink-faint hover:text-danger disabled:opacity-40"
                >
                  <Trash2 className="size-4" aria-hidden />
                </button>
              </div>
            ))}
          </div>
        </Card>

        <Card as="section">
          <div className="mb-4 flex items-center justify-between">
            <h2 className={sectionHeadingCls()}>Tests</h2>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() =>
                setTests([
                  ...tests,
                  { testCode: "", testName: "", specimenType: "", clinicalNotes: "" },
                ])
              }
            >
              <Plus className="size-3.5" aria-hidden />
              Add test
            </Button>
          </div>
          <div className="space-y-3">
            {tests.map((t, i) => (
              <div
                key={i}
                className="grid grid-cols-[1fr_1fr_auto] gap-2 rounded-lg border border-line bg-card-2/40 p-3 sm:grid-cols-[1fr_1.5fr_1fr_1.5fr_auto]"
              >
                <input
                  aria-label={`Test ${i + 1} code`}
                  placeholder="Test code"
                  value={t.testCode}
                  onChange={(e) => {
                    const next = [...tests];
                    next[i] = { ...next[i], testCode: e.target.value };
                    setTests(next);
                  }}
                  className="input tabular"
                />
                <input
                  aria-label={`Test ${i + 1} name`}
                  placeholder="Test name"
                  value={t.testName}
                  onChange={(e) => {
                    const next = [...tests];
                    next[i] = { ...next[i], testName: e.target.value };
                    setTests(next);
                  }}
                  className="input"
                />
                <input
                  aria-label={`Test ${i + 1} specimen type`}
                  placeholder="Specimen type"
                  value={t.specimenType}
                  onChange={(e) => {
                    const next = [...tests];
                    next[i] = { ...next[i], specimenType: e.target.value };
                    setTests(next);
                  }}
                  className="input"
                />
                <input
                  aria-label={`Test ${i + 1} clinical notes`}
                  placeholder="Clinical notes (optional)"
                  value={t.clinicalNotes ?? ""}
                  onChange={(e) => {
                    const next = [...tests];
                    next[i] = { ...next[i], clinicalNotes: e.target.value };
                    setTests(next);
                  }}
                  className="input"
                />
                <button
                  type="button"
                  onClick={() => setTests(tests.filter((_, j) => j !== i))}
                  disabled={tests.length === 1}
                  aria-label={`Remove test ${i + 1}`}
                  className="rounded p-2 text-ink-faint hover:text-danger disabled:opacity-40"
                >
                  <Trash2 className="size-4" aria-hidden />
                </button>
              </div>
            ))}
          </div>
        </Card>

        {error && (
          <div
            role="alert"
            className="rounded-xl border border-danger/25 bg-danger-tint p-4 text-sm text-danger"
          >
            {error}
          </div>
        )}
        <div className="flex gap-3">
          <Button type="submit" loading={loading}>
            Submit order
          </Button>
          <Button type="button" variant="secondary" onClick={() => router.back()}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}
