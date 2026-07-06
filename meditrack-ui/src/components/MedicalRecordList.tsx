import { FileText } from "lucide-react";
import { MedicalRecordResponse } from "@/lib/types";
import { Card, EmptyState } from "@/components/ui";
import { format } from "date-fns";

export default function MedicalRecordList({
  records,
}: {
  records: MedicalRecordResponse[];
}) {
  if (!records.length) {
    return (
      <EmptyState
        icon={FileText}
        title="No medical records"
        hint="Records added for this patient will appear here in chronological order."
      />
    );
  }
  return (
    <ul className="space-y-3">
      {records.map((r) => (
        <Card as="li" key={r.recordId} className="p-4">
          <div className="mb-1 flex items-baseline justify-between gap-3">
            <span className="font-medium text-ink">{r.diagnosis}</span>
            <span className="tabular shrink-0 text-xs text-ink-faint">
              {format(new Date(r.date), "MMM d, yyyy")}
            </span>
          </div>
          <p className="text-sm text-ink-muted">{r.treatment}</p>
        </Card>
      ))}
    </ul>
  );
}
