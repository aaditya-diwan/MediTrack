import type { LucideIcon } from "lucide-react";
import { Card } from "./Card";

export function StatCard({
  icon: Icon,
  label,
  value,
  hint,
  tone = "default",
}: {
  icon: LucideIcon;
  label: string;
  value: React.ReactNode;
  hint?: string;
  tone?: "default" | "danger";
}) {
  return (
    <Card className="flex items-start gap-4">
      <div
        className={`rounded-lg p-2 ${
          tone === "danger"
            ? "bg-danger-tint text-danger"
            : "bg-brand-tint text-brand-ink"
        }`}
      >
        <Icon className="size-5" aria-hidden />
      </div>
      <div className="min-w-0">
        <p className="text-xs font-medium uppercase tracking-wide text-ink-faint">
          {label}
        </p>
        <p className="mt-0.5 truncate font-display text-2xl font-semibold text-ink">
          {value}
        </p>
        {hint && <p className="mt-0.5 text-xs text-ink-muted">{hint}</p>}
      </div>
    </Card>
  );
}
