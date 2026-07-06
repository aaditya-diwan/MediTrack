"use client";

import { useState } from "react";
import Link from "next/link";
import { Plus, Search, UserSearch, Users } from "lucide-react";
import PatientCard from "@/components/PatientCard";
import { PatientResponse } from "@/lib/types";
import { Button, EmptyState, ListSkeleton, PageHeader } from "@/components/ui";

function NewPatientLink() {
  return (
    <Link
      href="/patients/new"
      className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-brand-strong"
    >
      <Plus className="size-4" aria-hidden />
      New patient
    </Link>
  );
}

export default function PatientsPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<PatientResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastQuery, setLastQuery] = useState("");

  async function search(q: string) {
    if (!q.trim()) return;
    setLoading(true);
    setSearched(true);
    setError(null);
    setLastQuery(q);
    try {
      const res = await fetch(
        `/api/patients/search?query=${encodeURIComponent(q)}`,
      );
      if (!res.ok) throw new Error(`Search failed (${res.status})`);
      const data = await res.json();
      setResults(Array.isArray(data) ? data : []);
    } catch {
      setResults([]);
      setError("Could not load patients. Check your connection and try again.");
    } finally {
      setLoading(false);
    }
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    search(query);
  }

  return (
    <div>
      <PageHeader
        eyebrow="Patients"
        title="Patient directory"
        description="Find a patient to open their chart, or register a new one."
        actions={<NewPatientLink />}
      />

      <form onSubmit={handleSearch} className="mb-6 flex gap-2" role="search">
        <div className="relative flex-1">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-faint"
            aria-hidden
          />
          <label htmlFor="patient-search" className="sr-only">
            Search patients
          </label>
          <input
            id="patient-search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name, MRN, email…"
            className="input pl-9"
          />
        </div>
        <Button type="submit" variant="secondary">
          Search
        </Button>
      </form>

      {loading && <ListSkeleton rows={4} />}

      {!loading && error && (
        <div className="rounded-xl border border-danger/25 bg-danger-tint p-4">
          <p className="text-sm font-medium text-danger">{error}</p>
          <Button
            variant="secondary"
            size="sm"
            className="mt-3"
            onClick={() => search(lastQuery)}
          >
            Retry
          </Button>
        </div>
      )}

      {!loading && !error && !searched && (
        <EmptyState
          icon={UserSearch}
          title="Search the patient directory"
          hint="Look up patients by name, MRN, or email to open their chart."
        />
      )}

      {!loading && !error && searched && results.length === 0 && (
        <EmptyState
          icon={Users}
          title="No patients found"
          hint={`Nothing matched “${lastQuery.trim()}”. Try a different spelling, or register the patient.`}
          action={<NewPatientLink />}
        />
      )}

      {!loading && !error && results.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2">
          {results.map((p) => (
            <PatientCard key={p.id} patient={p} />
          ))}
        </div>
      )}
    </div>
  );
}
