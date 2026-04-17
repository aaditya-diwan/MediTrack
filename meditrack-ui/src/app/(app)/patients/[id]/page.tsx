"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { use } from "react";
import { PatientResponse, MedicalRecordResponse } from "@/lib/types";
import MedicalRecordList from "@/components/MedicalRecordList";
import { format } from "date-fns";

type Tab = "overview" | "records" | "order-labs";

export default function PatientDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [tab, setTab] = useState<Tab>("overview");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`/api/patients/${id}`)
      .then((r) => r.json())
      .then((data) => {
        setPatient(data);
        setLoading(false);
      });
  }, [id]);

  if (loading) return <p className="text-slate-500">Loading…</p>;
  if (!patient) return <p className="text-red-500">Patient not found.</p>;

  return (
    <div>
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">
            {patient.firstName} {patient.lastName}
          </h1>
          <p className="text-slate-500 text-sm mt-0.5">
            MRN: {patient.mrn} · DOB:{" "}
            {format(new Date(patient.dateOfBirth), "MMM d, yyyy")}
          </p>
        </div>
        <Link
          href={`/insurance?patientId=${patient.id}`}
          className="text-sm text-slate-600 border border-slate-300 rounded-lg px-3 py-1.5 hover:bg-slate-100 transition-colors"
        >
          View Insurance
        </Link>
      </div>

      <div className="flex gap-1 border-b border-slate-200 mb-6">
        {(["overview", "records", "order-labs"] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 text-sm capitalize transition-colors ${
              tab === t
                ? "border-b-2 border-slate-800 text-slate-800 font-medium"
                : "text-slate-500 hover:text-slate-700"
            }`}
          >
            {t === "order-labs" ? "Order Labs" : t === "records" ? "Medical Records" : "Overview"}
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
  const fields: [string, string][] = [
    ["Email", patient.email],
    ["Phone", patient.phoneNumber],
    ["Address", patient.address],
    ["Insurance Provider", patient.insuranceProvider],
    ["Policy #", patient.insurancePolicyNumber],
    ["SSN", patient.ssn],
  ];
  return (
    <dl className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
      {fields.map(([label, value]) => (
        <div key={label}>
          <dt className="text-slate-400">{label}</dt>
          <dd className="text-slate-800 font-medium">{value}</dd>
        </div>
      ))}
    </dl>
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
      <div className="flex justify-end mb-4">
        <Link
          href={`/patients/${patientId}/records/new`}
          className="bg-slate-800 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-700 transition-colors"
        >
          + Add Record
        </Link>
      </div>
      <MedicalRecordList records={records} />
    </div>
  );
}

function OrderLabsTab({ patient }: { patient: PatientResponse }) {
  const [result, setResult] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setResult(null);
    setLoading(true);
    const form = new FormData(e.currentTarget);
    const body = {
      testCode: form.get("testCode"),
      priority: form.get("priority"),
      doctorId: form.get("doctorId"),
      notes: form.get("notes") || undefined,
    };
    const res = await fetch(`/api/patients/${patient.ssn}/order-labs/`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    setLoading(false);
    if (!res.ok) {
      setError("Failed to place lab order.");
      return;
    }
    const text = await res.text();
    setResult(text || "Order placed successfully.");
  }

  return (
    <div className="max-w-lg">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Test Code
          </label>
          <input
            name="testCode"
            required
            placeholder="e.g. CBC, BMP"
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Priority
          </label>
          <select
            name="priority"
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
          >
            <option value="ROUTINE">Routine</option>
            <option value="URGENT">Urgent</option>
            <option value="STAT">STAT</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Doctor ID
          </label>
          <input
            name="doctorId"
            required
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Notes (optional)
          </label>
          <textarea
            name="notes"
            rows={3}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
          />
        </div>
        {error && <p className="text-red-500 text-sm">{error}</p>}
        {result && <p className="text-green-700 text-sm">{result}</p>}
        <button
          type="submit"
          disabled={loading}
          className="bg-slate-800 text-white text-sm px-6 py-2 rounded-lg hover:bg-slate-700 disabled:opacity-50 transition-colors"
        >
          {loading ? "Ordering…" : "Place Order"}
        </button>
      </form>
    </div>
  );
}
