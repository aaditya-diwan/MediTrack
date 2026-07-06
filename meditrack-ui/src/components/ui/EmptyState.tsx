import type { LucideIcon } from "lucide-react";

export function EmptyState({
  icon: Icon,
  title,
  hint,
  action,
}: {
  icon?: LucideIcon;
  title: string;
  hint?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-line-strong bg-card-2/50 px-6 py-12 text-center">
      {Icon && <Icon className="mb-3 size-8 text-ink-faint" aria-hidden />}
      <p className="text-sm font-medium text-ink">{title}</p>
      {hint && <p className="mt-1 max-w-sm text-sm text-ink-muted">{hint}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
