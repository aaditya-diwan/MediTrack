"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import Link from "next/link";
import { AlertTriangle, CalendarPlus } from "lucide-react";
import {
  DoctorResponse,
  AvailabilitySlotResponse,
  formatSpecialization,
} from "@/lib/types";
import {
  Badge,
  Button,
  Card,
  DetailLabel,
  DetailValue,
  EcgLoading,
  EmptyState,
  PageHeader,
} from "@/components/ui";

const DAY_ORDER = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const DAY_ABBREV: Record<string, string> = {
  MONDAY: "Mon",
  TUESDAY: "Tue",
  WEDNESDAY: "Wed",
  THURSDAY: "Thu",
  FRIDAY: "Fri",
  SATURDAY: "Sat",
  SUNDAY: "Sun",
};

function fmtTime(t: string) {
  const [h, m] = t.split(":").map(Number);
  const ampm = h >= 12 ? "PM" : "AM";
  return `${h % 12 || 12}:${String(m).padStart(2, "0")} ${ampm}`;
}

/** Weekly availability as a 7-column week strip: one column per day, slot chips inside. */
function WeekStrip({ slots }: { slots: AvailabilitySlotResponse[] }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-line bg-card p-4">
      <div className="grid min-w-[640px] grid-cols-7 gap-2">
        {DAY_ORDER.map((day) => {
          const daySlots = slots.filter((s) => s.dayOfWeek === day);
          const hasAvailable = daySlots.some((s) => s.available);
          return (
            <div key={day} className="min-w-0">
              <p
                className={`mb-2 text-center text-xs font-semibold uppercase tracking-wide ${
                  hasAvailable ? "text-brand-ink" : "text-ink-faint"
                }`}
              >
                {DAY_ABBREV[day]}
              </p>
              <div className="space-y-1.5">
                {daySlots.length === 0 && (
                  <p className="rounded-lg bg-card-2 py-2 text-center text-[11px] text-ink-faint">
                    Off
                  </p>
                )}
                {daySlots.map((s) => (
                  <div
                    key={s.id}
                    className={`rounded-lg border px-1.5 py-1.5 text-center ${
                      s.available
                        ? "border-brand/20 bg-brand-tint text-brand-ink"
                        : "border-line bg-card-2 text-ink-faint"
                    }`}
                  >
                    <p className="tabular text-[11px] font-medium leading-tight">
                      {fmtTime(s.startTime)}
                      <span aria-hidden> – </span>
                      {fmtTime(s.endTime)}
                    </p>
                    <p className="mt-0.5 text-[10px] opacity-80">
                      {s.available ? `${s.slotDurationMinutes} min slots` : "Unavailable"}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function DoctorDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [doctor, setDoctor] = useState<DoctorResponse | null>(null);
  const [slots, setSlots] = useState<AvailabilitySlotResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      fetch(`/api/doctors/${id}`).then((r) =>
        r.ok ? r.json() : Promise.reject(new Error(String(r.status))),
      ),
      fetch(`/api/doctors/${id}/slots`).then((r) =>
        r.ok ? r.json() : Promise.reject(new Error(String(r.status))),
      ),
    ])
      .then(([doc, sl]) => {
        if (cancelled) return;
        setDoctor(doc);
        setSlots(Array.isArray(sl) ? sl : []);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id, reloadKey]);

  const load = () => {
    setLoading(true);
    setError(false);
    setReloadKey((k) => k + 1);
  };

  if (loading) return <EcgLoading label="Loading profile" />;

  if (error || !doctor) {
    return (
      <div
        role="alert"
        className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-danger/25 bg-danger-tint px-4 py-3"
      >
        <div className="flex items-center gap-2 text-sm text-danger">
          <AlertTriangle className="size-4 shrink-0" aria-hidden />
          Failed to load this doctor&apos;s profile.
        </div>
        <Button variant="secondary" size="sm" onClick={load}>
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        eyebrow="Doctor profile"
        title={doctor.fullName}
        description={`Employee ${doctor.employeeId}`}
        actions={
          <Link
            href={`/appointments/book?doctorId=${doctor.id}`}
            className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-brand-strong"
          >
            <CalendarPlus className="size-4" aria-hidden />
            Book appointment
          </Link>
        }
      />

      <div className="mb-6 flex items-center gap-4">
        <span
          aria-hidden
          className="flex size-14 shrink-0 items-center justify-center rounded-full bg-brand-tint text-lg font-semibold text-brand-ink"
        >
          {doctor.firstName?.[0]}
          {doctor.lastName?.[0]}
        </span>
        <div className="flex flex-wrap items-center gap-2">
          <Badge tone="brand">{formatSpecialization(doctor.specialization)}</Badge>
          <Badge tone={doctor.active ? "ok" : "neutral"}>
            {doctor.active ? "Active" : "Inactive"}
          </Badge>
          <span className="text-sm text-ink-muted">
            {doctor.yearsOfExperience} years of experience
          </span>
        </div>
      </div>

      <Card className="mb-6 max-w-2xl">
        <dl className="grid grid-cols-2 gap-x-8 gap-y-4">
          <div>
            <DetailLabel>Email</DetailLabel>
            <DetailValue>{doctor.email}</DetailValue>
          </div>
          <div>
            <DetailLabel>Phone</DetailLabel>
            <DetailValue mono>{doctor.phone}</DetailValue>
          </div>
          <div>
            <DetailLabel>Experience</DetailLabel>
            <DetailValue>{doctor.yearsOfExperience} years</DetailValue>
          </div>
          <div>
            <DetailLabel>Qualifications</DetailLabel>
            <DetailValue>{doctor.qualifications || "—"}</DetailValue>
          </div>
        </dl>
      </Card>

      {doctor.bio && (
        <p className="mb-8 max-w-2xl border-l-4 border-brand/30 pl-4 text-sm text-ink-muted">
          {doctor.bio}
        </p>
      )}

      <section>
        <h2 className="font-display mb-3 text-sm font-semibold uppercase tracking-wide text-ink-muted">
          Weekly availability
        </h2>
        {slots.length === 0 ? (
          <EmptyState
            title="No availability schedule set"
            hint="This doctor has not published consultation hours yet."
          />
        ) : (
          <WeekStrip slots={slots} />
        )}
      </section>
    </div>
  );
}
