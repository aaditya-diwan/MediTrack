"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Activity,
  CalendarPlus,
  FlaskConical,
  Sparkles,
  Stethoscope,
  UserPlus,
  type LucideIcon,
} from "lucide-react";
import { PatientPicker } from "@/components/PatientPicker";
import {
  Card,
  CardTitle,
  EmptyState,
  PageHeader,
  Skeleton,
  StatCard,
  StatusBadge,
  LAB_FLAG,
  statusOf,
} from "@/components/ui";
import type { DoctorResponse, LabResultResponse } from "@/lib/types";

function QuickAction({
  href,
  icon: Icon,
  title,
  hint,
}: {
  href: string;
  icon: LucideIcon;
  title: string;
  hint: string;
}) {
  return (
    <Link
      href={href}
      className="group flex items-start gap-3 rounded-xl border border-line bg-card p-4 transition-colors hover:border-brand"
    >
      <div className="rounded-lg bg-brand-tint p-2 text-brand-ink">
        <Icon className="size-4" aria-hidden />
      </div>
      <div>
        <p className="text-sm font-medium text-ink group-hover:text-brand-ink">
          {title}
        </p>
        <p className="mt-0.5 text-xs text-ink-muted">{hint}</p>
      </div>
    </Link>
  );
}

export default function DashboardPage() {
  const router = useRouter();
  const [doctors, setDoctors] = useState<DoctorResponse[] | null>(null);
  const [critical, setCritical] = useState<LabResultResponse[] | null>(null);

  useEffect(() => {
    fetch("/api/doctors")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: DoctorResponse[]) => setDoctors(Array.isArray(d) ? d : []))
      .catch(() => setDoctors([]));
    fetch("/api/lab/results/critical?limit=5")
      .then((r) => (r.ok ? r.json() : []))
      .then((d: LabResultResponse[]) => setCritical(Array.isArray(d) ? d : []))
      .catch(() => setCritical([]));
  }, []);

  return (
    <div>
      <PageHeader
        eyebrow="Overview"
        title="Hospital overview"
        description="Today's clinical picture at a glance."
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div className="sm:col-span-2 lg:col-span-1">
          <Card>
            <CardTitle className="mb-3">Find a patient</CardTitle>
            <PatientPicker
              selected={null}
              label="Search"
              onSelect={(p) => p && router.push(`/patients/${p.id}`)}
            />
          </Card>
        </div>
        <StatCard
          icon={Stethoscope}
          label="Doctors on staff"
          value={doctors === null ? "…" : doctors.filter((d) => d.active).length}
          hint={doctors === null ? undefined : `${doctors.length} registered`}
        />
        <StatCard
          icon={Activity}
          label="Critical results"
          value={critical === null ? "…" : critical.length}
          hint="Unacknowledged critical lab values"
          tone={critical && critical.length > 0 ? "danger" : "default"}
        />
      </div>

      <section className="mt-8">
        <h2 className="mb-3 font-display text-sm font-semibold uppercase tracking-wide text-ink-muted">
          Quick actions
        </h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <QuickAction
            href="/appointments/book"
            icon={CalendarPlus}
            title="Book appointment"
            hint="Specialty → doctor → slot"
          />
          <QuickAction
            href="/patients/new"
            icon={UserPlus}
            title="Register patient"
            hint="New patient intake"
          />
          <QuickAction
            href="/lab/orders"
            icon={FlaskConical}
            title="New lab order"
            hint="Order tests for a patient"
          />
          <QuickAction
            href="/ai"
            icon={Sparkles}
            title="AI console"
            hint="Safety checks, triage, notes"
          />
        </div>
      </section>

      <section className="mt-8">
        <div className="mb-3 flex items-baseline justify-between">
          <h2 className="font-display text-sm font-semibold uppercase tracking-wide text-ink-muted">
            Critical lab results
          </h2>
          <Link
            href="/lab/results/critical"
            className="text-xs font-medium text-brand-ink hover:underline"
          >
            View all
          </Link>
        </div>
        {critical === null ? (
          <div className="space-y-2">
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        ) : critical.length === 0 ? (
          <EmptyState
            icon={Activity}
            title="No critical results right now"
            hint="Critical lab values will surface here the moment they are reported."
          />
        ) : (
          <ul className="divide-y divide-line overflow-hidden rounded-xl border border-line bg-card">
            {critical.map((r) => (
              <li key={r.id}>
                <Link
                  href={`/lab/results/${r.id}`}
                  className="flex items-center justify-between gap-4 px-4 py-3 transition-colors hover:bg-card-2"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-ink">
                      {r.testName}
                    </p>
                    <p className="tabular mt-0.5 text-xs text-ink-muted">
                      {r.resultValue} {r.resultUnit} · ref {r.referenceRange}
                    </p>
                  </div>
                  <StatusBadge status={statusOf(LAB_FLAG, r.abnormalFlag)} />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
