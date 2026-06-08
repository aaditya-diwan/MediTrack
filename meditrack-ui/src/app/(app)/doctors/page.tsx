"use client";

import { useState } from "react";
import Link from "next/link";
import { DoctorResponse, SPECIALIZATIONS, formatSpecialization } from "@/lib/types";

export default function DoctorsPage() {
  const [specialization, setSpecialization] = useState("");
  const [doctors, setDoctors] = useState<DoctorResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  async function search() {
    setLoading(true);
    setSearched(true);
    const url = specialization
      ? `/api/doctors?specialization=${specialization}`
      : `/api/doctors`;
    const res = await fetch(url);
    setLoading(false);
    if (res.ok) setDoctors(await res.json());
    else setDoctors([]);
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Doctors</h1>
      </div>

      <div className="flex gap-3 mb-6">
        <select
          value={specialization}
          onChange={(e) => setSpecialization(e.target.value)}
          className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400 bg-white"
        >
          <option value="">All Specializations</option>
          {SPECIALIZATIONS.map((s) => (
            <option key={s} value={s}>
              {formatSpecialization(s)}
            </option>
          ))}
        </select>
        <button
          onClick={search}
          className="bg-slate-800 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-700 transition-colors"
        >
          {loading ? "Loading…" : "Search"}
        </button>
      </div>

      {searched && !loading && doctors.length === 0 && (
        <p className="text-slate-500 text-sm">No doctors found.</p>
      )}

      <div className="space-y-3">
        {doctors.map((d) => (
          <Link
            key={d.id}
            href={`/doctors/${d.id}`}
            className="block border border-slate-200 rounded-xl p-4 hover:border-slate-400 hover:shadow-sm transition-all bg-white"
          >
            <div className="flex items-start justify-between">
              <div>
                <p className="font-semibold text-slate-800">{d.fullName}</p>
                <p className="text-sm text-slate-500 mt-0.5">
                  {formatSpecialization(d.specialization)} · {d.yearsOfExperience}y exp
                </p>
                {d.qualifications && (
                  <p className="text-xs text-slate-400 mt-1">{d.qualifications}</p>
                )}
              </div>
              <span
                className={`text-xs px-2 py-1 rounded-full font-medium ${
                  d.active
                    ? "bg-green-100 text-green-700"
                    : "bg-slate-100 text-slate-500"
                }`}
              >
                {d.active ? "Active" : "Inactive"}
              </span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
