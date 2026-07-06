"use client";

import { use, useEffect, useState } from "react";
import { format } from "date-fns";
import { toast } from "sonner";
import {
  AlertTriangle,
  Download,
  FlaskConical,
  Pill,
  RefreshCw,
  Send,
  ShieldAlert,
  ShieldCheck,
} from "lucide-react";
import {
  Badge,
  Button,
  Card,
  CardTitle,
  DetailLabel,
  EcgLoading,
  EmptyState,
  Field,
  LAB_PRIORITY,
  PageHeader,
  PRESCRIPTION_STATUS,
  SEVERITY,
  StatusBadge,
  statusOf,
} from "@/components/ui";
import type {
  PrescriptionResponse,
  PrescriptionSafetyInfo,
  SafetyBlockResponse,
} from "@/lib/types";

type IssueResponse = PrescriptionResponse & { safety?: PrescriptionSafetyInfo };

export default function PrescriptionDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [rx, setRx] = useState<PrescriptionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [acting, setActing] = useState(false);

  // Safety-block override flow
  const [safetyBlock, setSafetyBlock] = useState<SafetyBlockResponse | null>(null);
  const [overrideReason, setOverrideReason] = useState("");
  const [safetyInfo, setSafetyInfo] = useState<PrescriptionSafetyInfo | null>(null);

  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/prescriptions/${id}`)
      .then((r) => {
        if (!r.ok) throw new Error(String(r.status));
        return r.json();
      })
      .then((data) => {
        if (!cancelled) setRx(data ?? null);
      })
      .catch(() => {
        if (!cancelled) setLoadError("Could not load this prescription.");
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
    setLoadError(null);
    setReloadKey((k) => k + 1);
  };

  async function issue(override = false) {
    setActing(true);
    try {
      const res = await fetch(`/api/prescriptions/${id}/issue`, {
        method: "POST",
        ...(override
          ? {
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                override: true,
                overrideReason: overrideReason.trim(),
              }),
            }
          : {}),
      });
      const body = await res.json().catch(() => null);
      if (res.status === 409 && body && Array.isArray(body.findings)) {
        setSafetyBlock(body as SafetyBlockResponse);
        toast.warning("Issue blocked by the safety check.");
        return;
      }
      if (!res.ok) {
        toast.error("Failed to issue prescription.");
        return;
      }
      const issued = body as IssueResponse;
      setRx(issued);
      setSafetyInfo(issued.safety ?? null);
      setSafetyBlock(null);
      setOverrideReason("");
      toast.success(
        issued.safety?.overridden
          ? "Prescription issued with safety override."
          : "Prescription issued.",
      );
    } catch {
      toast.error("Network error issuing the prescription.");
    } finally {
      setActing(false);
    }
  }

  async function dispatch(path: "send-pharmacy" | "send-lab", label: string) {
    setActing(true);
    try {
      const res = await fetch(`/api/prescriptions/${id}/${path}`, {
        method: "POST",
      });
      if (!res.ok) {
        toast.error(`Failed to ${label.toLowerCase()}.`);
        return;
      }
      setRx(await res.json());
      toast.success(`${label} done.`);
    } catch {
      toast.error(`Network error — could not ${label.toLowerCase()}.`);
    } finally {
      setActing(false);
    }
  }

  function downloadPdf() {
    const a = document.createElement("a");
    a.href = `/api/prescriptions/${id}/pdf`;
    a.download = `prescription-${id.slice(0, 8)}.pdf`;
    a.click();
  }

  if (loading) return <EcgLoading label="Loading prescription" />;

  if (loadError) {
    return (
      <div
        role="alert"
        className="flex max-w-2xl items-start justify-between gap-4 rounded-xl border border-danger/25 bg-danger-tint p-4"
      >
        <div className="flex items-start gap-2">
          <AlertTriangle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden />
          <p className="text-sm text-danger">{loadError}</p>
        </div>
        <Button variant="secondary" size="sm" onClick={load}>
          <RefreshCw className="size-3.5" aria-hidden />
          Retry
        </Button>
      </div>
    );
  }

  if (!rx) {
    return (
      <EmptyState
        icon={Pill}
        title="Prescription not found"
        hint="This prescription may have been removed, or the link is out of date."
      />
    );
  }

  const canIssue = rx.status === "DRAFT";
  const canDispatch =
    rx.status === "ISSUED" ||
    rx.status === "SENT_TO_PHARMACY" ||
    rx.status === "SENT_TO_LAB";

  return (
    <div className="max-w-3xl">
      <PageHeader
        eyebrow="Pharmacy"
        title="Prescription"
        description={[
          `Created ${format(new Date(rx.createdAt), "MMM d, yyyy")}`,
          rx.issuedAt ? `Issued ${format(new Date(rx.issuedAt), "MMM d, yyyy")}` : null,
          rx.validUntil ? `Valid until ${rx.validUntil}` : null,
        ]
          .filter(Boolean)
          .join(" · ")}
        actions={<StatusBadge status={statusOf(PRESCRIPTION_STATUS, rx.status)} />}
      />

      <div className="space-y-6">
        {safetyBlock && (
          <Card
            as="section"
            className="border-danger/40 bg-danger-tint"
          >
            <div className="flex items-start gap-3">
              <ShieldAlert className="mt-0.5 size-5 shrink-0 text-danger" aria-hidden />
              <div className="min-w-0 flex-1">
                <h2 className="font-display text-base font-semibold text-danger">
                  Issue blocked by safety check
                </h2>
                <div className="mt-1 flex flex-wrap items-center gap-2">
                  <StatusBadge status={statusOf(SEVERITY, safetyBlock.severity)} />
                  <p className="text-sm text-ink">{safetyBlock.summary}</p>
                </div>
                {safetyBlock.findings.length > 0 && (
                  <ul className="mt-3 space-y-2">
                    {safetyBlock.findings.map((f, i) => (
                      <li
                        key={i}
                        className="rounded-lg border border-danger/25 bg-card p-3 text-sm"
                      >
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-medium text-ink">
                            {f.type.replaceAll("_", " ")}
                          </span>
                          <StatusBadge status={statusOf(SEVERITY, f.severity)} />
                        </div>
                        <p className="mt-1 text-ink-muted">{f.description}</p>
                      </li>
                    ))}
                  </ul>
                )}
                {safetyBlock.overrideAllowed ? (
                  <div className="mt-4 space-y-3">
                    <Field
                      label="Override reason"
                      required
                      hint="Document the clinical justification for issuing despite these findings."
                    >
                      {(ids) => (
                        <textarea
                          {...ids}
                          className="input min-h-20 bg-card"
                          value={overrideReason}
                          onChange={(e) => setOverrideReason(e.target.value)}
                        />
                      )}
                    </Field>
                    <div className="flex gap-3">
                      <Button
                        variant="danger"
                        loading={acting}
                        disabled={!overrideReason.trim()}
                        onClick={() => issue(true)}
                      >
                        Override and issue
                      </Button>
                      <Button
                        variant="secondary"
                        disabled={acting}
                        onClick={() => {
                          setSafetyBlock(null);
                          setOverrideReason("");
                        }}
                      >
                        Cancel
                      </Button>
                    </div>
                  </div>
                ) : (
                  <p className="mt-3 text-sm font-medium text-danger">
                    An override is not permitted for these findings.
                  </p>
                )}
              </div>
            </div>
          </Card>
        )}

        {safetyInfo && safetyInfo.checked && (
          <Card as="section">
            <div className="flex items-start gap-3">
              {safetyInfo.overridden ? (
                <ShieldAlert className="mt-0.5 size-5 shrink-0 text-warn" aria-hidden />
              ) : (
                <ShieldCheck className="mt-0.5 size-5 shrink-0 text-ok" aria-hidden />
              )}
              <div className="min-w-0 flex-1">
                <CardTitle>Safety check</CardTitle>
                <div className="mt-1 flex flex-wrap items-center gap-2">
                  <StatusBadge status={statusOf(SEVERITY, safetyInfo.severity)} />
                  {safetyInfo.requiresPharmacistReview && (
                    <Badge tone="warn">Pharmacist review required</Badge>
                  )}
                  {safetyInfo.overridden && (
                    <Badge tone="danger">Overridden</Badge>
                  )}
                </div>
                <p className="mt-2 text-sm text-ink">{safetyInfo.summary}</p>
                {safetyInfo.findings && safetyInfo.findings.length > 0 && (
                  <ul className="mt-3 space-y-2">
                    {safetyInfo.findings.map((f, i) => (
                      <li key={i} className="rounded-lg bg-card-2 p-3 text-sm">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-medium text-ink">
                            {f.type.replaceAll("_", " ")}
                          </span>
                          <StatusBadge status={statusOf(SEVERITY, f.severity)} />
                        </div>
                        <p className="mt-1 text-ink-muted">{f.description}</p>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </Card>
        )}

        {rx.consultationNotes && (
          <Card as="section">
            <DetailLabel>Consultation notes</DetailLabel>
            <p className="mt-2 border-l-2 border-brand/30 pl-3 text-sm text-ink">
              {rx.consultationNotes}
            </p>
          </Card>
        )}

        {rx.diagnosisCodes && (
          <Card as="section">
            <DetailLabel>Diagnosis codes</DetailLabel>
            <p className="tabular mt-2 text-sm text-ink">{rx.diagnosisCodes}</p>
          </Card>
        )}

        <Card as="section" className="p-0">
          <div className="px-5 pt-5">
            <CardTitle>Medications</CardTitle>
          </div>
          {rx.medications && rx.medications.length > 0 ? (
            <div className="mt-3 overflow-x-auto">
              <table className="w-full border-collapse text-sm">
                <thead>
                  <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-faint">
                    <th scope="col" className="px-5 py-2 font-medium">
                      Medication
                    </th>
                    <th scope="col" className="px-4 py-2 font-medium">
                      Dosage
                    </th>
                    <th scope="col" className="px-4 py-2 font-medium">
                      Frequency
                    </th>
                    <th scope="col" className="px-4 py-2 font-medium">
                      Duration
                    </th>
                    <th scope="col" className="px-4 py-2 font-medium">
                      Route
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {rx.medications.map((m) => (
                    <tr key={m.id} className="border-b border-line last:border-0">
                      <td className="px-5 py-3">
                        <span className="font-medium text-ink">
                          {m.medicationName}
                        </span>
                        {m.genericName && (
                          <span className="block text-xs text-ink-faint">
                            {m.genericName}
                          </span>
                        )}
                        {m.instructions && (
                          <span className="block text-xs text-ink-muted">
                            {m.instructions}
                          </span>
                        )}
                      </td>
                      <td className="tabular px-4 py-3 text-ink">{m.dosage}</td>
                      <td className="px-4 py-3 text-ink">{m.frequency}</td>
                      <td className="px-4 py-3 text-ink-muted">
                        {m.duration || "—"}
                      </td>
                      <td className="px-4 py-3 text-ink-muted">{m.route || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="px-5 pb-5 pt-2 text-sm text-ink-faint">
              No medications on this prescription.
            </p>
          )}
        </Card>

        {rx.labOrders && rx.labOrders.length > 0 && (
          <Card as="section">
            <CardTitle className="mb-3">Lab orders</CardTitle>
            <ul className="space-y-2">
              {rx.labOrders.map((l) => (
                <li
                  key={l.id}
                  className="rounded-lg border border-line bg-card-2/40 px-4 py-3 text-sm"
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <FlaskConical className="size-4 text-ink-faint" aria-hidden />
                    <span className="font-medium text-ink">{l.testName}</span>
                    <span className="tabular text-xs text-ink-faint">
                      {l.testCode}
                    </span>
                    <StatusBadge status={statusOf(LAB_PRIORITY, l.urgency)} />
                  </div>
                  {l.clinicalIndication && (
                    <p className="mt-1 text-xs text-ink-muted">
                      {l.clinicalIndication}
                    </p>
                  )}
                </li>
              ))}
            </ul>
          </Card>
        )}

        <div className="flex flex-wrap gap-3">
          {canIssue && !safetyBlock && (
            <Button onClick={() => issue()} loading={acting}>
              Issue prescription
            </Button>
          )}
          {canDispatch && (
            <>
              <Button
                variant="secondary"
                onClick={() => dispatch("send-pharmacy", "Send to pharmacy")}
                disabled={acting}
              >
                <Send className="size-4" aria-hidden />
                Send to pharmacy
              </Button>
              {rx.labOrders && rx.labOrders.length > 0 && (
                <Button
                  variant="secondary"
                  onClick={() => dispatch("send-lab", "Send to lab")}
                  disabled={acting}
                >
                  <FlaskConical className="size-4" aria-hidden />
                  Send to lab
                </Button>
              )}
              <Button variant="secondary" onClick={downloadPdf}>
                <Download className="size-4" aria-hidden />
                Download PDF
              </Button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
