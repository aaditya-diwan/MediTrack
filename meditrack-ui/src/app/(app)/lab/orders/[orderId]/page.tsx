"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { format } from "date-fns";
import { AlertTriangle, FlaskConical, RefreshCw, Sparkles } from "lucide-react";
import {
  Button,
  Card,
  CardTitle,
  EmptyState,
  LAB_FLAG,
  ListSkeleton,
  PageHeader,
  RESULT_STATUS,
  StatusBadge,
  statusOf,
} from "@/components/ui";
import { LabExplanationView } from "@/components/LabExplanationView";
import type { LabExplanationResponse, LabResultResponse } from "@/lib/types";

export default function LabOrderResultsPage({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  const { orderId } = use(params);
  const [results, setResults] = useState<LabResultResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [explaining, setExplaining] = useState(false);
  const [explainError, setExplainError] = useState<string | null>(null);
  const [explanation, setExplanation] = useState<LabExplanationResponse | null>(
    null,
  );

  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/lab/results/order/${orderId}`)
      .then((r) => {
        if (!r.ok) throw new Error(String(r.status));
        return r.json();
      })
      .then((data) => {
        if (!cancelled) setResults(Array.isArray(data) ? data : []);
      })
      .catch(() => {
        if (!cancelled) setError("Could not load results for this order.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [orderId, reloadKey]);

  const load = () => {
    setLoading(true);
    setError(null);
    setReloadKey((k) => k + 1);
  };

  async function explain() {
    setExplaining(true);
    setExplainError(null);
    setExplanation(null);
    try {
      const res = await fetch("/api/ai/lab-result-explanation", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          results: results.map((r) => ({
            testName: r.testName,
            value: r.resultValue || undefined,
            unit: r.resultUnit || undefined,
            referenceRange: r.referenceRange || undefined,
            flag: r.abnormalFlag || undefined,
          })),
        }),
      });
      const body = await res.json().catch(() => null);
      if (!res.ok) {
        setExplainError(
          res.status === 502 || res.status === 503 || res.status === 504
            ? "The AI service could not complete this request."
            : ((body as { error?: string } | null)?.error ??
                `Request failed (HTTP ${res.status}).`),
        );
        return;
      }
      setExplanation(body as LabExplanationResponse);
    } catch {
      setExplainError("The AI service could not complete this request.");
    } finally {
      setExplaining(false);
    }
  }

  return (
    <div>
      <PageHeader
        eyebrow="Laboratory"
        title="Order results"
        description={`Order ${orderId}`}
        actions={
          results.length > 0 ? (
            <Button variant="secondary" onClick={explain} loading={explaining}>
              <Sparkles className="size-4" aria-hidden />
              Explain with AI
            </Button>
          ) : undefined
        }
      />

      {loading && <ListSkeleton rows={3} />}

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
        <EmptyState
          icon={FlaskConical}
          title="No results yet"
          hint="Results are processed asynchronously — check back shortly."
          action={
            <Button variant="secondary" size="sm" onClick={load}>
              <RefreshCw className="size-3.5" aria-hidden />
              Refresh
            </Button>
          }
        />
      )}

      {!loading && !error && results.length > 0 && (
        <div className="space-y-6">
          <Card className="overflow-x-auto p-0">
            <table className="w-full border-collapse text-sm">
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
                    Status
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Performed
                  </th>
                </tr>
              </thead>
              <tbody>
                {results.map((r) => (
                  <tr
                    key={r.id}
                    className={`border-b border-line last:border-0 ${
                      r.critical ? "bg-danger-tint" : ""
                    }`}
                  >
                    <td className="px-4 py-3">
                      <Link
                        href={`/lab/results/${r.id}`}
                        className="font-medium text-ink hover:text-brand-ink hover:underline"
                      >
                        {r.testName}
                      </Link>
                      <span className="tabular ml-2 text-xs text-ink-faint">
                        {r.testCode}
                      </span>
                    </td>
                    <td className="tabular px-4 py-3 text-ink">
                      {r.resultValue} {r.resultUnit}
                    </td>
                    <td className="tabular px-4 py-3 text-ink-muted">
                      {r.referenceRange}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={statusOf(LAB_FLAG, r.abnormalFlag)} />
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={statusOf(RESULT_STATUS, r.status)} />
                    </td>
                    <td className="px-4 py-3 text-ink-muted">
                      {format(new Date(r.performedAt), "MMM d, yyyy HH:mm")}
                      <span className="block text-xs text-ink-faint">
                        {r.performedBy}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>

          {(explaining || explainError || explanation) && (
            <Card as="section">
              <CardTitle className="mb-4">AI explanation</CardTitle>
              {explaining && (
                <p className="text-sm text-ink-muted">
                  Interpreting {results.length} result
                  {results.length === 1 ? "" : "s"}…
                </p>
              )}
              {explainError && (
                <div
                  role="alert"
                  className="flex items-start justify-between gap-4 rounded-xl border border-danger/25 bg-danger-tint p-4"
                >
                  <div className="flex items-start gap-2">
                    <AlertTriangle
                      className="mt-0.5 size-4 shrink-0 text-danger"
                      aria-hidden
                    />
                    <p className="text-sm text-danger">{explainError}</p>
                  </div>
                  <Button variant="secondary" size="sm" onClick={explain}>
                    <RefreshCw className="size-3.5" aria-hidden />
                    Retry
                  </Button>
                </div>
              )}
              {explanation && <LabExplanationView result={explanation} />}
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
