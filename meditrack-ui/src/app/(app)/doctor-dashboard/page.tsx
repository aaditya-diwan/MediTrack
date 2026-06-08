"use client";

import { useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { format, isToday, isTomorrow, parseISO } from "date-fns";
import { AppointmentResponse, AppointmentStatus } from "@/lib/types";

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-700",
  CONFIRMED: "bg-blue-100 text-blue-700",
  IN_PROGRESS: "bg-indigo-100 text-indigo-700",
  COMPLETED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-600",
  NO_SHOW: "bg-slate-100 text-slate-500",
};

function dayLabel(dateStr: string) {
  const d = parseISO(dateStr);
  if (isToday(d)) return "Today";
  if (isTomorrow(d)) return "Tomorrow";
  return format(d, "EEE, MMM d");
}

function DoctorSchedule() {
  const searchParams = useSearchParams();
  const [input, setInput] = useState(searchParams.get("doctorId") ?? "");
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  async function load(id: string) {
    if (!id.trim()) return;
    setLoading(true);
    setSearched(true);
    const res = await fetch(`/api/appointments/doctor/${id}`);
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

  const active = appointments.filter((a) => a.status !== "CANCELLED" && a.status !== "NO_SHOW");
  const sorted = active.slice().sort(
    (a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime()
  );

  const byDay = sorted.reduce<Record<string, AppointmentResponse[]>>((acc, a) => {
    const day = a.scheduledAt.slice(0, 10);
    (acc[day] ??= []).push(a);
    return acc;
  }, {});

  const todayKey = new Date().toISOString().slice(0, 10);
  const todayAppts = byDay[todayKey] ?? [];
  const upcoming = Object.entries(byDay).filter(([d]) => d > todayKey);

  return (
    <div>
      <form onSubmit={handleSubmit} className="flex gap-2 mb-6">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Doctor UUID"
          className="flex-1 input"
        />
        <button
          type="submit"
          className="bg-slate-800 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-700 transition-colors"
        >
          Load Schedule
        </button>
      </form>

      {loading && <p className="text-slate-500 text-sm">Loading…</p>}
      {!loading && searched && appointments.length === 0 && (
        <p className="text-slate-500 text-sm">No appointments found.</p>
      )}

      {todayAppts.length > 0 && (
        <div className="mb-8">
          <h2 className="text-base font-semibold text-slate-700 mb-3">Today&apos;s Queue</h2>
          <div className="space-y-2">
            {todayAppts.map((a) => <AppointmentRow key={a.id} appt={a} />)}
          </div>
        </div>
      )}

      {upcoming.map(([day, appts]) => (
        <div key={day} className="mb-6">
          <h2 className="text-sm font-medium text-slate-500 mb-2">{dayLabel(day)}</h2>
          <div className="space-y-2">
            {appts.map((a) => <AppointmentRow key={a.id} appt={a} />)}
          </div>
        </div>
      ))}
    </div>
  );
}

function AppointmentRow({ appt }: { appt: AppointmentResponse }) {
  return (
    <Link
      href={`/doctor-dashboard/consultation/${appt.id}`}
      className="flex items-center justify-between border border-slate-200 rounded-xl px-4 py-3 hover:border-slate-400 hover:shadow-sm transition-all bg-white"
    >
      <div className="text-sm">
        <span className="font-medium text-slate-800">
          {format(new Date(appt.scheduledAt), "h:mm a")}
        </span>
        <span className="text-slate-400 mx-2">·</span>
        <span className="text-slate-600 capitalize">{appt.type.replace(/_/g, " ").toLowerCase()}</span>
        {appt.reasonForVisit && (
          <span className="text-slate-400 ml-2 text-xs">— {appt.reasonForVisit}</span>
        )}
      </div>
      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_STYLES[appt.status]}`}>
        {appt.status.replace(/_/g, " ")}
      </span>
    </Link>
  );
}

export default function DoctorDashboardPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-2">Doctor Dashboard</h1>
      <p className="text-slate-500 text-sm mb-6">Enter your Doctor UUID to view your schedule.</p>
      <Suspense fallback={<p className="text-slate-500 text-sm">Loading…</p>}>
        <DoctorSchedule />
      </Suspense>
    </div>
  );
}
