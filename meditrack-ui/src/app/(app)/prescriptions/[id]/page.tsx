"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import { format } from "date-fns";
import { toast } from "sonner";
import { PrescriptionResponse } from "@/lib/types";

const STATUS_STYLES: Record<string, string> = {
  DRAFT: "bg-yellow-100 text-yellow-700",
  ISSUED: "bg-green-100 text-green-700",
  SENT_TO_PHARMACY: "bg-blue-100 text-blue-700",
  SENT_TO_LAB: "bg-purple-100 text-purple-700",
  FULFILLED: "bg-slate-100 text-slate-600",
};

export default function PrescriptionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [rx, setRx] = useState<PrescriptionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [acting, setActing] = useState(false);

  useEffect(() => {
    fetch(`/api/prescriptions/${id}`)
      .then((r) => r.json())
      .then((data) => { setRx(data); setLoading(false); });
  }, [id]);

  async function issue() {
    setActing(true);
    const res = await fetch(`/api/prescriptions/${id}/issue`, { method: "POST" });
    setActing(false);
    if (!res.ok) { toast.error("Failed to issue."); return; }
    setRx(await res.json());
    toast.success("Prescription issued.");
  }

  async function sendToPharmacy() {
    setActing(true);
    const res = await fetch(`/api/prescriptions/${id}/send-pharmacy`, { method: "POST" });
    setActing(false);
    if (!res.ok) { toast.error("Failed to send to pharmacy."); return; }
    setRx(await res.json());
    toast.success("Sent to pharmacy.");
  }

  async function sendToLab() {
    setActing(true);
    const res = await fetch(`/api/prescriptions/${id}/send-lab`, { method: "POST" });
    setActing(false);
    if (!res.ok) { toast.error("Failed to send to lab."); return; }
    setRx(await res.json());
    toast.success("Sent to lab.");
  }

  function downloadPdf() {
    const a = document.createElement("a");
    a.href = `/api/prescriptions/${id}/pdf`;
    a.download = `prescription-${id.slice(0, 8)}.pdf`;
    a.click();
  }

  if (loading) return <p className="text-slate-500">Loading…</p>;
  if (!rx) return <p className="text-red-500">Prescription not found.</p>;

  const canIssue = rx.status === "DRAFT";
  const canDispatch = rx.status === "ISSUED" || rx.status === "SENT_TO_PHARMACY" || rx.status === "SENT_TO_LAB";

  return (
    <div className="max-w-xl">
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-800">Prescription</h1>
          <p className="text-slate-500 text-sm mt-0.5">
            {format(new Date(rx.createdAt), "MMM d, yyyy")}
            {rx.issuedAt ? ` · Issued ${format(new Date(rx.issuedAt), "MMM d")}` : ""}
            {rx.validUntil ? ` · Valid until ${rx.validUntil}` : ""}
          </p>
        </div>
        <span className={`text-xs px-3 py-1 rounded-full font-medium ${STATUS_STYLES[rx.status] ?? "bg-slate-100 text-slate-500"}`}>
          {rx.status.replace(/_/g, " ")}
        </span>
      </div>

      {rx.consultationNotes && (
        <div className="mb-4">
          <h2 className="text-sm font-semibold text-slate-700 mb-1">Consultation Notes</h2>
          <p className="text-sm text-slate-600 border-l-4 border-slate-200 pl-3">{rx.consultationNotes}</p>
        </div>
      )}

      {rx.diagnosisCodes && (
        <div className="mb-4">
          <h2 className="text-sm font-semibold text-slate-700 mb-1">Diagnosis Codes</h2>
          <p className="text-sm text-slate-600">{rx.diagnosisCodes}</p>
        </div>
      )}

      {rx.medications && rx.medications.length > 0 && (
        <div className="mb-5">
          <h2 className="text-sm font-semibold text-slate-700 mb-2">Medications</h2>
          <div className="space-y-2">
            {rx.medications.map((m) => (
              <div key={m.id} className="border border-slate-200 rounded-lg px-4 py-3 bg-white text-sm">
                <div className="flex items-baseline gap-2">
                  <span className="font-medium text-slate-800">{m.medicationName}</span>
                  {m.genericName && <span className="text-slate-400 text-xs">({m.genericName})</span>}
                </div>
                <p className="text-slate-500 text-xs mt-0.5">
                  {m.dosage} · {m.frequency}
                  {m.duration ? ` · ${m.duration}` : ""}
                  {m.route ? ` · ${m.route}` : ""}
                </p>
                {m.instructions && <p className="text-slate-400 text-xs mt-0.5">{m.instructions}</p>}
              </div>
            ))}
          </div>
        </div>
      )}

      {rx.labOrders && rx.labOrders.length > 0 && (
        <div className="mb-5">
          <h2 className="text-sm font-semibold text-slate-700 mb-2">Lab Orders</h2>
          <div className="space-y-2">
            {rx.labOrders.map((l) => (
              <div key={l.id} className="border border-slate-200 rounded-lg px-4 py-3 bg-white text-sm">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-slate-800">{l.testName}</span>
                  <span className="text-slate-400 text-xs">{l.testCode}</span>
                  <span className={`text-xs px-1.5 py-0.5 rounded ${
                    l.urgency === "STAT" ? "bg-red-100 text-red-600"
                    : l.urgency === "URGENT" ? "bg-amber-100 text-amber-700"
                    : "bg-slate-100 text-slate-500"
                  }`}>{l.urgency}</span>
                </div>
                {l.clinicalIndication && (
                  <p className="text-slate-400 text-xs mt-0.5">{l.clinicalIndication}</p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="flex gap-3 flex-wrap pt-2">
        {canIssue && (
          <button onClick={issue} disabled={acting}
            className="bg-green-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-green-700 disabled:opacity-40 transition-colors">
            {acting ? "…" : "Issue Prescription"}
          </button>
        )}
        {canDispatch && (
          <>
            <button onClick={sendToPharmacy} disabled={acting}
              className="text-sm px-4 py-2 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 disabled:opacity-40 transition-colors">
              {acting ? "…" : "Send to Pharmacy"}
            </button>
            {rx.labOrders && rx.labOrders.length > 0 && (
              <button onClick={sendToLab} disabled={acting}
                className="text-sm px-4 py-2 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 disabled:opacity-40 transition-colors">
                {acting ? "…" : "Send to Lab"}
              </button>
            )}
            <button onClick={downloadPdf}
              className="text-sm px-4 py-2 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 transition-colors">
              Download PDF
            </button>
          </>
        )}
      </div>
    </div>
  );
}
