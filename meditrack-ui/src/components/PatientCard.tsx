import Link from "next/link";
import { PatientResponse } from "@/lib/types";
import { format } from "date-fns";

export default function PatientCard({ patient }: { patient: PatientResponse }) {
  return (
    <Link
      href={`/patients/${patient.id}`}
      className="block border border-slate-200 rounded-lg p-4 hover:border-slate-400 hover:shadow-sm transition-all"
    >
      <div className="flex justify-between items-start">
        <div>
          <p className="font-semibold text-slate-800">
            {patient.firstName} {patient.lastName}
          </p>
          <p className="text-sm text-slate-500 mt-0.5">MRN: {patient.mrn}</p>
        </div>
        <span className="text-xs text-slate-400">
          DOB: {format(new Date(patient.dateOfBirth), "MMM d, yyyy")}
        </span>
      </div>
      <div className="mt-2 text-sm text-slate-600 space-y-0.5">
        <p>{patient.email}</p>
        <p>{patient.phoneNumber}</p>
      </div>
    </Link>
  );
}
