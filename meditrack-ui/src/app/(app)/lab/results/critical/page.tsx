"use client";

import { useEffect, useState } from "react";
import { LabResultResponse } from "@/lib/types";
import LabResultBadge from "@/components/LabResultBadge";
import { format } from "date-fns";

export default function CriticalResultsPage() {
  const [results, setResults] = useState<LabResultResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/lab/results/critical?limit=100")
      .then((r) => r.json())
      .then((data) => {
        const sorted = (Array.isArray(data) ? data : []).sort(
          (a: LabResultResponse, b: LabResultResponse) =>
            new Date(b.performedAt).getTime() - new Date(a.performedAt).getTime()
        );
        setResults(sorted);
        setLoading(false);
      });
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-6">Critical Results</h1>
      {loading && <p className="text-slate-500 text-sm">Loading…</p>}
      {!loading && results.length === 0 && (
        <p className="text-slate-500 text-sm">No critical results.</p>
      )}
      <div className="overflow-x-auto">
        {results.length > 0 && (
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="border-b border-slate-200 text-left text-slate-500 text-xs uppercase tracking-wide">
                <th className="pb-2 pr-4">Test</th>
                <th className="pb-2 pr-4">Value</th>
                <th className="pb-2 pr-4">Flag</th>
                <th className="pb-2 pr-4">Patient Order</th>
                <th className="pb-2 pr-4">Performed At</th>
                <th className="pb-2">Status</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r) => (
                <tr
                  key={r.id}
                  className={`border-b border-slate-100 ${
                    r.critical ? "bg-red-50" : ""
                  }`}
                >
                  <td className="py-2 pr-4 font-medium text-slate-800">
                    {r.testName}
                    <span className="text-slate-400 ml-1 font-normal">
                      ({r.testCode})
                    </span>
                  </td>
                  <td className="py-2 pr-4 text-slate-700">
                    {r.resultValue} {r.resultUnit}
                  </td>
                  <td className="py-2 pr-4">
                    <LabResultBadge flag={r.abnormalFlag} />
                  </td>
                  <td className="py-2 pr-4 text-slate-500 font-mono text-xs">
                    {r.orderId.slice(0, 8)}…
                  </td>
                  <td className="py-2 pr-4 text-slate-500">
                    {format(new Date(r.performedAt), "MMM d, HH:mm")}
                  </td>
                  <td className="py-2 text-slate-500">{r.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
