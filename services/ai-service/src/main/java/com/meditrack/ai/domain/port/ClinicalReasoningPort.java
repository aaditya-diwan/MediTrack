package com.meditrack.ai.domain.port;

import com.meditrack.ai.domain.model.HistorySummaryCommand;
import com.meditrack.ai.domain.model.IcdCodeSuggestionCommand;
import com.meditrack.ai.domain.model.IcdCodeSuggestions;
import com.meditrack.ai.domain.model.LabResultExplanation;
import com.meditrack.ai.domain.model.LabResultExplanationCommand;
import com.meditrack.ai.domain.model.PatientHistorySummary;
import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.domain.model.SafetyCheckCommand;
import com.meditrack.ai.domain.model.SoapNote;
import com.meditrack.ai.domain.model.SoapNoteCommand;
import com.meditrack.ai.domain.model.TriageAssessment;
import com.meditrack.ai.domain.model.TriageCommand;

/**
 * Outbound port for clinical reasoning. The domain depends on this abstraction,
 * not on any particular LLM vendor — the TensorX adapter is one implementation,
 * and it can be swapped for another inference provider (or a rules engine)
 * without touching the application or domain layers.
 */
public interface ClinicalReasoningPort {

    /** Screen a proposed prescription for drug interactions and allergy conflicts. */
    SafetyAssessment assess(SafetyCheckCommand command);

    /** Explain a panel of lab results in clinician- and patient-facing language. */
    LabResultExplanation explainLabResult(LabResultExplanationCommand command);

    /** Triage a symptom presentation into an advisory urgency and specialty. */
    TriageAssessment triage(TriageCommand command);

    /** Structure free-text consultation notes into a SOAP note, without inventing findings. */
    SoapNote generateSoapNote(SoapNoteCommand command);

    /** Suggest ICD-10 codes supported by a clinical note. */
    IcdCodeSuggestions suggestIcdCodes(IcdCodeSuggestionCommand command);

    /** Distil a patient's record into a pre-consultation brief. */
    PatientHistorySummary summarizeHistory(HistorySummaryCommand command);
}
