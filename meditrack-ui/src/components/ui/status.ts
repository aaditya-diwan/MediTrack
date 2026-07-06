// Single source of truth for clinical status semantics.
// Tones map to the token palette; the same status must never render with
// different colors on different screens — these maps are the only place
// a status→color decision is made.

export type Tone = "neutral" | "brand" | "ok" | "warn" | "danger" | "info";

export interface StatusStyle {
  label: string;
  tone: Tone;
}

const s = (label: string, tone: Tone): StatusStyle => ({ label, tone });

export const APPOINTMENT_STATUS: Record<string, StatusStyle> = {
  PENDING: s("Pending", "warn"),
  CONFIRMED: s("Confirmed", "info"),
  IN_PROGRESS: s("In progress", "brand"),
  COMPLETED: s("Completed", "ok"),
  CANCELLED: s("Cancelled", "neutral"),
  NO_SHOW: s("No-show", "danger"),
};

export const PRESCRIPTION_STATUS: Record<string, StatusStyle> = {
  DRAFT: s("Draft", "neutral"),
  ISSUED: s("Issued", "info"),
  SENT_TO_PHARMACY: s("Sent to pharmacy", "brand"),
  SENT_TO_LAB: s("Sent to lab", "brand"),
  FULFILLED: s("Fulfilled", "ok"),
};

export const LAB_FLAG: Record<string, StatusStyle> = {
  NORMAL: s("Normal", "ok"),
  LOW: s("Low", "warn"),
  HIGH: s("High", "warn"),
  CRITICALLY_LOW: s("Critically low", "danger"),
  CRITICALLY_HIGH: s("Critically high", "danger"),
  ABNORMAL: s("Abnormal", "warn"),
};

export const LAB_PRIORITY: Record<string, StatusStyle> = {
  ROUTINE: s("Routine", "neutral"),
  URGENT: s("Urgent", "warn"),
  STAT: s("STAT", "danger"),
};

export const RESULT_STATUS: Record<string, StatusStyle> = {
  PRELIMINARY: s("Preliminary", "info"),
  FINAL: s("Final", "ok"),
  CORRECTED: s("Corrected", "warn"),
  AMENDED: s("Amended", "warn"),
};

/** AI drug-interaction severity (ai-service Severity enum). */
export const SEVERITY: Record<string, StatusStyle> = {
  NONE: s("No interaction", "ok"),
  MINOR: s("Minor", "info"),
  MODERATE: s("Moderate", "warn"),
  MAJOR: s("Major", "danger"),
  CONTRAINDICATED: s("Contraindicated", "danger"),
};

/** AI clinical urgency (lab explanations, triage). */
export const URGENCY: Record<string, StatusStyle> = {
  ROUTINE: s("Routine", "ok"),
  MONITOR: s("Monitor", "info"),
  SOON: s("See doctor soon", "warn"),
  URGENT: s("Urgent", "danger"),
  EMERGENCY: s("Emergency", "danger"),
  CRITICAL: s("Critical", "danger"),
};

export const FALLBACK_STATUS: StatusStyle = s("Unknown", "neutral");

export function statusOf(
  map: Record<string, StatusStyle>,
  value: string | null | undefined,
): StatusStyle {
  if (!value) return FALLBACK_STATUS;
  return map[value] ?? { label: value.replaceAll("_", " ").toLowerCase(), tone: "neutral" };
}
