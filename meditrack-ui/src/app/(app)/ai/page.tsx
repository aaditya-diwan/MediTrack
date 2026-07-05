"use client";

import { useState } from "react";

// --- shared helpers -------------------------------------------------------

function severityClasses(level: string): string {
  switch (level?.toUpperCase()) {
    case "NONE":
    case "ROUTINE":
      return "bg-emerald-100 text-emerald-800";
    case "MINOR":
    case "MONITOR":
      return "bg-amber-100 text-amber-800";
    case "MODERATE":
      return "bg-orange-100 text-orange-800";
    case "MAJOR":
    case "URGENT":
      return "bg-red-100 text-red-800";
    case "CONTRAINDICATED":
    case "CRITICAL":
      return "bg-red-200 text-red-900 font-semibold";
    default:
      return "bg-slate-100 text-slate-700";
  }
}

function Badge({ level }: { level: string }) {
  return (
    <span className={`inline-block text-xs px-2 py-0.5 rounded-full ${severityClasses(level)}`}>
      {level}
    </span>
  );
}

function toList(csv: string): string[] {
  return csv
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="border border-slate-200 rounded-xl p-5 bg-white shadow-sm">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">{title}</h2>
      {children}
    </section>
  );
}

const inputCls =
  "w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400";
const btnCls =
  "bg-slate-800 text-white text-sm px-5 py-2 rounded-lg hover:bg-slate-700 disabled:opacity-50 transition-colors";

// --- response types -------------------------------------------------------

interface Interaction {
  drugA: string;
  drugB: string;
  severity: string;
  mechanism: string;
  clinicalConsequence: string;
  management: string;
}
interface AllergyConflict {
  medication: string;
  allergen: string;
  severity: string;
  note: string;
}
interface SafetyResult {
  overallRisk: string;
  requiresPharmacistReview: boolean;
  summary: string;
  recommendation: string;
  interactions: Interaction[];
  allergyConflicts: AllergyConflict[];
  modelUsed: string;
  disclaimer: string;
}
interface LabDetail {
  testName: string;
  interpretation: string;
  explanation: string;
  clinicalSignificance: string;
}
interface LabResult {
  urgency: string;
  overallSummary: string;
  patientFriendlySummary: string;
  suggestedFollowUp: string;
  results: LabDetail[];
  modelUsed: string;
  disclaimer: string;
}

// --- prescription safety panel --------------------------------------------

