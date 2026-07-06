import Link from "next/link";
import { Mail, Phone } from "lucide-react";
import { PatientResponse } from "@/lib/types";
import { format } from "date-fns";

export default function PatientCard({ patient }: { patient: PatientResponse }) {
  return (
    <Link
      href={`/patients/${patient.id}`}
      className="block rounded-xl border border-line bg-card p-4 shadow-[0_1px_2px_rgb(0_0_0/0.04)] transition-colors hover:border-brand"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate font-display text-sm font-semibold text-ink">
            {patient.firstName} {patient.lastName}
          </p>
          <p className="tabular mt-0.5 text-xs text-ink-muted">
            MRN {patient.mrn}
          </p>
        </div>
        <span className="tabular shrink-0 text-xs text-ink-faint">
          DOB {format(new Date(patient.dateOfBirth), "MMM d, yyyy")}
        </span>
      </div>
      <div className="mt-3 space-y-1 text-sm text-ink-muted">
        <p className="flex items-center gap-2">
          <Mail className="size-3.5 shrink-0 text-ink-faint" aria-hidden />
          <span className="truncate">{patient.email}</span>
        </p>
        <p className="flex items-center gap-2">
          <Phone className="size-3.5 shrink-0 text-ink-faint" aria-hidden />
          <span className="tabular">{patient.phoneNumber}</span>
        </p>
      </div>
    </Link>
  );
}
