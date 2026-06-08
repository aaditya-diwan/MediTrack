"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import Link from "next/link";
import { format } from "date-fns";
import { toast } from "sonner";
import { AppointmentResponse, AppointmentStatus } from "@/lib/types";

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-700",
  CONFIRMED: "bg-blue-100 text-blue-700",
  IN_PROGRESS: "bg-indigo-100 text-indigo-700",
  COMPLETED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-600",
  NO_SHOW: "bg-slate-100 text-slate-500",
};

const NEXT_STATUSES: Partial<Record<AppointmentStatus, AppointmentStatus[]>> = {
  CONFIRMED: ["IN_PROGRESS", "CANCELLED", "NO_SHOW"],
  IN_PROGRESS: ["COMPLETED", "CANCELLED"],
};

export default function AppointmentDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [appt, setAppt] = useState<AppointmentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    fetch(`/api/appointments/${id}`)
      .then((r) => r.json())
      .then((data) => { setAppt(data); setLoading(false); });
  }, [id]);

  async function updateStatus(status: AppointmentStatus) {
    setUpdating(true);
    const res = await fetch(`/api/appointments/${id}/status?status=${status}`, { method: "PUT" });
    setUpdating(false);
    if (!res.ok) { toast.error("Failed to update status."); return; }
    const updated = await res.json();
    setAppt(updated);
    toast.success(`Marked as ${status.replace(/_/g, " ")}`);
  }

  if (loading) return <p className="text-slate-500">Loading…</p>;
  if (!appt) return <p className="text-red-500">Appointment not found.</p>;

  const nextStatuses = NEXT_STATUSES[appt.status] ?? [];

  return (
    <div className="max-w-xl">
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">
            {format(new Date(appt.scheduledAt), "MMM d, yyyy")}
          </h1>
          <p className="text-slate-500 text-sm mt-0.5">
            {format(new Date(appt.scheduledAt), "h:mm a")} ·{" "}
            {appt.type.replace(/_/g, " ").toLowerCase()}
          </p>
        </div>
        <span className={`text-xs px-3 py-1 rounded-full font-medium ${STATUS_STYLES[appt.status]}`}>
          {appt.status.replace(/_/g, " ")}
        </span>
      </div>

      <dl className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm mb-6 border border-slate-200 rounded-xl p-4 bg-white">
        {[
          ["Patient ID", appt.patientId],
          ["Doctor ID", appt.doctorId],
          ["Reason", appt.reasonForVisit || "—"],
          ["Notes", appt.notes || "—"],
          ["Booked", format(new Date(appt.createdAt), "MMM d, yyyy")],
        ].map(([label, val]) => (
          <div key={label}>
            <dt className="text-slate-400">{label}</dt>
            <dd className="text-slate-800 font-medium break-all">{val}</dd>
          </div>
        ))}
      </dl>

      {nextStatuses.length > 0 && (
        <div className="flex gap-2 flex-wrap mb-6">
          {nextStatuses.map((s) => (
            <button
              key={s}
              onClick={() => updateStatus(s)}
              disabled={updating}
              className={`text-sm px-4 py-2 rounded-lg border transition-colors disabled:opacity-40 ${
                s === "CANCELLED" || s === "NO_SHOW"
                  ? "border-red-300 text-red-600 hover:bg-red-50"
                  : "border-slate-300 text-slate-700 hover:bg-slate-100"
              }`}
            >
              {updating ? "…" : `Mark ${s.replace(/_/g, " ")}`}
            </button>
          ))}
        </div>
      )}

      <div className="flex gap-3">
        <Link
          href={`/doctor-dashboard/consultation/${appt.id}`}
          className="text-sm px-4 py-2 rounded-lg bg-slate-800 text-white hover:bg-slate-700 transition-colors"
        >
          Open Consultation
        </Link>
        <Link
          href={`/prescriptions?patientId=${appt.patientId}`}
          className="text-sm px-4 py-2 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100 transition-colors"
        >
          View Prescriptions
        </Link>
      </div>
    </div>
  );
}
