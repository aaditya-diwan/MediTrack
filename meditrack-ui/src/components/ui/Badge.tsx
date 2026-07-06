import type { Tone, StatusStyle } from "./status";

const TONE_CLASSES: Record<Tone, string> = {
  neutral: "bg-card-2 text-ink-muted border-line",
  brand: "bg-brand-tint text-brand-ink border-brand/20",
  ok: "bg-ok-tint text-ok border-ok/20",
  warn: "bg-warn-tint text-warn border-warn/25",
  danger: "bg-danger-tint text-danger border-danger/25",
  info: "bg-info-tint text-info border-info/20",
};

export function Badge({
  tone = "neutral",
  children,
  className = "",
}: {
  tone?: Tone;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium whitespace-nowrap ${TONE_CLASSES[tone]} ${className}`}
    >
      {children}
    </span>
  );
}

/** Renders a status value through one of the shared maps in status.ts. */
export function StatusBadge({
  status,
  className = "",
}: {
  status: StatusStyle;
  className?: string;
}) {
  return (
    <Badge tone={status.tone} className={className}>
      {status.label}
    </Badge>
  );
}
