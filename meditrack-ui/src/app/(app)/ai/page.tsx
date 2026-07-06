"use client";

import { useState } from "react";
import { toast } from "sonner";
import {
  AlertTriangle,
  Copy,
  FileText,
  FlaskConical,
  History,
  ListChecks,
  Plus,
  RefreshCw,
  ShieldCheck,
  Siren,
  Sparkles,
  Stethoscope,
  X,
} from "lucide-react";
import {
  Badge,
  Button,
  Card,
  CardTitle,
  EcgLoading,
  EmptyState,
  Field,
  PageHeader,
  SEVERITY,
  StatusBadge,
  URGENCY,
  statusOf,
} from "@/components/ui";
import { LabExplanationView } from "@/components/LabExplanationView";
import type {
  HistorySummaryResponse,
  IcdCodesResponse,
  LabExplanationResponse,
  PrescriptionSafetyResponse,
  SoapNoteResponse,
  SymptomTriageResponse,
} from "@/lib/types";

// --- shared helpers -------------------------------------------------------

function toList(csv: string): string[] {
  return csv
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

class AiRequestError extends Error {
  constructor(message: string, readonly serviceDown: boolean) {
    super(message);
  }
}

async function postAi<T>(path: string, body: unknown): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  } catch {
    throw new AiRequestError("Network error contacting the AI service.", true);
  }
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 502 || res.status === 503 || res.status === 504) {
      throw new AiRequestError(
        "The AI service could not complete this request.",
        true,
      );
    }
    throw new AiRequestError(
      (payload as { error?: string } | null)?.error ??
        `Request failed (HTTP ${res.status})`,
      false,
    );
  }
  return payload as T;
}

function AiErrorCard({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div
      role="alert"
      className="flex items-start justify-between gap-4 rounded-xl border border-danger/25 bg-danger-tint p-4"
    >
      <div className="flex items-start gap-2">
        <AlertTriangle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden />
        <p className="text-sm text-danger">{message}</p>
      </div>
      <Button variant="secondary" size="sm" onClick={onRetry}>
        <RefreshCw className="size-3.5" aria-hidden />
        Retry
      </Button>
    </div>
  );
}

function Disclaimer({ text }: { text?: string | null }) {
  if (!text) return null;
  return <p className="mt-4 text-xs italic text-ink-faint">{text}</p>;
}

function SectionHeading({ children }: { children: React.ReactNode }) {
  return (
    <h3 className="font-display text-sm font-semibold uppercase tracking-wide text-ink-muted">
      {children}
    </h3>
  );
}

function ResultShell({
  loading,
  error,
  onRetry,
  empty,
  emptyHint,
  children,
}: {
  loading: boolean;
  error: string | null;
  onRetry: () => void;
  empty: boolean;
  emptyHint: string;
  children: React.ReactNode;
}) {
  return (
    <Card as="section" className="min-h-48">
      <CardTitle className="mb-4">Result</CardTitle>
      {loading ? (
        <EcgLoading label="Consulting model" />
      ) : error ? (
        <AiErrorCard message={error} onRetry={onRetry} />
      ) : empty ? (
        <EmptyState icon={Sparkles} title="No result yet" hint={emptyHint} />
      ) : (
        children
      )}
    </Card>
  );
}

function useAiCall<T>() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<T | null>(null);

  async function run(path: string, body: unknown) {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      setResult(await postAi<T>(path, body));
    } catch (e) {
      setError(
        e instanceof AiRequestError
          ? e.message
          : "The AI service could not complete this request.",
      );
    } finally {
      setLoading(false);
    }
  }

  return { loading, error, result, run };
}

function copyText(text: string, what: string) {
  navigator.clipboard
    .writeText(text)
    .then(() => toast.success(`${what} copied to clipboard.`))
    .catch(() => toast.error("Could not copy to clipboard."));
}

function AgeSexFields({
  age,
  sex,
  onAge,
  onSex,
}: {
  age: string;
  sex: string;
  onAge: (v: string) => void;
  onSex: (v: string) => void;
}) {
  return (
    <div className="grid grid-cols-2 gap-3">
      <Field label="Patient age">
        {(ids) => (
          <input
            {...ids}
            type="number"
            min={0}
            className="input"
            value={age}
            onChange={(e) => onAge(e.target.value)}
          />
        )}
      </Field>
      <Field label="Patient sex">
        {(ids) => (
          <select
            {...ids}
            className="input"
            value={sex}
            onChange={(e) => onSex(e.target.value)}
          >
            <option value="">—</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
          </select>
        )}
      </Field>
    </div>
  );
}

