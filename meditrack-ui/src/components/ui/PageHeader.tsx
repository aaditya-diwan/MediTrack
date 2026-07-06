import { EcgTrace } from "./Ecg";

/**
 * Standard page header: optional eyebrow (section context), display-face
 * title, supporting line, right-aligned actions, and the ECG hairline rule.
 */
export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: React.ReactNode;
}) {
  return (
    <header className="mb-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          {eyebrow && (
            <p className="mb-1 text-[11px] font-medium uppercase tracking-[0.14em] text-brand-ink">
              {eyebrow}
            </p>
          )}
          <h1 className="font-display text-2xl font-semibold tracking-tight text-ink">
            {title}
          </h1>
          {description && (
            <p className="mt-1 max-w-2xl text-sm text-ink-muted">{description}</p>
          )}
        </div>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>
      <div className="mt-4 flex items-center text-line-strong">
        <div className="h-px flex-1 bg-line" />
        <EcgTrace className="h-4 w-24 shrink-0" />
        <div className="h-px w-8 bg-line" />
      </div>
    </header>
  );
}
