import { StatusBadge, URGENCY, statusOf } from "@/components/ui";
import type { LabExplanationResponse } from "@/lib/types";

/**
 * Renders an AI lab-result explanation: urgency badge, summaries, per-test
 * notes, follow-up, and disclaimer. Shared between the AI console and the
 * embedded "Explain with AI" flow on lab order results.
 */
export function LabExplanationView({
  result,
}: {
  result: LabExplanationResponse;
}) {
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <span className="text-sm text-ink-muted">Urgency</span>
        <StatusBadge status={statusOf(URGENCY, result.urgency)} />
      </div>
      <p className="text-sm text-ink">{result.overallSummary}</p>
      {result.patientFriendlySummary && (
        <p className="rounded-lg bg-brand-tint p-3 text-sm text-ink">
          <span className="font-medium text-brand-ink">In plain language: </span>
          {result.patientFriendlySummary}
        </p>
      )}
      {result.results?.length > 0 && (
        <ul className="space-y-2">
          {result.results.map((d, i) => (
            <li key={i} className="rounded-lg bg-card-2 p-3 text-sm">
              <span className="font-medium text-ink">{d.testName}</span>
              {d.interpretation && (
                <span className="text-ink-muted"> — {d.interpretation}</span>
              )}
              <p className="mt-1 text-ink-muted">{d.explanation}</p>
              {d.clinicalSignificance && (
                <p className="mt-1 text-xs text-ink-faint">
                  {d.clinicalSignificance}
                </p>
              )}
            </li>
          ))}
        </ul>
      )}
      {result.suggestedFollowUp && (
        <p className="text-sm text-ink">
          <span className="font-medium">Suggested follow-up: </span>
          {result.suggestedFollowUp}
        </p>
      )}
      {result.disclaimer && (
        <p className="mt-4 text-xs italic text-ink-faint">{result.disclaimer}</p>
      )}
    </div>
  );
}