function BulletList({ items, tone }: { items: string[]; tone?: "danger" }) {
  return (
    <ul className="mt-2 space-y-1">
      {items.map((item, i) => (
        <li
          key={i}
          className={`flex items-start gap-2 text-sm ${
            tone === "danger" ? "text-danger" : "text-ink"
          }`}
        >
          <span
            className={`mt-1.5 size-1.5 shrink-0 rounded-full ${
              tone === "danger" ? "bg-danger" : "bg-brand"
            }`}
            aria-hidden
          />
          {item}
        </li>
      ))}
    </ul>
  );
}

// --- tool 1: prescription safety -------------------------------------------

function PrescriptionSafetyTool() {
  const [meds, setMeds] = useState([{ name: "", dosage: "" }]);
  const [allergies, setAllergies] = useState("");
  const [current, setCurrent] = useState("");
  const [age, setAge] = useState("");
  const [sex, setSex] = useState("");
  const { loading, error, result, run } =
    useAiCall<PrescriptionSafetyResponse>();

  function submit() {
    run("/api/ai/prescription-safety", {
      medications: meds
        .filter((m) => m.name.trim())
        .map((m) => ({
          name: m.name.trim(),
          dosage: m.dosage.trim() || undefined,
        })),
      knownAllergies: toList(allergies),
      currentMedications: toList(current),
      patientAgeYears: age ? parseInt(age, 10) : undefined,
      patientSex: sex || undefined,
    });
  }

  return (
    <div className="grid gap-6 xl:grid-cols-2">
      <Card as="section">
        <CardTitle className="mb-4">Prescription safety check</CardTitle>
        <div className="space-y-4">
          <div>
            <span className="mb-1 block text-xs font-medium text-ink-muted">
              Proposed medications
            </span>
            <div className="space-y-2">
              {meds.map((m, i) => (
                <div key={i} className="flex gap-2">
                  <input
                    aria-label={`Medication ${i + 1} name`}
                    className="input"
                    placeholder="Name (e.g. Warfarin)"
                    value={m.name}
                    onChange={(e) => {
                      const next = [...meds];
                      next[i] = { ...next[i], name: e.target.value };
                      setMeds(next);
                    }}
                  />
                  <input
                    aria-label={`Medication ${i + 1} dosage`}
                    className="input"
                    placeholder="Dosage (optional)"
                    value={m.dosage}
                    onChange={(e) => {
                      const next = [...meds];
                      next[i] = { ...next[i], dosage: e.target.value };
                      setMeds(next);
                    }}
                  />
                  {meds.length > 1 && (
                    <button
                      type="button"
                      onClick={() => setMeds(meds.filter((_, j) => j !== i))}
                      aria-label={`Remove medication ${i + 1}`}
                      className="rounded p-1 text-ink-faint hover:text-danger"
                    >
                      <X className="size-4" aria-hidden />
                    </button>
                  )}
                </div>
              ))}
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="mt-2"
              onClick={() => setMeds([...meds, { name: "", dosage: "" }])}
            >
              <Plus className="size-3.5" aria-hidden />
              Add medication
            </Button>
          </div>
          <Field label="Known allergies" hint="Comma-separated, e.g. penicillin, sulfa">
            {(ids) => (
              <input
                {...ids}
                className="input"
                value={allergies}
                onChange={(e) => setAllergies(e.target.value)}
              />
            )}
          </Field>
          <Field
            label="Current medications"
            hint="Comma-separated, e.g. Aspirin 81mg"
          >
            {(ids) => (
              <input
                {...ids}
                className="input"
                value={current}
                onChange={(e) => setCurrent(e.target.value)}
              />
            )}
          </Field>
          <AgeSexFields age={age} sex={sex} onAge={setAge} onSex={setSex} />
          <Button
            onClick={submit}
            loading={loading}
            disabled={!meds.some((m) => m.name.trim())}
          >
            Run safety check
          </Button>
        </div>
      </Card>

      <ResultShell
        loading={loading}
        error={error}
        onRetry={submit}
        empty={!result}
        emptyHint="Add the proposed medications and run the safety check."
      >
        {result && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-sm text-ink-muted">Overall risk</span>
              <StatusBadge status={statusOf(SEVERITY, result.overallRisk)} />
              {result.requiresPharmacistReview && (
                <Badge tone="danger">
                  <AlertTriangle className="size-3" aria-hidden />
                  Pharmacist review required
                </Badge>
              )}
            </div>
            <p className="text-sm text-ink">{result.summary}</p>
            {result.recommendation && (
              <p className="text-sm text-ink">
                <span className="font-medium">Recommendation: </span>
                {result.recommendation}
              </p>
            )}
            {result.interactions?.length > 0 && (
              <div>
                <SectionHeading>Drug interactions</SectionHeading>
                <ul className="mt-2 space-y-2">
                  {result.interactions.map((it, i) => (
                    <li key={i} className="rounded-lg bg-card-2 p-3 text-sm">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium text-ink">
                          {it.drugA} + {it.drugB}
                        </span>
                        <StatusBadge status={statusOf(SEVERITY, it.severity)} />
                      </div>
                      <p className="mt-1 text-ink-muted">
                        {it.clinicalConsequence}
                      </p>
                      {it.management && (
                        <p className="mt-1 text-xs text-ink-faint">
                          Management: {it.management}
                        </p>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {result.allergyConflicts?.length > 0 && (
              <div>
                <SectionHeading>Allergy conflicts</SectionHeading>
                <ul className="mt-2 space-y-2">
                  {result.allergyConflicts.map((c, i) => (
                    <li
                      key={i}
                      className="rounded-lg border border-danger/25 bg-danger-tint p-3 text-sm"
                    >
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium text-ink">
                          {c.medication} ↔ {c.allergen}
                        </span>
                        <StatusBadge status={statusOf(SEVERITY, c.severity)} />
                      </div>
                      <p className="mt-1 text-ink-muted">{c.note}</p>
                    </li>
                  ))}
                </ul>
              </div>
            )}
            <Disclaimer text={result.disclaimer} />
          </div>
        )}
      </ResultShell>
    </div>
  );
}

// --- tool 2: lab result explainer -------------------------------------------

const EMPTY_LAB_ROW = {
  testName: "",
  value: "",
  unit: "",
  referenceRange: "",
  flag: "",
};

function LabExplainerTool() {
  const [rows, setRows] = useState([{ ...EMPTY_LAB_ROW }]);
  const [age, setAge] = useState("");
  const [sex, setSex] = useState("");
  const { loading, error, result, run } = useAiCall<LabExplanationResponse>();

  function setRow(i: number, key: keyof typeof EMPTY_LAB_ROW, value: string) {
    const next = [...rows];
    next[i] = { ...next[i], [key]: value };
    setRows(next);
  }

  function submit() {
    run("/api/ai/lab-result-explanation", {
      results: rows
        .filter((r) => r.testName.trim())
        .map((r) => ({
          testName: r.testName.trim(),
          value: r.value.trim() || undefined,
          unit: r.unit.trim() || undefined,
          referenceRange: r.referenceRange.trim() || undefined,
          flag: r.flag.trim() || undefined,
        })),
      patientAgeYears: age ? parseInt(age, 10) : undefined,
      patientSex: sex || undefined,
    });
  }

  return (
    <div className="grid gap-6 xl:grid-cols-2">
      <Card as="section">
        <CardTitle className="mb-4">Lab result explainer</CardTitle>
        <div className="space-y-4">
          <div>
            <span className="mb-1 block text-xs font-medium text-ink-muted">
              Lab results
            </span>
            <div className="space-y-2">
              {rows.map((r, i) => (
                <div key={i} className="grid grid-cols-5 items-center gap-2">
                  <input
                    aria-label={`Row ${i + 1} test name`}
                    className="input"
                    placeholder="Test"
                    value={r.testName}
                    onChange={(e) => setRow(i, "testName", e.target.value)}
                  />
                  <input
                    aria-label={`Row ${i + 1} value`}
                    className="input tabular"
                    placeholder="Value"
                    value={r.value}
                    onChange={(e) => setRow(i, "value", e.target.value)}
                  />
                  <input
                    aria-label={`Row ${i + 1} unit`}
                    className="input"
                    placeholder="Unit"
                    value={r.unit}
                    onChange={(e) => setRow(i, "unit", e.target.value)}
                  />
                  <input
                    aria-label={`Row ${i + 1} reference range`}
                    className="input tabular"
                    placeholder="Ref range"
                    value={r.referenceRange}
                    onChange={(e) => setRow(i, "referenceRange", e.target.value)}
                  />
                  <div className="flex items-center gap-1">
                    <input
                      aria-label={`Row ${i + 1} flag`}
                      className="input"
                      placeholder="Flag"
                      value={r.flag}
                      onChange={(e) => setRow(i, "flag", e.target.value)}
                    />
                    {rows.length > 1 && (
                      <button
                        type="button"
                        onClick={() => setRows(rows.filter((_, j) => j !== i))}
                        aria-label={`Remove row ${i + 1}`}
                        className="rounded p-1 text-ink-faint hover:text-danger"
                      >
                        <X className="size-4" aria-hidden />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="mt-2"
              onClick={() => setRows([...rows, { ...EMPTY_LAB_ROW }])}
            >
              <Plus className="size-3.5" aria-hidden />
              Add result
            </Button>
          </div>
          <AgeSexFields age={age} sex={sex} onAge={setAge} onSex={setSex} />
          <Button
            onClick={submit}
            loading={loading}
            disabled={!rows.some((r) => r.testName.trim())}
          >
            Explain results
          </Button>
        </div>
      </Card>

      <ResultShell
        loading={loading}
        error={error}
        onRetry={submit}
        empty={!result}
        emptyHint="Enter one or more lab results to get a plain-language explanation."
      >
        {result && <LabExplanationView result={result} />}
      </ResultShell>
    </div>
  );
}

// --- tool 3: symptom triage --------------------------------------------------

function SymptomTriageTool() {
  const [symptoms, setSymptoms] = useState("");
  const [duration, setDuration] = useState("");
  const [conditions, setConditions] = useState("");
  const [medications, setMedications] = useState("");
  const [allergies, setAllergies] = useState("");
  const [age, setAge] = useState("");
  const [sex, setSex] = useState("");
  const { loading, error, result, run } = useAiCall<SymptomTriageResponse>();

  function submit() {
    run("/api/ai/symptom-triage", {
      symptoms: symptoms.trim(),
      duration: duration.trim() || undefined,
      patientAgeYears: age ? parseInt(age, 10) : undefined,
      patientSex: sex || undefined,
      knownConditions: toList(conditions),
      currentMedications: toList(medications),
      knownAllergies: toList(allergies),
    });
  }

  return (
    <div className="grid gap-6 xl:grid-cols-2">
      <Card as="section">
        <CardTitle className="mb-4">Symptom triage</CardTitle>
        <div className="space-y-4">
          <Field label="Symptoms" required>
            {(ids) => (
              <textarea
                {...ids}
                className="input min-h-24"
                placeholder="e.g. crushing chest pain radiating to left arm, shortness of breath"
                value={symptoms}
                onChange={(e) => setSymptoms(e.target.value)}
              />
            )}
          </Field>
          <Field label="Duration" hint="e.g. 2 days, started this morning">
            {(ids) => (
              <input
                {...ids}
                className="input"
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
              />
            )}
          </Field>
          <Field label="Known conditions" hint="Comma-separated">
            {(ids) => (
              <input
                {...ids}
                className="input"
                value={conditions}
                onChange={(e) => setConditions(e.target.value)}
              />
            )}
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Current medications" hint="Comma-separated">
              {(ids) => (
                <input
                  {...ids}
                  className="input"
                  value={medications}
                  onChange={(e) => setMedications(e.target.value)}
                />
              )}
            </Field>
            <Field label="Known allergies" hint="Comma-separated">
              {(ids) => (
                <input
                  {...ids}
                  className="input"
                  value={allergies}
                  onChange={(e) => setAllergies(e.target.value)}
                />
              )}
            </Field>
          </div>
          <AgeSexFields age={age} sex={sex} onAge={setAge} onSex={setSex} />
          <Button onClick={submit} loading={loading} disabled={!symptoms.trim()}>
            Triage symptoms
          </Button>
        </div>
      </Card>

      <ResultShell
        loading={loading}
        error={error}
        onRetry={submit}
        empty={!result}
        emptyHint="Describe the patient's symptoms to get an urgency assessment."
      >
        {result && (
          <div className="space-y-4">
            {result.emergency && (
              <div
                role="alert"
                className="flex items-center gap-3 rounded-xl border-2 border-danger bg-danger-tint p-4"
              >
                <Siren className="size-6 shrink-0 text-danger" aria-hidden />
                <div>
                  <p className="font-display text-base font-bold text-danger">
                    Call emergency services
                  </p>
                  <p className="text-sm text-danger">
                    This presentation may be life-threatening. Do not wait for an
                    appointment.
                  </p>
                </div>
              </div>
            )}
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-sm text-ink-muted">Urgency</span>
              <StatusBadge status={statusOf(URGENCY, result.urgency)} />
              {result.recommendedSpecialty && (
                <>
                  <span className="text-sm text-ink-muted">Refer to</span>
                  <Badge tone="brand">{result.recommendedSpecialty}</Badge>
                </>
              )}
            </div>
            {result.redFlags?.length > 0 && (
              <div>
                <SectionHeading>Red flags</SectionHeading>
                <BulletList items={result.redFlags} tone="danger" />
              </div>
            )}
            <div>
              <SectionHeading>Rationale</SectionHeading>
              <p className="mt-2 text-sm text-ink">{result.rationale}</p>
            </div>
            {result.selfCareAdvice && (
              <div>
                <SectionHeading>Self-care advice</SectionHeading>
                <p className="mt-2 rounded-lg bg-card-2 p-3 text-sm text-ink">
                  {result.selfCareAdvice}
                </p>
              </div>
            )}
            <Disclaimer text={result.disclaimer} />
          </div>
        )}
      </ResultShell>
    </div>
  );
}

// --- tool 4: SOAP note --------------------------------------------------------

function SoapNoteTool() {
  const [notes, setNotes] = useState("");
  const [conditions, setConditions] = useState("");
  const [vitals, setVitals] = useState([{ name: "", value: "" }]);
  const [age, setAge] = useState("");
  const [sex, setSex] = useState("");
  const { loading, error, result, run } = useAiCall<SoapNoteResponse>();

  function submit() {
    const vitalsMap: Record<string, string> = {};
    for (const v of vitals) {
      if (v.name.trim() && v.value.trim()) vitalsMap[v.name.trim()] = v.value.trim();
    }
    run("/api/ai/soap-note", {
      consultationNotes: notes.trim(),
      patientAgeYears: age ? parseInt(age, 10) : undefined,
      patientSex: sex || undefined,
      knownConditions: toList(conditions),
      vitals: vitalsMap,
    });
  }

  function copyNote() {
    if (!result) return;
    copyText(
      [
        `SUBJECTIVE\n${result.subjective}`,
        `OBJECTIVE\n${result.objective}`,
        `ASSESSMENT\n${result.assessment}`,
        `PLAN\n${result.plan}`,
        result.followUp ? `FOLLOW-UP\n${result.followUp}` : null,
      ]
        .filter(Boolean)
        .join("\n\n"),
      "SOAP note",
    );
  }

  const sections: { label: string; letter: string; text: string }[] = result
    ? [
        { label: "Subjective", letter: "S", text: result.subjective },
        { label: "Objective", letter: "O", text: result.objective },
        { label: "Assessment", letter: "A", text: result.assessment },
        { label: "Plan", letter: "P", text: result.plan },
      ]
    : [];

  return (
    <div className="grid gap-6 xl:grid-cols-2">
      <Card as="section">
        <CardTitle className="mb-4">SOAP note generator</CardTitle>
        <div className="space-y-4">
          <Field label="Consultation notes" required>
            {(ids) => (
              <textarea
                {...ids}
                className="input min-h-36"
                placeholder="Paste free-text consultation notes to structure into a SOAP note…"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            )}
          </Field>
          <Field label="Known conditions" hint="Comma-separated">
            {(ids) => (
              <input
                {...ids}
                className="input"
                value={conditions}
                onChange={(e) => setConditions(e.target.value)}
              />
            )}
          </Field>
          <div>
            <span className="mb-1 block text-xs font-medium text-ink-muted">
              Vitals
            </span>
            <div className="space-y-2">
              {vitals.map((v, i) => (
                <div key={i} className="flex gap-2">
                  <input
                    aria-label={`Vital ${i + 1} name`}
                    className="input"
                    placeholder="Name (e.g. BP)"
                    value={v.name}
                    onChange={(e) => {
                      const next = [...vitals];
                      next[i] = { ...next[i], name: e.target.value };
                      setVitals(next);
                    }}
                  />
                  <input
                    aria-label={`Vital ${i + 1} value`}
                    className="input tabular"
                    placeholder="Value (e.g. 128/82)"
                    value={v.value}
                    onChange={(e) => {
                      const next = [...vitals];
                      next[i] = { ...next[i], value: e.target.value };
                      setVitals(next);
                    }}
                  />
                  {vitals.length > 1 && (
                    <button
                      type="button"
                      onClick={() => setVitals(vitals.filter((_, j) => j !== i))}
                      aria-label={`Remove vital ${i + 1}`}
                      className="rounded p-1 text-ink-faint hover:text-danger"
                    >
                      <X className="size-4" aria-hidden />
                    </button>
                  )}
                </div>
              ))}
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="mt-2"
              onClick={() => setVitals([...vitals, { name: "", value: "" }])}
            >
              <Plus className="size-3.5" aria-hidden />
              Add vital
            </Button>
          </div>
          <AgeSexFields age={age} sex={sex} onAge={setAge} onSex={setSex} />
          <Button onClick={submit} loading={loading} disabled={!notes.trim()}>
            Generate SOAP note
          </Button>
        </div>
      </Card>

      <ResultShell
        loading={loading}
        error={error}
        onRetry={submit}
        empty={!result}
        emptyHint="Paste consultation notes and generate a structured S/O/A/P document."
      >
        {result && (
          <div className="space-y-4">
            <div className="flex justify-end">
              <Button variant="secondary" size="sm" onClick={copyNote}>
                <Copy className="size-3.5" aria-hidden />
                Copy note
              </Button>
            </div>
            {sections.map((s) => (
              <section key={s.letter} className="flex gap-3">
                <span
                  aria-hidden
                  className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-brand-tint font-display text-sm font-bold text-brand-ink"
                >
                  {s.letter}
                </span>
                <div className="min-w-0">
                  <SectionHeading>{s.label}</SectionHeading>
                  <p className="mt-1 whitespace-pre-wrap text-sm text-ink">
                    {s.text}
                  </p>
                </div>
              </section>
            ))}
            {result.assessmentProblems?.length > 0 && (
              <div>
                <SectionHeading>Problem list</SectionHeading>
                <BulletList items={result.assessmentProblems} />
              </div>
            )}
            {result.followUp && (
              <p className="text-sm text-ink">
                <span className="font-medium">Follow-up: </span>
                {result.followUp}
              </p>
            )}
            <Disclaimer text={result.disclaimer} />
          </div>
        )}
      </ResultShell>
    </div>
  );
}

// --- tool 5: ICD-10 codes -------------------------------------------------------

const CONFIDENCE_TONE = { HIGH: "ok", MODERATE: "warn", LOW: "neutral" } as const;

function IcdCodesTool() {
  const [notes, setNotes] = useState("");
  const [existing, setExisting] = useState("");
  const { loading, error, result, run } = useAiCall<IcdCodesResponse>();

  function submit() {
    run("/api/ai/icd-codes", {
      clinicalNotes: notes.trim(),
      existingDiagnosis: existing.trim() || undefined,
    });
  }

  return (
    <div className="grid gap-6 xl:grid-cols-2">
      <Card as="section">
        <CardTitle className="mb-4">ICD-10 code suggestions</CardTitle>
        <div className="space-y-4">
          <Field label="Clinical notes" required>
            {(ids) => (
              <textarea
                {...ids}
                className="input min-h-36"
                placeholder="Describe the clinical findings and diagnosis…"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            )}
          </Field>
          <Field label="Existing diagnosis" hint="Optional working diagnosis">
            {(ids) => (
              <input
                {...ids}
                className="input"
                value={existing}
                onChange={(e) => setExisting(e.target.value)}
              />
            )}
          </Field>
          <Button onClick={submit} loading={loading} disabled={!notes.trim()}>
            Suggest codes
          </Button>
        </div>
      </Card>

      <ResultShell
        loading={loading}
        error={error}
        onRetry={submit}
        empty={!result}
        emptyHint="Enter clinical notes to receive up to 8 ICD-10 code suggestions."
      >
        {result && (
          <div className="space-y-4">
            {result.suggestions?.length > 0 ? (
              <ul className="divide-y divide-line">
                {result.suggestions.map((s) => (
                  <li key={s.code} className="flex items-start gap-3 py-3">
                    <code className="tabular rounded bg-brand-tint px-2 py-0.5 text-sm font-semibold text-brand-ink">
                      {s.code}
                    </code>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-medium text-ink">
                          {s.description}
                        </span>
                        <Badge tone={CONFIDENCE_TONE[s.confidence] ?? "neutral"}>
                          {s.confidence}
                        </Badge>
                      </div>
                      <p className="mt-1 text-xs text-ink-muted">{s.rationale}</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => copyText(s.code, `Code ${s.code}`)}
                      aria-label={`Copy code ${s.code}`}
                      className="rounded p-1.5 text-ink-faint hover:bg-card-2 hover:text-ink"
                    >
                      <Copy className="size-4" aria-hidden />
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState
                icon={ListChecks}
                title="No codes suggested"
                hint="The model did not find codeable diagnoses in these notes."
              />
            )}
            <Disclaimer text={result.caveat} />
          </div>
        )}
      </ResultShell>
    </div>
  );
}

// --- tool 6: history summary -----------------------------------------------------

function HistorySummaryTool() {
  const [conditions, setConditions] = useState("");
  const [medications, setMedications] = useState("");
  const [allergies, setAllergies] = useState("");
  const [labs, setLabs] = useState([{ name: "", value: "", flag: "" }]);
  const [visits, setVisits] = useState([{ date: "", note: "" }]);
  const [age, setAge] = useState("");
  const [sex, setSex] = useState("");
  const { loading, error, result, run } = useAiCall<HistorySummaryResponse>();

  const hasInput =
    toList(conditions).length > 0 ||
    toList(medications).length > 0 ||
    toList(allergies).length > 0 ||
    labs.some((l) => l.name.trim()) ||
    visits.some((v) => v.note.trim());

  function submit() {
    run("/api/ai/history-summary", {
      patientAgeYears: age ? parseInt(age, 10) : undefined,
      patientSex: sex || undefined,
      conditions: toList(conditions),
      medications: toList(medications),
      allergies: toList(allergies),
      recentLabResults: labs
        .filter((l) => l.name.trim())
        .map((l) => ({
          name: l.name.trim(),
          value: l.value.trim() || undefined,
          flag: l.flag.trim() || undefined,
        })),
      pastVisits: visits
        .filter((v) => v.note.trim())
        .map((v) => ({
          date: v.date.trim() || undefined,
          note: v.note.trim(),
        })),
    });
  }

  const listSections: { heading: string; items: string[]; danger?: boolean }[] =
    result
      ? [
          { heading: "Key conditions", items: result.keyConditions ?? [] },
          { heading: "Active medications", items: result.activeMedications ?? [] },
          {
            heading: "Critical allergies",
            items: result.criticalAllergies ?? [],
            danger: true,
          },
          {
            heading: "Recent abnormal findings",
            items: result.recentAbnormalFindings ?? [],
          },
          { heading: "Red flags", items: result.redFlags ?? [], danger: true },
        ].filter((s) => s.items.length > 0)
      : [];

  return (
    <div className="grid gap-6 xl:grid-cols-2">
      <Card as="section">
        <CardTitle className="mb-4">Patient history summary</CardTitle>
        <div className="space-y-4">
          <Field label="Conditions" hint="Comma-separated">
            {(ids) => (
              <input
                {...ids}
                className="input"
                value={conditions}
                onChange={(e) => setConditions(e.target.value)}
              />
            )}
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Medications" hint="Comma-separated">
              {(ids) => (
                <input
                  {...ids}
                  className="input"
                  value={medications}
                  onChange={(e) => setMedications(e.target.value)}
                />
              )}
            </Field>
            <Field label="Allergies" hint="Comma-separated">
              {(ids) => (
                <input
                  {...ids}
                  className="input"
                  value={allergies}
                  onChange={(e) => setAllergies(e.target.value)}
                />
              )}
            </Field>
          </div>
          <div>
            <span className="mb-1 block text-xs font-medium text-ink-muted">
              Recent lab results
            </span>
            <div className="space-y-2">
              {labs.map((l, i) => (
                <div key={i} className="flex gap-2">
                  <input
                    aria-label={`Lab ${i + 1} name`}
                    className="input"
                    placeholder="Test"
                    value={l.name}
                    onChange={(e) => {
                      const next = [...labs];
                      next[i] = { ...next[i], name: e.target.value };
                      setLabs(next);
                    }}
                  />
                  <input
                    aria-label={`Lab ${i + 1} value`}
                    className="input tabular"
                    placeholder="Value"
                    value={l.value}
                    onChange={(e) => {
                      const next = [...labs];
                      next[i] = { ...next[i], value: e.target.value };
                      setLabs(next);
                    }}
                  />
                  <input
                    aria-label={`Lab ${i + 1} flag`}
                    className="input"
                    placeholder="Flag"
                    value={l.flag}
                    onChange={(e) => {
                      const next = [...labs];
                      next[i] = { ...next[i], flag: e.target.value };
                      setLabs(next);
                    }}
                  />
                  {labs.length > 1 && (
                    <button
                      type="button"
                      onClick={() => setLabs(labs.filter((_, j) => j !== i))}
                      aria-label={`Remove lab ${i + 1}`}
                      className="rounded p-1 text-ink-faint hover:text-danger"
                    >
                      <X className="size-4" aria-hidden />
                    </button>
                  )}
                </div>
              ))}
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="mt-2"
              onClick={() => setLabs([...labs, { name: "", value: "", flag: "" }])}
            >
              <Plus className="size-3.5" aria-hidden />
              Add lab result
            </Button>
          </div>
          <div>
            <span className="mb-1 block text-xs font-medium text-ink-muted">
              Past visits
            </span>
            <div className="space-y-2">
              {visits.map((v, i) => (
                <div key={i} className="flex gap-2">
                  <input
                    aria-label={`Visit ${i + 1} date`}
                    type="date"
                    className="input tabular max-w-40"
                    value={v.date}
                    onChange={(e) => {
                      const next = [...visits];
                      next[i] = { ...next[i], date: e.target.value };
                      setVisits(next);
                    }}
                  />
                  <input
                    aria-label={`Visit ${i + 1} note`}
                    className="input"
                    placeholder="Visit note"
                    value={v.note}
                    onChange={(e) => {
                      const next = [...visits];
                      next[i] = { ...next[i], note: e.target.value };
                      setVisits(next);
                    }}
                  />
                  {visits.length > 1 && (
                    <button
                      type="button"
                      onClick={() => setVisits(visits.filter((_, j) => j !== i))}
                      aria-label={`Remove visit ${i + 1}`}
                      className="rounded p-1 text-ink-faint hover:text-danger"
                    >
                      <X className="size-4" aria-hidden />
                    </button>
                  )}
                </div>
              ))}
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="mt-2"
              onClick={() => setVisits([...visits, { date: "", note: "" }])}
            >
              <Plus className="size-3.5" aria-hidden />
              Add visit
            </Button>
          </div>
          <AgeSexFields age={age} sex={sex} onAge={setAge} onSex={setSex} />
          <Button onClick={submit} loading={loading} disabled={!hasInput}>
            Summarize history
          </Button>
        </div>
      </Card>

      <ResultShell
        loading={loading}
        error={error}
        onRetry={submit}
        empty={!result}
        emptyHint="Provide conditions, medications, labs, or visits to build a clinical summary."
      >
        {result && (
          <div className="space-y-4">
            <p className="rounded-lg bg-card-2 p-3 text-sm leading-relaxed text-ink">
              {result.narrativeSummary}
            </p>
            {listSections.map((s) => (
              <div key={s.heading}>
                <SectionHeading>{s.heading}</SectionHeading>
                <BulletList items={s.items} tone={s.danger ? "danger" : undefined} />
              </div>
            ))}
            {result.suggestedFollowUps?.length > 0 && (
              <div>
                <SectionHeading>Suggested follow-ups</SectionHeading>
                <BulletList items={result.suggestedFollowUps} />
              </div>
            )}
            <Disclaimer text={result.disclaimer} />
          </div>
        )}
      </ResultShell>
    </div>
  );
}

// --- console shell ------------------------------------------------------------

const TOOLS = [
  {
    id: "prescription-safety",
    label: "Prescription safety",
    description: "Screen for drug interactions and allergy conflicts",
    icon: ShieldCheck,
    render: () => <PrescriptionSafetyTool />,
  },
  {
    id: "lab-explainer",
    label: "Lab result explainer",
    description: "Plain-language interpretation of lab panels",
    icon: FlaskConical,
    render: () => <LabExplainerTool />,
  },
  {
    id: "symptom-triage",
    label: "Symptom triage",
    description: "Urgency assessment and specialty routing",
    icon: Stethoscope,
    render: () => <SymptomTriageTool />,
  },
  {
    id: "soap-note",
    label: "SOAP note",
    description: "Structure free-text notes into S/O/A/P",
    icon: FileText,
    render: () => <SoapNoteTool />,
  },
  {
    id: "icd-codes",
    label: "ICD-10 codes",
    description: "Diagnosis coding suggestions from notes",
    icon: ListChecks,
    render: () => <IcdCodesTool />,
  },
  {
    id: "history-summary",
    label: "History summary",
    description: "Condense a patient chart into key findings",
    icon: History,
    render: () => <HistorySummaryTool />,
  },
] as const;

type ToolId = (typeof TOOLS)[number]["id"];

export default function AiCdssPage() {
  const [active, setActive] = useState<ToolId>("prescription-safety");
  const tool = TOOLS.find((t) => t.id === active) ?? TOOLS[0];

  return (
    <div>
      <PageHeader
        eyebrow="Clinical decision support"
        title="AI clinical console"
        description="Advisory only — powered by TensorX open-weight inference. Verify every result with a licensed clinician or pharmacist before acting."
      />
      <div className="grid gap-6 lg:grid-cols-[240px_1fr]">
        <nav aria-label="AI tools">
          <ul className="flex gap-2 overflow-x-auto lg:flex-col lg:overflow-visible">
            {TOOLS.map((t) => {
              const selected = t.id === active;
              return (
                <li key={t.id} className="shrink-0 lg:shrink">
                  <button
                    type="button"
                    aria-current={selected ? "true" : undefined}
                    onClick={() => setActive(t.id)}
                    className={`flex w-full items-start gap-3 rounded-lg border px-3 py-2.5 text-left transition-colors ${
                      selected
                        ? "border-brand/30 bg-brand-tint text-brand-ink"
                        : "border-transparent text-ink-muted hover:bg-card-2 hover:text-ink"
                    }`}
                  >
                    <t.icon
                      className={`mt-0.5 size-4 shrink-0 ${
                        selected ? "text-brand-ink" : "text-ink-faint"
                      }`}
                      aria-hidden
                    />
                    <span className="min-w-0">
                      <span className="block text-sm font-medium whitespace-nowrap lg:whitespace-normal">
                        {t.label}
                      </span>
                      <span className="hidden text-xs text-ink-faint lg:block">
                        {t.description}
                      </span>
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        </nav>
        <div className="min-w-0">{tool.render()}</div>
      </div>
    </div>
  );
}
