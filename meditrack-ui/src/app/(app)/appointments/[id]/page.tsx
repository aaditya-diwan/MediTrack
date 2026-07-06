"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import Link from "next/link";
import { format } from "date-fns";
import { toast } from "sonner";
import { AlertTriangle, ClipboardList, Stethoscope } from "lucide-react";
import { AppointmentResponse, AppointmentStatus } from "@/lib/types";
import {
  APPOINTMENT_STATUS,
  Button,
  Card,
  DetailLabel,
  DetailValue,
  EcgLoading,
  PageHeader,
  StatusBadge,
  statusOf,
} from "@/components/ui";

const NEXT_STATUSES: Partial<Record<AppointmentStatus, AppointmentStatus[]>> = {
  CONFIRMED: ["IN_PROGRESS", "CANCELLED", "NO_SHOW"],
  IN_PROGRESS: ["COMPLETED", "CANCELLED"],
};

export default function AppointmentDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [appt, setAppt] = useState<AppointmentResponse | null>(null);
  const [error, setError] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [updating, setUpdating] = useState<AppointmentStatus | null>(null);
  const loading = appt === null && !error;

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/appointments/${id}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((data: AppointmentResponse) => {
        if (!cancelled) setAppt(data);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [id, reloadKey]);

  const load = () => {
    setAppt(null);
    setError(false);
    setReloadKey((k) => k + 1);
  };

  async function updateStatus(status: AppointmentStatus) {
    setUpdating(status);
    try {
      const res = await fetch(`/api/appointments/${id}/status?status=${status}`, {
        method: "PUT",
      });
      if (!res.ok) {
        toast.error("Failed to update status.");
        return;
      }
      const updated = await res.json();
      setAppt(updated);
      toast.success(`Marked as ${status.replace(/_/g, " ").toLowerCase()}`);
    } catch {
      toast.error("Network error — status not updated.");
    } finally {
      setUpdating(null);
    }
  }

  if (loading) return <EcgLoading label="Loading appointment" />;

  if (error || !appt) {
    return (
      <div
        role="alert"
        className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-danger/25 bg-danger-tint px-4 py-3"
      >
        <div className="flex items-center gap-2 text-sm text-danger">
          <AlertTriangle className="size-4 shrink-0" aria-hidden />
          Failed to load this appointment.
        </div>
        <Button variant="secondary" size="sm" onClick={load}>
          Retry
        </Button>
      </div>
    );
  }

  const nextStatuses = NEXT_STATUSES[appt.status] ?? [];

  return (
    <div className="max-w-2xl">
      <PageHeader
        eyebrow="Appointment"
        title={format(new Date(appt.scheduledAt), "MMM d, yyyy")}
        description={`${format(new Date(appt.scheduledAt), "h:mm a")} · ${appt.type
          .replace(/_/g, " ")
          .toLowerCase()}`}
        actions={<StatusBadge status={statusOf(APPOINTMENT_STATUS, appt.status)} />}
      />

      <Card className="mb-6">
        <dl className="grid grid-cols-2 gap-x-8 gap-y-4">
          <div>
            <DetailLabel>Patient ID</DetailLabel>
            <DetailValue mono>{appt.patientId}</DetailValue>
          </div>
          <div>
            <DetailLabel>Doctor ID</DetailLabel>
            <DetailValue mono>{appt.doctorId}</DetailValue>
          </div>
          <div>
            <DetailLabel>Reason</DetailLabel>
            <DetailValue>{appt.reasonForVisit || "—"}</DetailValue>
          </div>
          <div>
            <DetailLabel>Notes</DetailLabel>
            <DetailValue>{appt.notes || "—"}</DetailValue>
          </div>
          <div>
            <DetailLabel>Booked</DetailLabel>
            <DetailValue>{format(new Date(appt.createdAt), "MMM d, yyyy")}</DetailValue>
          </div>
        </dl>
      </Card>

      {nextStatuses.length > 0 && (
        <section className="mb-6">
          <h2 className="font-display mb-3 text-sm font-semibold uppercase tracking-wide text-ink-muted">
            Update status
          </h2>
          <div className="flex flex-wrap gap-2">
            {nextStatuses.map((s) => (
              <Button
                key={s}
                variant={s === "CANCELLED" || s === "NO_SHOW" ? "danger" : "secondary"}
                onClick={() => updateStatus(s)}
                loading={updating === s}
                disabled={updating !== null}
              >
                Mark {statusOf(APPOINTMENT_STATUS, s).label.toLowerCase()}
              </Button>
            ))}
          </div>
        </section>
      )}

      <div className="flex flex-wrap gap-3">
        <Link
          href={`/doctor-dashboard/consultation/${appt.id}`}
          className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-brand-strong"
        >
          <Stethoscope className="size-4" aria-hidden />
          Open consultation
        </Link>
        <Link
          href={`/prescriptions?patientId=${appt.patientId}`}
          className="inline-flex items-center gap-2 rounded-lg border border-line-strong bg-card px-4 py-2 text-sm font-medium text-ink transition-colors hover:border-brand hover:text-brand-ink"
        >
          <ClipboardList className="size-4" aria-hidden />
          View prescriptions
        </Link>
      </div>
    </div>
  );
}
