"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import Link from "next/link";
import { DoctorResponse, AvailabilitySlotResponse, formatSpecialization } from "@/lib/types";

const DAY_ORDER = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

function fmtTime(t: string) {
  const [h, m] = t.split(":").map(Number);
  const ampm = h >= 12 ? "PM" : "AM";
  return `${h % 12 || 12}:${String(m).padStart(2, "0")} ${ampm}`;
}

export default function DoctorDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [doctor, setDoctor] = useState<DoctorResponse | null>(null);
  const [slots, setSlots] = useState<AvailabilitySlotResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetch(`/api/doctors/${id}`).then((r) => r.json()),
      fetch(`/api/doctors/${id}/slots`).then((r) => r.json()),
    ]).then(([doc, sl]) => {
      setDoctor(doc);
      setSlots(Array.isArray(sl) ? sl : []);
      setLoading(false);
    });
  }, [id]);

  if (loading) return <p className="text-slate-500">Loading…</p>;
  if (!doctor) return <p className="text-red-500">Doctor not found.</p>;

  const sortedSlots = [...slots].sort(
    (a, b) => DAY_ORDER.indexOf(a.dayOfWeek) - DAY_ORDER.indexOf(b.dayOfWeek)
  );

  return (
    <div>
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">{doctor.fullName}</h1>
          <p className="text-slate-500 text-sm mt-0.5">
            {formatSpecialization(doctor.specialization)} · Employee {doctor.employeeId}
          </p>
        </div>
        <Link
          href={`/appointments/book?doctorId=${doctor.id}`}
          className="bg-slate-800 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-700 transition-colors"
        >
          Book Appointment
        </Link>
      </div>

      <dl className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm mb-8">
        {[
          ["Email", doctor.email],
          ["Phone", doctor.phone],
          ["Experience", `${doctor.yearsOfExperience} years`],
          ["Qualifications", doctor.qualifications || "—"],
        ].map(([label, value]) => (
          <div key={label}>
            <dt className="text-slate-400">{label}</dt>
            <dd className="text-slate-800 font-medium">{value}</dd>
          </div>
        ))}
      </dl>

      {doctor.bio && (
        <p className="text-slate-600 text-sm mb-8 border-l-4 border-slate-200 pl-4">{doctor.bio}</p>
      )}

      <h2 className="text-lg font-semibold text-slate-800 mb-3">Weekly Availability</h2>
      {sortedSlots.length === 0 ? (
        <p className="text-slate-500 text-sm">No availability schedule set.</p>
      ) : (
        <div className="space-y-2">
          {sortedSlots.map((s) => (
            <div
              key={s.id}
              className="flex items-center justify-between border border-slate-200 rounded-lg px-4 py-3 bg-white text-sm"
            >
              <span className="font-medium text-slate-700 capitalize">
                {s.dayOfWeek.charAt(0) + s.dayOfWeek.slice(1).toLowerCase()}
              </span>
              <span className="text-slate-500">
                {fmtTime(s.startTime)} – {fmtTime(s.endTime)}
              </span>
              <span className="text-slate-400">{s.slotDurationMinutes} min slots</span>
              <span
                className={`text-xs px-2 py-0.5 rounded-full ${
                  s.available ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-500"
                }`}
              >
                {s.available ? "Available" : "Unavailable"}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
