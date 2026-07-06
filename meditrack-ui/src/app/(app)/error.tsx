"use client";

import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/Button";

export default function AppError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-line-strong bg-card-2/50 px-6 py-16 text-center">
      <AlertTriangle className="mb-3 size-8 text-warn" aria-hidden />
      <p className="text-sm font-medium text-ink">Something went wrong loading this page</p>
      <p className="mt-1 max-w-md text-sm text-ink-muted">
        {error.message || "The service may be temporarily unavailable."}
      </p>
      <Button variant="secondary" className="mt-4" onClick={reset}>
        Try again
      </Button>
    </div>
  );
}
