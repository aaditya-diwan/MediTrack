"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LabOrderRequest, DiagnosisCodeDto, TestInfoDto, Priority } from "@/lib/types";

const PRIORITIES: Priority[] = ["ROUTINE", "URGENT", "STAT"];

export default function LabOrdersPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [diagCodes, setDiagCodes] = useState<DiagnosisCodeDto[]>([
    { system: "", code: "", description: "" },
  ]);
  const [tests, setTests] = useState<TestInfoDto[]>([
    { testCode: "", testName: "", specimenType: "", clinicalNotes: "" },
  ]);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setLoading(true);
    const form = new FormData(e.currentTarget);
    const body: LabOrderRequest = {
      patientId: form.get("patientId") as string,
      facilityId: form.get("facilityId") as string,
      orderingPhysicianId: form.get("orderingPhysicianId") as string,
      preAuthorizationId: (form.get("preAuthorizationId") as string) || undefined,
      orderTimestamp: new Date().toISOString(),
      priority: form.get("priority") as Priority,
      diagnosisCodes: diagCodes.filter((d) => d.code),
      tests: tests.filter((t) => t.testCode),
    };
    const res = await fetch("/api/lab/orders/", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    setLoading(false);
    if (!res.ok) {
      setError("Failed to create lab order.");
      return;
    }
    const { id } = await res.json();
    router.push(`/lab/orders/${id}`);
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">New Lab Order</h1>
      <form onSubmit={handleSubmit} className="space-y-6">
        <section>
          <h2 className="text-sm font-semibold text-slate-600 uppercase tracking-wide mb-3">
            Order Details
          </h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Patient ID</label>
              <input name="patientId" required className="input" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Facility ID</label>
              <input name="facilityId" required className="input" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Ordering Physician ID</label>
              <input name="orderingPhysicianId" required className="input" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Pre-Auth ID (optional)</label>
              <input name="preAuthorizationId" className="input" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Priority</label>
              <select name="priority" className="input">
                {PRIORITIES.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
          </div>
        </section>

        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-slate-600 uppercase tracking-wide">
              Diagnosis Codes
            </h2>
            <button
              type="button"
              onClick={() =>
                setDiagCodes([...diagCodes, { system: "", code: "", description: "" }])
              }
              className="text-xs text-slate-600 border border-slate-300 px-2 py-1 rounded hover:bg-slate-100"
            >
              + Add
            </button>
          </div>
          {diagCodes.map((d, i) => (
            <div key={i} className="grid grid-cols-3 gap-2 mb-2">
              <input
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
                placeholder="Code"
                value={d.code}
                onChange={(e) => {
                  const next = [...diagCodes];
                  next[i] = { ...next[i], code: e.target.value };
                  setDiagCodes(next);
                }}
                className="input"
              />
              <input
                placeholder="Description"
                value={d.description}
                onChange={(e) => {
                  const next = [...diagCodes];
                  next[i] = { ...next[i], description: e.target.value };
                  setDiagCodes(next);
                }}
                className="input"
              />
            </div>
          ))}
        </section>

        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-slate-600 uppercase tracking-wide">
              Tests
            </h2>
            <button
              type="button"
              onClick={() =>
                setTests([...tests, { testCode: "", testName: "", specimenType: "", clinicalNotes: "" }])
              }
              className="text-xs text-slate-600 border border-slate-300 px-2 py-1 rounded hover:bg-slate-100"
            >
              + Add
            </button>
          </div>
          {tests.map((t, i) => (
            <div key={i} className="grid grid-cols-2 gap-2 mb-2">
              <input
                placeholder="Test Code"
                value={t.testCode}
                onChange={(e) => {
                  const next = [...tests];
                  next[i] = { ...next[i], testCode: e.target.value };
                  setTests(next);
                }}
                className="input"
              />
              <input
                placeholder="Test Name"
                value={t.testName}
                onChange={(e) => {
                  const next = [...tests];
                  next[i] = { ...next[i], testName: e.target.value };
                  setTests(next);
                }}
                className="input"
              />
              <input
                placeholder="Specimen Type"
                value={t.specimenType}
                onChange={(e) => {
                  const next = [...tests];
                  next[i] = { ...next[i], specimenType: e.target.value };
                  setTests(next);
                }}
                className="input"
              />
              <input
                placeholder="Clinical Notes (optional)"
                value={t.clinicalNotes ?? ""}
                onChange={(e) => {
                  const next = [...tests];
                  next[i] = { ...next[i], clinicalNotes: e.target.value };
                  setTests(next);
                }}
                className="input"
              />
            </div>
          ))}
        </section>

        {error && <p className="text-red-500 text-sm">{error}</p>}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={loading}
            className="bg-slate-800 text-white text-sm px-6 py-2 rounded-lg hover:bg-slate-700 disabled:opacity-50 transition-colors"
          >
            {loading ? "Submitting…" : "Submit Order"}
          </button>
          <button
            type="button"
            onClick={() => router.back()}
            className="text-sm px-6 py-2 rounded-lg border border-slate-300 hover:bg-slate-100 transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
