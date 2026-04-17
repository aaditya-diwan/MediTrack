import { MedicalRecordResponse } from "@/lib/types";
import { format } from "date-fns";

export default function MedicalRecordList({
  records,
}: {
  records: MedicalRecordResponse[];
}) {
  if (!records.length) {
    return <p className="text-slate-500 text-sm">No medical records found.</p>;
  }
  return (
    <ul className="space-y-3">
      {records.map((r) => (
        <li
          key={r.recordId}
          className="border border-slate-200 rounded-lg p-4 text-sm"
        >
          <div className="flex justify-between items-center mb-1">
            <span className="font-medium text-slate-800">{r.diagnosis}</span>
            <span className="text-xs text-slate-400">
              {format(new Date(r.date), "MMM d, yyyy")}
            </span>
          </div>
          <p className="text-slate-600">{r.treatment}</p>
        </li>
      ))}
    </ul>
  );
}