function PrescriptionSafety() {
  const [meds, setMeds] = useState([{ name: "", dosage: "" }]);
  const [allergies, setAllergies] = useState("");
  const [current, setCurrent] = useState("");
  const [age, setAge] = useState("");
  const [sex, setSex] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SafetyResult | null>(null);

  async function submit() {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await fetch("/api/ai/prescription-safety", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          medications: meds
            .filter((m) => m.name.trim())
            .map((m) => ({ name: m.name.trim(), dosage: m.dosage.trim() || undefined })),
          knownAllergies: toList(allergies),
          currentMedications: toList(current),
          patientAgeYears: age ? parseInt(age, 10) : undefined,
          patientSex: sex || undefined,
        }),
      });
      const body = await res.json();
      if (!res.ok) {
        setError(body.error || `Request failed (HTTP ${res.status})`);
        return;
      }
      setResult(body as SafetyResult);
    } catch {
      setError("Network error contacting the AI service.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card title="Prescription Safety Check">
      <div className="space-y-3">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Medications</label>
          {meds.map((m, i) => (
            <div key={i} className="flex gap-2 mb-2">
              <input
                className={inputCls}
                placeholder="Name (e.g. Warfarin)"
                value={m.name}
                onChange={(e) => {
                  const next = [...meds];
                  next[i] = { ...next[i], name: e.target.value };
                  setMeds(next);
                }}
              />
              <input
                className={inputCls}
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
                  className="text-slate-400 hover:text-red-500 px-2"
                >
                  ×
                </button>
              )}
            </div>
          ))}
          <button
            type="button"
            onClick={() => setMeds([...meds, { name: "", dosage: "" }])}
            className="text-xs text-slate-500 hover:text-slate-800"
          >
            + Add medication
          </button>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Known allergies (comma-separated)
            </label>
            <input className={inputCls} placeholder="penicillin, sulfa" value={allergies} onChange={(e) => setAllergies(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Current medications (comma-separated)
            </label>
            <input className={inputCls} placeholder="Aspirin 81mg" value={current} onChange={(e) => setCurrent(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Patient age</label>
            <input className={inputCls} type="number" value={age} onChange={(e) => setAge(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Patient sex</label>
            <select className={inputCls} value={sex} onChange={(e) => setSex(e.target.value)}>
              <option value="">—</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
            </select>
          </div>
        </div>
        <button className={btnCls} onClick={submit} disabled={loading}>
          {loading ? "Screening…" : "Run safety check"}
        </button>
        {error && <p className="text-red-500 text-sm">{error}</p>}

        {result && (
          <div className="mt-4 border-t border-slate-100 pt-4 space-y-3">
            <div className="flex items-center gap-3">
              <span className="text-sm text-slate-500">Overall risk:</span>
              <Badge level={result.overallRisk} />
              {result.requiresPharmacistReview && (
                <span className="text-xs text-red-600">⚠ Pharmacist review required</span>
              )}
            </div>
            <p className="text-sm text-slate-700">{result.summary}</p>
            {result.recommendation && (
              <p className="text-sm text-slate-700">
                <span className="font-medium">Recommendation: </span>
                {result.recommendation}
              </p>
            )}
            {result.interactions?.length > 0 && (
              <div>
                <h3 className="text-sm font-semibold text-slate-700 mb-1">Drug interactions</h3>
                <ul className="space-y-2">
                  {result.interactions.map((it, i) => (
                    <li key={i} className="text-sm bg-slate-50 rounded-lg p-3">
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{it.drugA} + {it.drugB}</span>
                        <Badge level={it.severity} />
                      </div>
                      <p className="text-slate-600 mt-1">{it.clinicalConsequence}</p>
                      <p className="text-slate-500 text-xs mt-1">Management: {it.management}</p>
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {result.allergyConflicts?.length > 0 && (
              <div>
                <h3 className="text-sm font-semibold text-slate-700 mb-1">Allergy conflicts</h3>
                <ul className="space-y-2">
                  {result.allergyConflicts.map((c, i) => (
                    <li key={i} className="text-sm bg-red-50 rounded-lg p-3">
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{c.medication} ↔ {c.allergen}</span>
                        <Badge level={c.severity} />
                      </div>
                      <p className="text-slate-600 mt-1">{c.note}</p>
                    </li>
                  ))}
                </ul>
              </div>
            )}
            <p className="text-xs text-slate-400 italic">{result.disclaimer}</p>
          </div>
        )}
      </div>
    </Card>
  );
}

// --- lab result explainer panel -------------------------------------------

function LabExplainer() {
  const [rows, setRows] = useState([{ testName: "", value: "", unit: "", referenceRange: "", flag: "" }]);
  const [age, setAge] = useState("");
  const [sex, setSex] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<LabResult | null>(null);

  function setRow(i: number, key: string, value: string) {
    const next = [...rows];
    next[i] = { ...next[i], [key]: value };
    setRows(next);
  }

  async function submit() {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await fetch("/api/ai/lab-result-explanation", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
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
        }),
      });
      const body = await res.json();
      if (!res.ok) {
        setError(body.error || `Request failed (HTTP ${res.status})`);
        return;
      }
      setResult(body as LabResult);
    } catch {
      setError("Network error contacting the AI service.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card title="Lab Result Explainer">
      <div className="space-y-3">
        <label className="block text-sm font-medium text-slate-700">Lab results</label>
        {rows.map((r, i) => (
          <div key={i} className="grid grid-cols-5 gap-2 items-center">
            <input className={inputCls} placeholder="Test" value={r.testName} onChange={(e) => setRow(i, "testName", e.target.value)} />
            <input className={inputCls} placeholder="Value" value={r.value} onChange={(e) => setRow(i, "value", e.target.value)} />
            <input className={inputCls} placeholder="Unit" value={r.unit} onChange={(e) => setRow(i, "unit", e.target.value)} />
            <input className={inputCls} placeholder="Ref range" value={r.referenceRange} onChange={(e) => setRow(i, "referenceRange", e.target.value)} />
            <div className="flex gap-1">
              <input className={inputCls} placeholder="Flag" value={r.flag} onChange={(e) => setRow(i, "flag", e.target.value)} />
              {rows.length > 1 && (
                <button type="button" onClick={() => setRows(rows.filter((_, j) => j !== i))} className="text-slate-400 hover:text-red-500 px-1">×</button>
              )}
            </div>
          </div>
        ))}
        <button
          type="button"
          onClick={() => setRows([...rows, { testName: "", value: "", unit: "", referenceRange: "", flag: "" }])}
          className="text-xs text-slate-500 hover:text-slate-800"
        >
          + Add result
        </button>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Patient age</label>
            <input className={inputCls} type="number" value={age} onChange={(e) => setAge(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Patient sex</label>
            <select className={inputCls} value={sex} onChange={(e) => setSex(e.target.value)}>
              <option value="">—</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
            </select>
          </div>
        </div>
        <button className={btnCls} onClick={submit} disabled={loading}>
          {loading ? "Explaining…" : "Explain results"}
        </button>
        {error && <p className="text-red-500 text-sm">{error}</p>}

        {result && (
          <div className="mt-4 border-t border-slate-100 pt-4 space-y-3">
            <div className="flex items-center gap-3">
              <span className="text-sm text-slate-500">Urgency:</span>
              <Badge level={result.urgency} />
            </div>
            <p className="text-sm text-slate-700">{result.overallSummary}</p>
            {result.patientFriendlySummary && (
              <p className="text-sm bg-slate-50 rounded-lg p-3 text-slate-700">
                <span className="font-medium">In plain language: </span>
                {result.patientFriendlySummary}
              </p>
            )}
            {result.results?.length > 0 && (
              <ul className="space-y-2">
                {result.results.map((d, i) => (
                  <li key={i} className="text-sm bg-slate-50 rounded-lg p-3">
                    <span className="font-medium">{d.testName}</span>
                    {d.interpretation && <span className="text-slate-500"> — {d.interpretation}</span>}
                    <p className="text-slate-600 mt-1">{d.explanation}</p>
                    {d.clinicalSignificance && (
                      <p className="text-slate-500 text-xs mt-1">{d.clinicalSignificance}</p>
                    )}
                  </li>
                ))}
              </ul>
            )}
            {result.suggestedFollowUp && (
              <p className="text-sm text-slate-700">
                <span className="font-medium">Suggested follow-up: </span>
                {result.suggestedFollowUp}
              </p>
            )}
            <p className="text-xs text-slate-400 italic">{result.disclaimer}</p>
          </div>
        )}
      </div>
    </Card>
  );
}

export default function AiCdssPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">AI Clinical Decision Support</h1>
        <p className="text-sm text-slate-500 mt-1">
          Advisory only — powered by TensorX open-weight inference. Verify every result with a
          licensed clinician or pharmacist before acting.
        </p>
      </div>
      <PrescriptionSafety />
      <LabExplainer />
    </div>
  );
}
