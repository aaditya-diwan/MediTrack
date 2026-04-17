"use client";

import { use, useEffect, useState } from "react";
import { LabResultResponse } from "@/lib/types";
import LabResultBadge from "@/components/LabResultBadge";
import { format } from "date-fns";

export default function LabResultDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [result, setResult] = useState<LabResultResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`/api/lab/results/${id}`)
      .then((r) => r.json())
      .then((data) => {
        setResult(data);
        setLoading(false);
      });
  }, [id]);

  if (loading) return <p className="text-slate-500">Loading…</p>;
  if (!result) return <p className="text-red-500">Result not found.</p>;

  return (
    <div className="max-w-2xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-800">{result.testName}</h1>
        <LabResultBadge flag={result.abnormalFlag} />
      </div>
      <dl className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
        {[
          ["Test Code", result.testCode],
          ["LOINC Code", result.loincCode],
          ["Value", `${result.resultValue} ${result.resultUnit}`],
          ["Reference Range", result.referenceRange],
          ["Status", result.status],
          ["Performed By", result.performedBy],
          ["Performed At", format(new Date(result.performedAt), "PPpp")],
          ...(result.verifiedBy ? [["Verified By", result.verifiedBy]] : []),
          ...(result.verifiedAt
            ? [["Verified At", format(new Date(result.verifiedAt), "PPpp")]]
            : []),
          ["Order ID", result.orderId],
          ["Critical", result.critical ? "Yes" : "No"],
        ].map(([label, value]) => (
          <div key={label}>
            <dt className="text-slate-400">{label}</dt>
            <dd className="text-slate-800 font-medium">{value}</dd>
          </div>
        ))}
      </dl>
      {result.notes && (
        <div className="mt-4 p-4 bg-slate-50 rounded-lg text-sm text-slate-700">
          <p className="text-slate-400 text-xs uppercase tracking-wide mb-1">Notes</p>
          {result.notes}
        </div>
      )}
    </div>
  );
}
