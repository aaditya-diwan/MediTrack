"use client";

import { useEffect, useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { format, isToday, isTomorrow, parseISO } from "date-fns";
import { AlertTriangle, CalendarClock } from "lucide-react";
import { AppointmentResponse } from "@/lib/types";
import {
  APPOINTMENT_STATUS,
  Button,
  EmptyState,
  ListSkeleton,
  PageHeader,
  StatusBadge,
  statusOf,
} from "@/components/ui";
import { DoctorPicker } from "@/components/DoctorPicker";

function dayLabel(dateStr: string) {
  const d = parseISO(dateStr);
  if (isToday(d)) return "Today";
  if (isTomorrow(d)) return "Tomorrow";
  return format(d, "EEE, MMM d");
}

function AppointmentRow({ appt }: { appt: AppointmentResponse }) {
  return (
    <li className="relative pl-6">
      <span
        aria-hidden
        className="absolute left-0 top-5 size-2.5 -translate-x-1/2 rounded-full border-2 border-brand bg-card"
      />
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-card px-4 py-3">
        <div className="flex min-w-0 items-baseline gap-3 text-sm">
          <span className="tabular shrink-0 font-medium text-ink">
            {format(new Date(appt.scheduledAt), "h:mm a")}
          </span>
          <span className="tabular shrink-0 text-xs text-ink-muted">
            Patient …{appt.patientId.slice(-6)}
          </span>
          <span className="capitalize text-ink-muted">
            {appt.type.replace(/_/g, " ").toLowerCase()}
          </span>
          {appt.reasonForVisit && (
            <span className="truncate text-xs text-ink-faint">
              — {appt.reasonForVisit}
            </span>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <StatusBadge status={statusOf(APPOINTMENT_STATUS, appt.status)} />
          <Link
            href={`/doctor-dashboard/consultation/${appt.id}`}
            className="inline-flex items-center rounded-lg border border-line-strong bg-card px-2.5 py-1.5 text-xs font-medium text-ink transition-colors hover:border-brand hover:text-brand-ink"
          >
            Open consultation
          </Link>
        </div>
      </div>
    </li>
  );
}

function Timeline({ appointments }: { appointments: AppointmentResponse[] }) {
  return (
    <ul className="space-y-2 border-l border-line-strong">
      {appointments.map((a) => (
        <AppointmentRow key={a.id} appt={a} />
      ))}
    </ul>
  );
}

function DoctorSchedule() {
  const searchParams = useSearchParams();
  const [doctorId, setDoctorId] = useState<string | null>(
    searchParams.get("doctorId"),
  );
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [error, setError] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [loadedKey, setLoadedKey] = useState<string | null>(null);
  const requestKey = doctorId ? `${doctorId}:${reloadKey}` : null;
  const loading = requestKey !== null && loadedKey !== requestKey;

  useEffect(() => {
    if (!doctorId || !requestKey) return;
    let cancelled = false;
    fetch(`/api/appointments/doctor/${doctorId}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((data) => {
        if (cancelled) return;
        setAppointments(Array.isArray(data) ? data : []);
        setError(false);
        setLoadedKey(requestKey);
      })
      .catch(() => {
        if (cancelled) return;
        setError(true);
        setAppointments([]);
        setLoadedKey(requestKey);
      });
    return () => {
      cancelled = true;
    };
  }, [doctorId, requestKey]);

  const active = appointments.filter(
    (a) => a.status !== "CANCELLED" && a.status !== "NO_SHOW",
  );
  const sorted = active
    .slice()
    .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime());

  const byDay = sorted.reduce<Record<string, AppointmentResponse[]>>((acc, a) => {
    const day = a.scheduledAt.slice(0, 10);
    (acc[day] ??= []).push(a);
    return acc;
  }, {});

  const todayKey = new Date().toISOString().slice(0, 10);
  const todayAppts = byDay[todayKey] ?? [];
  const upcoming = Object.entries(byDay).filter(([d]) => d > todayKey);

  return (
    <div className="space-y-6">
      <div className="max-w-md">
        <DoctorPicker
          selectedId={doctorId}
          onSelect={(doc) => {
            setDoctorId(doc?.id ?? null);
            setError(false);
            if (!doc) setAppointments([]);
          }}
        />
      </div>

      {error && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-danger/25 bg-danger-tint px-4 py-3"
        >
          <div className="flex items-center gap-2 text-sm text-danger">
            <AlertTriangle className="size-4 shrink-0" aria-hidden />
            Failed to load the schedule.
          </div>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => {
              if (!doctorId) return;
              setError(false);
              setReloadKey((k) => k + 1);
            }}
          >
            Retry
          </Button>
        </div>
      )}

      {loading && <ListSkeleton rows={4} />}

      {!loading && !error && !doctorId && (
        <EmptyState
          icon={CalendarClock}
          title="Select a doctor to view their schedule"
          hint="Pick a doctor above to see today's queue and upcoming appointments."
        />
      )}

      {!loading && !error && doctorId && sorted.length === 0 && (
        <EmptyState
          icon={CalendarClock}
          title="No upcoming appointments"
          hint="This doctor has no active appointments on the schedule."
        />
      )}

      {!loading && !error && sorted.length > 0 && (
        <>
          <section>
            <h2 className="font-display mb-3 text-sm font-semibold uppercase tracking-wide text-ink-muted">
              Today&apos;s queue
            </h2>
            {todayAppts.length > 0 ? (
              <Timeline appointments={todayAppts} />
            ) : (
              <p className="text-sm text-ink-faint">Nothing scheduled for today.</p>
            )}
          </section>

          {upcoming.map(([day, appts]) => (
            <section key={day}>
              <h2 className="font-display mb-3 text-sm font-semibold uppercase tracking-wide text-ink-muted">
                {dayLabel(day)}
              </h2>
              <Timeline appointments={appts} />
            </section>
          ))}
        </>
      )}
    </div>
  );
}

export default function DoctorDashboardPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Clinical"
        title="Doctor dashboard"
        description="Pick a doctor to see today's queue and the days ahead."
      />
      <Suspense fallback={<ListSkeleton rows={4} />}>
        <DoctorSchedule />
      </Suspense>
    </div>
  );
}
