"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import Link from "next/link";
import InsurancePolicyCard from "@/components/InsurancePolicyCard";
import { PolicyResponse } from "@/lib/types";

export default function InsurancePage({
  searchParams,
}: {
  searchParams: Promise<{ patientId?: string }>;
}) {
  const { patientId } = use(searchParams);
  const [policies, setPolicies] = useState<PolicyResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [query, setQuery] = useState(patientId ?? "");

  useEffect(() => {
    if (patientId) {
      loadPolicies(patientId);
    }
  }, [patientId]);

  async function loadPolicies(id: string) {
    setLoading(true);
    setSearched(true);
    const res = await fetch(`/api/insurance/policies/patient/${id}`);
    setLoading(false);
    if (res.ok) setPolicies(await res.json());
    else setPolicies([]);
  }

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    loadPolicies(query.trim());
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Insurance Policies</h1>
        <Link
          href="/insurance/new"
          className="bg-slate-800 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-700 transition-colors"
        >
          + New Policy
        </Link>
      </div>
      <form onSubmit={handleSearch} className="flex gap-2 mb-6">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Patient ID (UUID)"
          className="flex-1 border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
        />
        <button
          type="submit"
          className="bg-slate-700 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-600 transition-colors"
        >
          Search
        </button>
      </form>
      {loading && <p className="text-slate-500 text-sm">Loading…</p>}
      {!loading && searched && policies.length === 0 && (
        <p className="text-slate-500 text-sm">No policies found for this patient.</p>
      )}
      <div className="space-y-4">
        {policies.map((p) => (
          <InsurancePolicyCard key={p.policyId} policy={p} />
        ))}
      </div>
    </div>
  );
}
