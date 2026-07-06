/**
 * The ECG trace — MediTrack's signature mark. Used in exactly three places:
 * the sidebar brand, the page-header rule, and loading states. Keep it out
 * of everything else so it stays meaningful.
 */
export function EcgTrace({
  className = "",
  animate = false,
}: {
  className?: string;
  animate?: boolean;
}) {
  return (
    <svg
      viewBox="0 0 120 24"
      fill="none"
      aria-hidden
      className={className}
      preserveAspectRatio="none"
    >
      <path
        d="M0 12 H38 L44 12 48 4 54 20 58 8 61 12 H120"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinejoin="round"
        strokeLinecap="round"
        className={animate ? "ecg-animate" : undefined}
      />
    </svg>
  );
}

/** Full-area loading state with the animated trace. */
export function EcgLoading({ label = "Loading" }: { label?: string }) {
  return (
    <div
      className="flex flex-col items-center justify-center gap-3 py-16 text-ink-faint"
      role="status"
    >
      <EcgTrace animate className="h-6 w-40 text-brand" />
      <span className="text-xs uppercase tracking-widest">{label}</span>
    </div>
  );
}
