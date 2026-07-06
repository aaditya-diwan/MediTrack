"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { format } from "date-fns";
import { AlertTriangle, RefreshCw, ShieldCheck } from "lucide-react";
import {
  Button,
  Card,
  LAB_FLAG,
  ListSkeleton,
  PageHeader,
  RESULT_STATUS,
  StatusBadge,
  statusOf,
} from "@/components/ui";
import type { LabResultResponse } from "@/lib/types";

export default function CriticalResultsPage() {
  const [results, setResults] = useState<LabResultResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetch("/api/lab/results/critical?limit=100")
      .then((r) => {
        if (!r.ok) throw new Error(String(r.status));
        return r.json();
      })
      .then((data) => {
        if (cancelled) return;
        const sorted = (Array.isArray(data) ? (data as LabResultResponse[]) : []).sort(
          (a, b) =>
            new Date(b.performedAt).getTime() - new Date(a.performedAt).getTime(),
        );
        setResults(sorted);
      })
      .catch(() => {
        if (!cancelled) setError("Could not load critical results.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const load = () => {
    setLoading(true);
    setError(null);
    setReloadKey((k) => k + 1);
  };

  return (
    <div>
      <PageHeader
        eyebrow="Laboratory"
        title="Critical results"
        description="Results flagged critically abnormal — review and act on these first."
      />

      {loading && <ListSkeleton rows={5} />}

      {!loading && error && (
        <div
          role="alert"
          className="flex items-start justify-between gap-4 rounded-xl border border-danger/25 bg-danger-tint p-4"
        >
          <div className="flex items-start gap-2">
            <AlertTriangle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden />
            <p className="text-sm text-danger">{error}</p>
          </div>
          <Button variant="secondary" size="sm" onClick={load}>
            <RefreshCw className="size-3.5" aria-hidden />
            Retry
          </Button>
        </div>
      )}

      {!loading && !error && results.length === 0 && (
        <EmptyStateBlock onRefresh={load} />
      )}

      {!loading && !error && results.length > 0 && (
        <Card className="overflow-x-auto p-0">
          <table className="w-full border-collapse text-sm">
            <caption className="sr-only">
              Critical lab results, most recent first
            </caption>
            <thead>
              <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-faint">
                <th scope="col" className="px-4 py-3 font-medium">
                  Test
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Value
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Reference range
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Flag
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Order
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Performed
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Status
                </th>
              </tr>
            </thead>
            <tbody>
              {results.map((r) => (
                <tr
                  key={r.id}
                  className="border-b border-line bg-danger-tint/40 last:border-0"
                >
                  <th scope="row" className="px-4 py-3 text-left font-medium">
                    <Link
                      href={`/lab/results/${r.id}`}
                      className="text-ink hover:text-brand-ink hover:underline"
                    >
                      {r.testName}
                    </Link>
                    <span className="tabular ml-2 text-xs font-normal text-ink-faint">
                      {r.testCode}
                    </span>
                  </th>
                  <td className="tabular px-4 py-3 font-semibold text-danger">
                    {r.resultValue} {r.resultUnit}
                  </td>
                  <td className="tabular px-4 py-3 text-ink-muted">
                    {r.referenceRange}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={statusOf(LAB_FLAG, r.abnormalFlag)} />
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      href={`/lab/orders/${r.orderId}`}
                      className="tabular text-xs text-brand-ink hover:underline"
                    >
                      {r.orderId.slice(0, 8)}…
                    </Link>
                  </td>
                  <td className="tabular px-4 py-3 text-ink-muted">
                    {format(new Date(r.performedAt), "MMM d, HH:mm")}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={statusOf(RESULT_STATUS, r.status)} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}

function EmptyStateBlock({ onRefresh }: { onRefresh: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-line-strong bg-card-2/50 px-6 py-12 text-center">
      <ShieldCheck className="mb-3 size-8 text-ok" aria-hidden />
      <p className="text-sm font-medium text-ink">No critical results</p>
      <p className="mt-1 max-w-sm text-sm text-ink-muted">
        No results are currently flagged critical. New critical values appear
        here as soon as the lab reports them.
      </p>
      <Button variant="secondary" size="sm" className="mt-4" onClick={onRefresh}>
        <RefreshCw className="size-3.5" aria-hidden />
        Refresh
      </Button>
    </div>
  );
}
