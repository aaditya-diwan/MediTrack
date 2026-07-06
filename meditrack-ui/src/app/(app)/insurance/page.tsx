"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { FileSearch, Plus, ShieldOff } from "lucide-react";
import InsurancePolicyCard from "@/components/InsurancePolicyCard";
import { PatientPicker } from "@/components/PatientPicker";
import { PolicyResponse, PatientResponse } from "@/lib/types";
import { Button, EmptyState, ListSkeleton, PageHeader } from "@/components/ui";

function NewPolicyLink() {
  return (
    <Link
      href="/insurance/new"
      className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-brand-strong"
    >
      <Plus className="size-4" aria-hidden />
      New policy
    </Link>
  );
}

export default function InsurancePage({
  searchParams,
}: {
  searchParams: Promise<{ patientId?: string }>;
}) {
  const { patientId } = use(searchParams);
  const [selected, setSelected] = useState<PatientResponse | null>(null);
  const [policies, setPolicies] = useState<PolicyResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastId, setLastId] = useState<string | null>(null);

  useEffect(() => {
    if (patientId) {
      loadPolicies(patientId);
    }
  }, [patientId]);

  async function loadPolicies(id: string) {
    setLoading(true);
    setSearched(true);
    setError(null);
    setLastId(id);
    try {
      const res = await fetch(`/api/insurance/policies/patient/${id}`);
      if (!res.ok) {
        setPolicies([]);
        return;
      }
      const data = await res.json();
      setPolicies(Array.isArray(data) ? data : []);
    } catch {
      setPolicies([]);
      setError("Could not load policies. Check your connection and try again.");
    } finally {
      setLoading(false);
    }
  }

  function handleSelect(patient: PatientResponse | null) {
    setSelected(patient);
    if (patient) {
      loadPolicies(patient.id);
    } else {
      setPolicies([]);
      setSearched(false);
      setError(null);
    }
  }

  return (
    <div>
      <PageHeader
        eyebrow="Insurance"
        title="Insurance policies"
        description="Look up a patient to review their coverage and policy details."
        actions={<NewPolicyLink />}
      />

      <div className="mb-6 max-w-md">
        <PatientPicker selected={selected} onSelect={handleSelect} />
      </div>

      {loading && <ListSkeleton rows={3} />}

      {!loading && error && (
        <div className="rounded-xl border border-danger/25 bg-danger-tint p-4">
          <p className="text-sm font-medium text-danger">{error}</p>
          {lastId && (
            <Button
              variant="secondary"
              size="sm"
              className="mt-3"
              onClick={() => loadPolicies(lastId)}
            >
              Retry
            </Button>
          )}
        </div>
      )}

      {!loading && !error && !searched && (
        <EmptyState
          icon={FileSearch}
          title="Select a patient"
          hint="Search for a patient above to view their insurance policies."
        />
      )}

      {!loading && !error && searched && policies.length === 0 && (
        <EmptyState
          icon={ShieldOff}
          title="No policies on file"
          hint="This patient has no insurance policies yet. You can add one now."
          action={<NewPolicyLink />}
        />
      )}

      {!loading && !error && policies.length > 0 && (
        <div className="space-y-4">
          {policies.map((p) => (
            <InsurancePolicyCard key={p.policyId} policy={p} />
          ))}
        </div>
      )}
    </div>
  );
}
