"use client";

import { useId } from "react";

/**
 * Accessible form field: label is always associated via htmlFor/id, errors
 * are announced via aria-describedby. Works with react-hook-form by spreading
 * `register(...)` into the control props.
 */
export function Field({
  label,
  error,
  hint,
  required,
  children,
  className = "",
}: {
  label: string;
  error?: string;
  hint?: string;
  required?: boolean;
  className?: string;
  children: (ids: {
    id: string;
    "aria-invalid": boolean | undefined;
    "aria-describedby": string | undefined;
  }) => React.ReactNode;
}) {
  const id = useId();
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;
  return (
    <div className={className}>
      <label
        htmlFor={id}
        className="mb-1 block text-xs font-medium text-ink-muted"
      >
        {label}
        {required && <span className="ml-0.5 text-danger">*</span>}
      </label>
      {children({
        id,
        "aria-invalid": error ? true : undefined,
        "aria-describedby": describedBy,
      })}
      {hint && !error && (
        <p id={`${id}-hint`} className="mt-1 text-xs text-ink-faint">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  );
}
