"use client";

import { useState } from "react";
import Link from "next/link";
import PatientCard from "@/components/PatientCard";
import { PatientResponse } from "@/lib/types";

export default function PatientsPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<PatientResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    setSearched(true);
    const res = await fetch(
      `/api/patients/search?query=${encodeURIComponent(query)}`
    );
    setLoading(false);
    if (res.ok) setResults(await res.json());
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Patients</h1>
        <Link
          href="/patients/new"
          className="bg-slate-800 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-700 transition-colors"
        >
          + New Patient
        </Link>
      </div>

      <form onSubmit={handleSearch} className="flex gap-2 mb-6">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by name, MRN, email…"
          className="flex-1 border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
        />
        <button
          type="submit"
          className="bg-slate-700 text-white text-sm px-4 py-2 rounded-lg hover:bg-slate-600 transition-colors"
        >
          Search
        </button>
      </form>

      {loading && <p className="text-slate-500 text-sm">Searching…</p>}
      {!loading && searched && results.length === 0 && (
        <p className="text-slate-500 text-sm">No patients found.</p>
      )}
      <div className="space-y-3">
        {results.map((p) => (
          <PatientCard key={p.id} patient={p} />
        ))}
      </div>
    </div>
  );
}
