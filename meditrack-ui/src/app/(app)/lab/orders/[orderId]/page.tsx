"use client";

import { use, useEffect, useState } from "react";
import { LabResultResponse } from "@/lib/types";
import LabResultBadge from "@/components/LabResultBadge";
import { format } from "date-fns";

export default function LabOrderResultsPage({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  const { orderId } = use(params);
  const [results, setResults] = useState<LabResultResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`/api/lab/results/order/${orderId}`)
      .then((r) => r.json())
      .then((data) => {
        setResults(Array.isArray(data) ? data : []);
        setLoading(false);
      });
  }, [orderId]);

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Order Results</h1>
      <p className="text-slate-500 text-sm mb-6">Order ID: {orderId}</p>

      {loading && <p className="text-slate-500 text-sm">Loading…</p>}
      {!loading && results.length === 0 && (
        <p className="text-slate-500 text-sm">
          No results yet. Results are processed asynchronously — check back shortly.
        </p>
      )}
      <div className="space-y-4">
        {results.map((r) => (
          <div
            key={r.id}
            className={`border rounded-lg p-4 text-sm ${
              r.critical ? "border-red-300 bg-red-50" : "border-slate-200"
            }`}
          >
            <div className="flex items-center justify-between mb-2">
              <div>
                <span className="font-semibold text-slate-800">{r.testName}</span>
                <span className="text-slate-400 ml-2">({r.testCode})</span>
              </div>
              <LabResultBadge flag={r.abnormalFlag} />
            </div>
            <div className="grid grid-cols-2 gap-x-8 gap-y-1 text-slate-600">
              <span className="text-slate-400">Value</span>
              <span>
                {r.resultValue} {r.resultUnit}
              </span>
              <span className="text-slate-400">Reference Range</span>
              <span>{r.referenceRange}</span>
              <span className="text-slate-400">Status</span>
              <span>{r.status}</span>
              <span className="text-slate-400">Performed By</span>
              <span>{r.performedBy}</span>
              <span className="text-slate-400">Performed At</span>
              <span>{format(new Date(r.performedAt), "MMM d, yyyy HH:mm")}</span>
              {r.notes && (
                <>
                  <span className="text-slate-400">Notes</span>
                  <span>{r.notes}</span>
                </>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
