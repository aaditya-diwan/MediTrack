"use client";

import { useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { format } from "date-fns";
import { PrescriptionResponse } from "@/lib/types";

const STATUS_STYLES: Record<string, string> = {
  DRAFT: "bg-yellow-100 text-yellow-700",
  ISSUED: "bg-green-100 text-green-700",
  SENT_TO_PHARMACY: "bg-blue-100 text-blue-700",
  SENT_TO_LAB: "bg-purple-100 text-purple-700",
  FULFILLED: "bg-slate-100 text-slate-600",
};

function PrescriptionList() {
  const searchParams = useSearchParams();
  const [input, setInput] = useState(searchParams.get("patientId") ?? "");
  const [prescriptions, setPrescriptions] = useState<PrescriptionResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  async function load(id: string) {
    if (!id.trim()) return;
    setLoading(true);
    setSearched(true);
    const res = await fetch(`/api/prescriptions/patient/${id}`);
    setLoading(false);
    if (res.ok) {
      const data = await res.json();
      setPrescriptions(Array.isArray(data) ? data : []);
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    load(input);
  }

  return (
    <div>
      <form onSubmit={handleSubmit} className="flex gap-2 mb-6">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Patient UUID"
          className="flex-1 input"
        />
        <button
          type="submit"
          className="bg-slate-800 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-700 transition-colors"
        >
          Load
        </button>
      </form>

      {loading && <p className="text-slate-500 text-sm">Loading…</p>}
      {!loading && searched && prescriptions.length === 0 && (
        <p className="text-slate-500 text-sm">No prescriptions found.</p>
      )}

      <div className="space-y-3">
        {prescriptions
          .slice()
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .map((rx) => (
            <Link
              key={rx.id}
              href={`/prescriptions/${rx.id}`}
              className="block border border-slate-200 rounded-xl p-4 hover:border-slate-400 hover:shadow-sm transition-all bg-white"
            >
              <div className="flex items-start justify-between mb-2">
                <div>
                  <p className="text-sm font-medium text-slate-800">
                    {format(new Date(rx.createdAt), "MMM d, yyyy")}
                  </p>
                  {rx.diagnosisCodes && (
                    <p className="text-xs text-slate-400 mt-0.5">{rx.diagnosisCodes}</p>
                  )}
                </div>
                <span className={`text-xs px-2 py-1 rounded-full font-medium ${STATUS_STYLES[rx.status] ?? "bg-slate-100 text-slate-500"}`}>
                  {rx.status.replace(/_/g, " ")}
                </span>
              </div>
              {rx.medications && rx.medications.length > 0 && (
                <div className="flex flex-wrap gap-1">
                  {rx.medications.slice(0, 3).map((m) => (
                    <span key={m.id} className="bg-slate-100 text-slate-600 text-xs px-2 py-0.5 rounded">
                      {m.medicationName} {m.dosage}
                    </span>
                  ))}
                  {rx.medications.length > 3 && (
                    <span className="text-slate-400 text-xs">+{rx.medications.length - 3} more</span>
                  )}
                </div>
              )}
            </Link>
          ))}
      </div>
    </div>
  );
}

export default function PrescriptionsPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-6">Prescriptions</h1>
      <Suspense fallback={<p className="text-slate-500 text-sm">Loading…</p>}>
        <PrescriptionList />
      </Suspense>
    </div>
  );
}
