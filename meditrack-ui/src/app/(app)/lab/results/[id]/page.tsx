"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { format } from "date-fns";
import { AlertTriangle, FlaskConical, RefreshCw } from "lucide-react";
import {
  Badge,
  Button,
  Card,
  DetailLabel,
  DetailValue,
  EcgLoading,
  EmptyState,
  LAB_FLAG,
  PageHeader,
  RESULT_STATUS,
  StatusBadge,
  statusOf,
} from "@/components/ui";
import type { LabResultResponse } from "@/lib/types";

export default function LabResultDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [result, setResult] = useState<LabResultResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/lab/results/${id}`)
      .then((r) => {
        if (!r.ok) throw new Error(String(r.status));
        return r.json();
      })
      .then((data) => {
        if (!cancelled) setResult(data ?? null);
      })
      .catch(() => {
        if (!cancelled) setError("Could not load this lab result.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id, reloadKey]);

  const load = () => {
    setLoading(true);
    setError(null);
    setReloadKey((k) => k + 1);
  };

  if (loading) return <EcgLoading label="Loading result" />;

  if (error) {
    return (
      <div
        role="alert"
        className="flex max-w-2xl items-start justify-between gap-4 rounded-xl border border-danger/25 bg-danger-tint p-4"
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
    );
  }

  if (!result) {
    return (
      <EmptyState
        icon={FlaskConical}
        title="Result not found"
        hint="This lab result may have been removed, or the link is out of date."
      />
    );
  }

  return (
    <div className="max-w-2xl">
      <PageHeader
        eyebrow="Laboratory"
        title={result.testName}
        description={`Result recorded ${format(new Date(result.performedAt), "PPpp")}`}
        actions={
          <div className="flex items-center gap-2">
            {result.critical && (
              <Badge tone="danger">
                <AlertTriangle className="size-3" aria-hidden />
                Critical
              </Badge>
            )}
            <StatusBadge status={statusOf(LAB_FLAG, result.abnormalFlag)} />
            <StatusBadge status={statusOf(RESULT_STATUS, result.status)} />
          </div>
        }
      />

      <Card as="section">
        <dl className="grid grid-cols-2 gap-x-8 gap-y-4">
          <div>
            <DetailLabel>Value</DetailLabel>
            <DetailValue mono>
              {result.resultValue} {result.resultUnit}
            </DetailValue>
          </div>
          <div>
            <DetailLabel>Reference range</DetailLabel>
            <DetailValue mono>{result.referenceRange}</DetailValue>
          </div>
          <div>
            <DetailLabel>Test code</DetailLabel>
            <DetailValue mono>{result.testCode}</DetailValue>
          </div>
          <div>
            <DetailLabel>LOINC code</DetailLabel>
            <DetailValue mono>{result.loincCode}</DetailValue>
          </div>
          <div>
            <DetailLabel>Performed by</DetailLabel>
            <DetailValue>{result.performedBy}</DetailValue>
          </div>
          <div>
            <DetailLabel>Performed at</DetailLabel>
            <DetailValue>
              {format(new Date(result.performedAt), "PPpp")}
            </DetailValue>
          </div>
          <div>
            <DetailLabel>Verified by</DetailLabel>
            <DetailValue>{result.verifiedBy ?? "Not verified"}</DetailValue>
          </div>
          <div>
            <DetailLabel>Verified at</DetailLabel>
            <DetailValue>
              {result.verifiedAt
                ? format(new Date(result.verifiedAt), "PPpp")
                : "—"}
            </DetailValue>
          </div>
          <div>
            <DetailLabel>Order</DetailLabel>
            <DetailValue mono>
              <Link
                href={`/lab/orders/${result.orderId}`}
                className="text-brand-ink hover:underline"
              >
                {result.orderId}
              </Link>
            </DetailValue>
          </div>
        </dl>
      </Card>

      {result.notes && (
        <Card as="section" className="mt-4">
          <DetailLabel>Notes</DetailLabel>
          <p className="mt-2 text-sm text-ink">{result.notes}</p>
        </Card>
      )}
    </div>
  );
}
