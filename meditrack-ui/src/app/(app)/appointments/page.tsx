"use client";

import { useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { format } from "date-fns";
import { AppointmentResponse, AppointmentStatus } from "@/lib/types";

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-700",
  CONFIRMED: "bg-blue-100 text-blue-700",
  IN_PROGRESS: "bg-indigo-100 text-indigo-700",
  COMPLETED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-600",
  NO_SHOW: "bg-slate-100 text-slate-500",
};

function AppointmentsList() {
  const searchParams = useSearchParams();
  const [input, setInput] = useState(searchParams.get("patientId") ?? "");
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  async function load(id: string) {
    if (!id.trim()) return;
    setLoading(true);
    setSearched(true);
    const res = await fetch(`/api/appointments/patient/${id}`);
    setLoading(false);
    if (res.ok) {
      const data = await res.json();
      setAppointments(Array.isArray(data) ? data : []);
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
      {!loading && searched && appointments.length === 0 && (
        <p className="text-slate-500 text-sm">No appointments found.</p>
      )}

      <div className="space-y-3">
        {appointments
          .slice()
          .sort((a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime())
          .map((a) => (
            <Link
              key={a.id}
              href={`/appointments/${a.id}`}
              className="block border border-slate-200 rounded-xl p-4 hover:border-slate-400 hover:shadow-sm transition-all bg-white"
            >
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-slate-800">
                    {format(new Date(a.scheduledAt), "MMM d, yyyy 'at' h:mm a")}
                  </p>
                  <p className="text-sm text-slate-500 mt-0.5 capitalize">
                    {a.type.replace(/_/g, " ").toLowerCase()}
                    {a.reasonForVisit ? ` · ${a.reasonForVisit}` : ""}
                  </p>
                </div>
                <span className={`text-xs px-2 py-1 rounded-full font-medium ${STATUS_STYLES[a.status]}`}>
                  {a.status.replace(/_/g, " ")}
                </span>
              </div>
            </Link>
          ))}
      </div>
    </div>
  );
}

export default function AppointmentsPage() {
  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Appointments</h1>
        <Link
          href="/appointments/book"
          className="bg-slate-800 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-700 transition-colors"
        >
          + Book Appointment
        </Link>
      </div>
      <Suspense fallback={<p className="text-slate-500 text-sm">Loading…</p>}>
        <AppointmentsList />
      </Suspense>
    </div>
  );
}
