"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AlertTriangle, Stethoscope } from "lucide-react";
import { DoctorResponse, SPECIALIZATIONS, formatSpecialization } from "@/lib/types";
import {
  Badge,
  Button,
  EmptyState,
  Field,
  ListSkeleton,
  PageHeader,
} from "@/components/ui";

function DoctorCard({ doctor }: { doctor: DoctorResponse }) {
  return (
    <Link
      href={`/doctors/${doctor.id}`}
      className="flex items-start gap-4 rounded-xl border border-line bg-card p-5 shadow-[0_1px_2px_rgb(0_0_0/0.04)] transition-all hover:border-brand hover:shadow-sm"
    >
      <span
        aria-hidden
        className="flex size-11 shrink-0 items-center justify-center rounded-full bg-brand-tint text-sm font-semibold text-brand-ink"
      >
        {doctor.firstName?.[0]}
        {doctor.lastName?.[0]}
      </span>
      <span className="min-w-0">
        <span className="flex flex-wrap items-center gap-2">
          <span className="truncate text-sm font-semibold text-ink">{doctor.fullName}</span>
          {!doctor.active && <Badge tone="neutral">Inactive</Badge>}
        </span>
        <span className="mt-1 flex flex-wrap items-center gap-2">
          <Badge tone="brand">{formatSpecialization(doctor.specialization)}</Badge>
          <span className="text-xs text-ink-muted">
            {doctor.yearsOfExperience} yrs experience
          </span>
        </span>
        {doctor.qualifications && (
          <span className="mt-1.5 block truncate text-xs text-ink-faint">
            {doctor.qualifications}
          </span>
        )}
      </span>
    </Link>
  );
}

export default function DoctorsPage() {
  const [specialization, setSpecialization] = useState("");
  const [doctors, setDoctors] = useState<DoctorResponse[]>([]);
  const [error, setError] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [loadedKey, setLoadedKey] = useState<string | null>(null);
  const requestKey = `${specialization}:${reloadKey}`;
  const loading = loadedKey !== requestKey;

  useEffect(() => {
    let cancelled = false;
    const url = specialization
      ? `/api/doctors?specialization=${specialization}`
      : `/api/doctors`;
    fetch(url)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((data) => {
        if (cancelled) return;
        setDoctors(Array.isArray(data) ? data : []);
        setError(false);
        setLoadedKey(requestKey);
      })
      .catch(() => {
        if (cancelled) return;
        setError(true);
        setDoctors([]);
        setLoadedKey(requestKey);
      });
    return () => {
      cancelled = true;
    };
  }, [specialization, requestKey]);

  const retryLoad = () => {
    setError(false);
    setReloadKey((k) => k + 1);
  };

  return (
    <div>
      <PageHeader
        eyebrow="Directory"
        title="Doctors"
        description="Browse the medical staff and open a profile to see availability."
      />

      <div className="mb-6 max-w-xs">
        <Field label="Specialty">
          {(ids) => (
            <select
              {...ids}
              value={specialization}
              onChange={(e) => {
                setSpecialization(e.target.value);
                setError(false);
              }}
              className="input"
            >
              <option value="">All specialties</option>
              {SPECIALIZATIONS.map((s) => (
                <option key={s} value={s}>
                  {formatSpecialization(s)}
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>

      {loading && <ListSkeleton rows={4} />}

      {error && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-danger/25 bg-danger-tint px-4 py-3"
        >
          <div className="flex items-center gap-2 text-sm text-danger">
            <AlertTriangle className="size-4 shrink-0" aria-hidden />
            Failed to load the doctor directory.
          </div>
          <Button variant="secondary" size="sm" onClick={retryLoad}>
            Retry
          </Button>
        </div>
      )}

      {!loading && !error && doctors.length === 0 && (
        <EmptyState
          icon={Stethoscope}
          title="No doctors found"
          hint={
            specialization
              ? `No doctors are registered under ${formatSpecialization(specialization)}.`
              : "No doctors are registered yet."
          }
          action={
            specialization ? (
              <Button variant="secondary" size="sm" onClick={() => setSpecialization("")}>
                Show all specialties
              </Button>
            ) : undefined
          }
        />
      )}

      {!loading && !error && doctors.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {doctors.map((d) => (
            <DoctorCard key={d.id} doctor={d} />
          ))}
        </div>
      )}
    </div>
  );
}
