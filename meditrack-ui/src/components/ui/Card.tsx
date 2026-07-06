export function Card({
  className = "",
  children,
  as: Tag = "div",
}: {
  className?: string;
  children: React.ReactNode;
  as?: "div" | "section" | "article" | "li";
}) {
  return (
    <Tag
      className={`rounded-xl border border-line bg-card p-5 shadow-[0_1px_2px_rgb(0_0_0/0.04)] ${className}`}
    >
      {children}
    </Tag>
  );
}

export function CardTitle({
  className = "",
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <h2 className={`font-display text-base font-semibold text-ink ${className}`}>
      {children}
    </h2>
  );
}

/** Small uppercase field label used in detail cards. */
export function DetailLabel({ children }: { children: React.ReactNode }) {
  return (
    <dt className="text-[11px] font-medium uppercase tracking-wide text-ink-faint">
      {children}
    </dt>
  );
}

export function DetailValue({
  children,
  mono = false,
}: {
  children: React.ReactNode;
  mono?: boolean;
}) {
  return (
    <dd className={`mt-0.5 text-sm text-ink ${mono ? "tabular" : ""}`}>
      {children ?? "—"}
    </dd>
  );
}
