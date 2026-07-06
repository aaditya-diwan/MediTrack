"use client";

import { useEffect, useState } from "react";
import type { DoctorResponse as Doctor } from "@/lib/types";
import { formatSpecialization } from "@/lib/types";

/**
 * Doctor selector fed by /api/doctors. Replaces "paste a doctor UUID" inputs.
 */
export function DoctorPicker({
  onSelect,
  selectedId,
  label = "Doctor",
}: {
  onSelect: (doctor: Doctor | null) => void;
  selectedId: string | null;
  label?: string;
}) {
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetch("/api/doctors")
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((data: Doctor[]) => {
        if (!cancelled) setDoctors(Array.isArray(data) ? data : []);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <label className="block text-xs font-medium text-ink-muted">
      {label}
      <select
        className="input mt-1"
        value={selectedId ?? ""}
        onChange={(e) => {
          const doc = doctors.find((d) => d.id === e.target.value) ?? null;
          onSelect(doc);
        }}
      >
        <option value="">
          {failed ? "Doctor list unavailable" : "Select a doctor…"}
        </option>
        {doctors.map((d) => (
          <option key={d.id} value={d.id}>
            Dr. {d.firstName} {d.lastName} — {formatSpecialization(d.specialization)}
          </option>
        ))}
      </select>
    </label>
  );
}
