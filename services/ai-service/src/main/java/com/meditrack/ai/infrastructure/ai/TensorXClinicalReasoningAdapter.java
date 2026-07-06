package com.meditrack.ai.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.ai.application.exception.ClinicalReasoningException;
import com.meditrack.ai.domain.model.AllergyConflict;
import com.meditrack.ai.domain.model.ClinicalUrgency;
import com.meditrack.ai.domain.model.DrugInteraction;
import com.meditrack.ai.domain.model.HistorySummaryCommand;
import com.meditrack.ai.domain.model.IcdCodeSuggestion;
import com.meditrack.ai.domain.model.IcdCodeSuggestionCommand;
import com.meditrack.ai.domain.model.IcdCodeSuggestions;
import com.meditrack.ai.domain.model.IcdConfidence;
import com.meditrack.ai.domain.model.LabResultDetail;
import com.meditrack.ai.domain.model.LabResultExplanation;
import com.meditrack.ai.domain.model.LabResultExplanationCommand;
import com.meditrack.ai.domain.model.LabValue;
import com.meditrack.ai.domain.model.Medication;
import com.meditrack.ai.domain.model.PatientHistorySummary;
import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.domain.model.SafetyCheckCommand;
import com.meditrack.ai.domain.model.Severity;
import com.meditrack.ai.domain.model.SoapNote;
import com.meditrack.ai.domain.model.SoapNoteCommand;
import com.meditrack.ai.domain.model.TriageAssessment;
import com.meditrack.ai.domain.model.TriageCommand;
import com.meditrack.ai.domain.model.TriageUrgency;
import com.meditrack.ai.domain.model.VisitNote;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/**
 * Clinical reasoning backed by a TensorX open-weight model.
 *
 * <p>The prompt is deliberately conservative: the model is told to escalate on
 * uncertainty, never fabricate findings, and return strict JSON. The response is
 * parsed leniently and every field is defensively defaulted so a malformed model
 * reply degrades to "needs pharmacist review" rather than a silent all-clear.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TensorXClinicalReasoningAdapter implements ClinicalReasoningPort {

    private static final String SYSTEM_PROMPT = """
            You are a clinical decision support assistant for licensed healthcare professionals.
            You screen a proposed prescription for (1) drug-drug interactions among the new and
            current medications, and (2) conflicts with the patient's documented allergies.

            Rules:
            - Use these severity tiers only: NONE, MINOR, MODERATE, MAJOR, CONTRAINDICATED.
            - Be conservative: when uncertain, escalate the severity and recommend pharmacist review
              rather than understate the risk.
            - Never invent medications, allergens, or interactions that are not implied by the input.
            - "overallRisk" must equal the single highest severity across all findings (NONE if there are none).
            - Set "requiresPharmacistReview" to true whenever overallRisk is MAJOR or CONTRAINDICATED,
              or when any allergy conflict is present.
            - Respond with a SINGLE valid JSON object and nothing else. No prose, no markdown code fences.
            """;

    private static final String LAB_SYSTEM_PROMPT = """
            You are a clinical decision support assistant for licensed healthcare professionals.
            You explain a panel of lab results clearly and accurately.

            Rules:
            - Use these urgency tiers only: ROUTINE, MONITOR, URGENT, CRITICAL.
            - "urgency" must equal the single highest urgency across all findings.
            - Be conservative: when uncertain, escalate urgency rather than understate it.
            - Never invent tests, values, or findings that are not implied by the input.
            - "patientFriendlySummary" must be plain language a non-clinician can understand.
            - Respond with a SINGLE valid JSON object and nothing else. No prose, no markdown code fences.
            """;

    private static final String TRIAGE_SYSTEM_PROMPT = """
            You are a conservative clinical triage assistant. You assess a symptom presentation
            and recommend how quickly the patient should be seen and by which specialty.
            You never diagnose — you only prioritise.

            Rules:
            - Use these urgency tiers only: ROUTINE, SOON, URGENT, EMERGENCY.
            - Be conservative: when uncertain, escalate the urgency rather than understate it.
            - Return EMERGENCY whenever any emergency red flag is present or plausible, including:
              chest pain or pressure, signs of stroke (facial droop, arm weakness, slurred speech),
              anaphylaxis or severe allergic reaction, difficulty breathing, severe uncontrolled
              bleeding, sudden severe ("thunderclap") headache, loss of consciousness, seizure,
              suicidal ideation, signs of sepsis, or severe abdominal pain with rigidity.
            - List in "redFlags" every red-flag finding actually implied by the input; do not invent any.
            - Never provide a diagnosis; "rationale" explains the prioritisation only.
            - "selfCareAdvice" may be given only for ROUTINE or SOON presentations; otherwise set it to null.
            - Never invent symptoms, conditions, medications, or allergies not implied by the input.
            - Respond with a SINGLE valid JSON object and nothing else. No prose, no markdown code fences.
            """;

    private static final String SOAP_SYSTEM_PROMPT = """
            You are a clinical documentation assistant for licensed healthcare professionals.
            You restructure a clinician's free-text consultation notes into a SOAP note
            (Subjective, Objective, Assessment, Plan).

            Rules:
            - Use ONLY information present in the input. NEVER invent symptoms, findings,
              vitals, diagnoses, medications, or plans that are not documented.
            - If the input contains nothing for a section, that section must be exactly
              "Not documented." — do not fill gaps with plausible content.
            - "assessmentProblems" lists the individual problems named in the assessment;
              leave it empty when none are documented.
            - "followUp" is the documented follow-up arrangement, or null when none is documented.
            - Preserve the clinician's clinical meaning; you may fix grammar and expand
              unambiguous shorthand, nothing more.
            - Respond with a SINGLE valid JSON object and nothing else. No prose, no markdown code fences.
            """;

    private static final String ICD_SYSTEM_PROMPT = """
            You are a medical coding assistant for licensed healthcare professionals.
            You suggest ICD-10 codes that are supported by a clinical note.

            Rules:
            - Suggest ONLY codes clearly supported by the documented findings and diagnoses.
              Never invent findings, and never suggest a code for a condition merely suspected
              by you rather than documented in the note.
            - Use these confidence tiers only: HIGH, MODERATE, LOW. HIGH requires the note to
              state the condition explicitly; when uncertain, use a LOWER confidence.
            - "rationale" must quote or point to the part of the note that supports the code.
            - Order suggestions best-supported first and return at most 8.
            - Prefer the most specific ICD-10 code the documentation actually supports; do not
              guess extra specificity that is not documented.
            - Respond with a SINGLE valid JSON object and nothing else. No prose, no markdown code fences.
            """;

    private static final String HISTORY_SYSTEM_PROMPT = """
            You are a clinical decision support assistant for licensed healthcare professionals.
            You distil a patient's record into a short pre-consultation brief.

            Rules:
            - Use ONLY the supplied record. NEVER invent conditions, medications, allergies,
              lab results, or events that are not present in the input.
            - Be conservative: surface anything potentially dangerous (critical allergies,
              abnormal or flagged labs, concerning visit notes) rather than omit it.
            - "recentAbnormalFindings" and "redFlags" must each be empty when the record
              contains nothing abnormal or concerning — do not manufacture concerns.
            - "narrativeSummary" is a short clinician-facing paragraph; do not add advice there.
            - "suggestedFollowUps" are advisory next steps grounded in the record.
            - Respond with a SINGLE valid JSON object and nothing else. No prose, no markdown code fences.
            """;

    private final RestClient tensorxRestClient;
    private final TensorXProperties props;
    private final ObjectMapper objectMapper;

    @Override
    public SafetyAssessment assess(SafetyCheckCommand command) {
        String content = requestCompletion(SYSTEM_PROMPT, buildUserPrompt(command));
        return toAssessment(parse(content));
    }

    @Override
    public LabResultExplanation explainLabResult(LabResultExplanationCommand command) {
        String content = requestCompletion(LAB_SYSTEM_PROMPT, buildLabPrompt(command));
        return toExplanation(parseLab(content));
    }

    @Override
    public TriageAssessment triage(TriageCommand command) {
        String content = requestCompletion(TRIAGE_SYSTEM_PROMPT, buildTriagePrompt(command));
        return toTriage(parseTriage(content));
    }

    @Override
    public SoapNote generateSoapNote(SoapNoteCommand command) {
        String content = requestCompletion(SOAP_SYSTEM_PROMPT, buildSoapPrompt(command));
        return toSoapNote(parseSoap(content));
    }

    @Override
    public IcdCodeSuggestions suggestIcdCodes(IcdCodeSuggestionCommand command) {
        String content = requestCompletion(ICD_SYSTEM_PROMPT, buildIcdPrompt(command));
        return toIcdSuggestions(parseIcd(content));
    }

    @Override
    public PatientHistorySummary summarizeHistory(HistorySummaryCommand command) {
        String content = requestCompletion(HISTORY_SYSTEM_PROMPT, buildHistoryPrompt(command));
        return toHistorySummary(parseHistory(content));
    }

    /**
     * Shared TensorX call: validates config, sends a JSON-mode chat completion,
     * and returns the (fence-stripped) content string. Vendor/HTTP concerns live
     * here; each use case only supplies its system + user prompt and parses.
     */
    private String requestCompletion(String systemPrompt, String userPrompt) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new ClinicalReasoningException(
                    "TensorX API key is not configured. Set the TENSORX_API_KEY environment variable.");
        }

        TensorXApi.ChatRequest request = new TensorXApi.ChatRequest(
                props.model(),
                List.of(
                        new TensorXApi.Message("system", systemPrompt),
                        new TensorXApi.Message("user", userPrompt)
                ),
                props.temperature(),
                TensorXApi.ResponseFormat.jsonObject()
        );

        final TensorXApi.ChatResponse response;
        try {
            response = tensorxRestClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TensorXApi.ChatResponse.class);
        } catch (RestClientException ex) {
            throw new ClinicalReasoningException("TensorX inference call failed: " + ex.getMessage(), ex);
        }

        return extractContent(response);
    }

    private String buildUserPrompt(SafetyCheckCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Screen this prescription for the patient described below.\n\n");

        sb.append("Patient:\n");
        sb.append("- age: ").append(cmd.patientAgeYears() != null ? cmd.patientAgeYears() : "unknown").append('\n');
        sb.append("- sex: ").append(orUnknown(cmd.patientSex())).append('\n');
        sb.append("- known allergies: ").append(joinOrNone(cmd.knownAllergies())).append('\n');
        sb.append("- current medications: ").append(joinOrNone(cmd.currentMedications())).append("\n\n");

        sb.append("Newly prescribed medications:\n");
        if (cmd.newMedications() == null || cmd.newMedications().isEmpty()) {
            sb.append("- (none provided)\n");
        } else {
            for (Medication m : cmd.newMedications()) {
                sb.append("- ").append(orUnknown(m.name()));
                if (m.dosage() != null && !m.dosage().isBlank()) {
                    sb.append(" ").append(m.dosage());
                }
                if (m.route() != null && !m.route().isBlank()) {
                    sb.append(" (").append(m.route()).append(")");
                }
                sb.append('\n');
            }
        }

        sb.append("""

                Return JSON exactly in this shape:
                {
                  "overallRisk": "NONE|MINOR|MODERATE|MAJOR|CONTRAINDICATED",
                  "summary": "one short paragraph",
                  "recommendation": "the recommended next action",
                  "requiresPharmacistReview": true,
                  "interactions": [
                    {"drugA": "", "drugB": "", "severity": "", "mechanism": "", "clinicalConsequence": "", "management": ""}
                  ],
                  "allergyConflicts": [
                    {"medication": "", "allergen": "", "severity": "", "note": ""}
                  ]
                }
                """);
        return sb.toString();
    }

    private String buildLabPrompt(LabResultExplanationCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Explain these lab results for the patient described below.\n\n");

        sb.append("Patient:\n");
        sb.append("- age: ").append(cmd.patientAgeYears() != null ? cmd.patientAgeYears() : "unknown").append('\n');
        sb.append("- sex: ").append(orUnknown(cmd.patientSex())).append('\n');
        if (cmd.context() != null && !cmd.context().isBlank()) {
            sb.append("- context: ").append(cmd.context()).append('\n');
        }

        sb.append("\nResults:\n");
        if (cmd.results() == null || cmd.results().isEmpty()) {
            sb.append("- (none provided)\n");
        } else {
            for (LabValue v : cmd.results()) {
                sb.append("- ").append(orUnknown(v.testName())).append(": ")
                        .append(orUnknown(v.value()));
                if (v.unit() != null && !v.unit().isBlank()) {
                    sb.append(' ').append(v.unit());
                }
                if (v.referenceRange() != null && !v.referenceRange().isBlank()) {
                    sb.append(" (ref ").append(v.referenceRange()).append(')');
                }
                if (v.flag() != null && !v.flag().isBlank()) {
                    sb.append(" flag=").append(v.flag());
                }
                sb.append('\n');
            }
        }

        sb.append("""

                Return JSON exactly in this shape:
                {
                  "overallSummary": "clinician-facing summary of the panel",
                  "patientFriendlySummary": "the same in plain language for a patient",
                  "suggestedFollowUp": "the recommended next step",
                  "urgency": "ROUTINE|MONITOR|URGENT|CRITICAL",
                  "results": [
                    {"testName": "", "interpretation": "", "explanation": "", "clinicalSignificance": ""}
                  ]
                }
                """);
        return sb.toString();
    }

    private String buildTriagePrompt(TriageCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Triage the symptom presentation for the patient described below.\n\n");

        sb.append("Patient:\n");
        sb.append("- age: ").append(cmd.patientAgeYears() != null ? cmd.patientAgeYears() : "unknown").append('\n');
        sb.append("- sex: ").append(orUnknown(cmd.patientSex())).append('\n');
        sb.append("- known conditions: ").append(joinOrNone(cmd.knownConditions())).append('\n');
        sb.append("- current medications: ").append(joinOrNone(cmd.currentMedications())).append('\n');
        sb.append("- known allergies: ").append(joinOrNone(cmd.knownAllergies())).append("\n\n");

        sb.append("Presentation:\n");
        sb.append("- symptoms: ").append(orUnknown(cmd.symptoms())).append('\n');
        sb.append("- duration: ").append(orUnknown(cmd.duration())).append('\n');

        sb.append("""

                Return JSON exactly in this shape:
                {
                  "urgency": "ROUTINE|SOON|URGENT|EMERGENCY",
                  "recommendedSpecialty": "the specialty best placed to see the patient",
                  "redFlags": ["red-flag finding present in the input"],
                  "rationale": "why this urgency was chosen (no diagnosis)",
                  "selfCareAdvice": "self-care guidance, or null unless ROUTINE or SOON"
                }
                """);
        return sb.toString();
    }

    private String buildSoapPrompt(SoapNoteCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Structure the consultation notes below into a SOAP note. ");
        sb.append("Use only what is documented; sections without documented content must read \"Not documented.\"\n\n");

        sb.append("Patient:\n");
        sb.append("- age: ").append(cmd.patientAgeYears() != null ? cmd.patientAgeYears() : "unknown").append('\n');
        sb.append("- sex: ").append(orUnknown(cmd.patientSex())).append('\n');
        sb.append("- known conditions: ").append(joinOrNone(cmd.knownConditions())).append('\n');

        sb.append("\nVitals:\n");
        if (cmd.vitals() == null || cmd.vitals().isEmpty()) {
            sb.append("- (none provided)\n");
        } else {
            cmd.vitals().forEach((name, value) ->
                    sb.append("- ").append(name).append(": ").append(orUnknown(value)).append('\n'));
        }

        sb.append("\nConsultation notes:\n");
        sb.append(orUnknown(cmd.consultationNotes())).append('\n');

        sb.append("""

                Return JSON exactly in this shape:
                {
                  "subjective": "what the patient reported, or \\"Not documented.\\"",
                  "objective": "examination findings and vitals, or \\"Not documented.\\"",
                  "assessment": "the clinician's assessment, or \\"Not documented.\\"",
                  "plan": "the management plan, or \\"Not documented.\\"",
                  "assessmentProblems": ["problem named in the assessment"],
                  "followUp": "documented follow-up arrangement, or null"
                }
                """);
        return sb.toString();
    }

    private String buildIcdPrompt(IcdCodeSuggestionCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Suggest ICD-10 codes supported by the clinical note below.\n\n");

        sb.append("Clinical note:\n");
        sb.append(orUnknown(cmd.clinicalNotes())).append('\n');

        if (cmd.existingDiagnosis() != null && !cmd.existingDiagnosis().isBlank()) {
            sb.append("\nClinician's existing diagnosis:\n");
            sb.append(cmd.existingDiagnosis()).append('\n');
        }

        sb.append("""

                Return JSON exactly in this shape (at most 8 suggestions, best-supported first):
                {
                  "suggestions": [
                    {"code": "", "description": "", "confidence": "HIGH|MODERATE|LOW", "rationale": ""}
                  ]
                }
                """);
        return sb.toString();
    }

    private String buildHistoryPrompt(HistorySummaryCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summarise the patient record below into a pre-consultation brief.\n\n");

        sb.append("Patient:\n");
        sb.append("- age: ").append(cmd.patientAgeYears() != null ? cmd.patientAgeYears() : "unknown").append('\n');
        sb.append("- sex: ").append(orUnknown(cmd.patientSex())).append('\n');
        sb.append("- conditions: ").append(joinOrNone(cmd.conditions())).append('\n');
        sb.append("- medications: ").append(joinOrNone(cmd.medications())).append('\n');
        sb.append("- allergies: ").append(joinOrNone(cmd.allergies())).append('\n');

        sb.append("\nRecent lab results:\n");
        if (cmd.recentLabResults() == null || cmd.recentLabResults().isEmpty()) {
            sb.append("- (none provided)\n");
        } else {
            for (LabValue v : cmd.recentLabResults()) {
                sb.append("- ").append(orUnknown(v.testName())).append(": ").append(orUnknown(v.value()));
                if (v.flag() != null && !v.flag().isBlank()) {
                    sb.append(" flag=").append(v.flag());
                }
                sb.append('\n');
            }
        }

        sb.append("\nPast visit notes:\n");
        if (cmd.pastVisits() == null || cmd.pastVisits().isEmpty()) {
            sb.append("- (none provided)\n");
        } else {
            for (VisitNote visit : cmd.pastVisits()) {
                sb.append("- ").append(orUnknown(visit.date())).append(": ")
                        .append(orUnknown(visit.note())).append('\n');
            }
        }

        sb.append("""

                Return JSON exactly in this shape:
                {
                  "keyConditions": ["condition that matters most right now"],
                  "activeMedications": ["current medication"],
                  "criticalAllergies": ["allergy a prescriber must not miss"],
                  "recentAbnormalFindings": ["abnormal lab or finding from the supplied data"],
                  "redFlags": ["anything needing attention before the visit"],
                  "narrativeSummary": "short clinician-facing paragraph",
                  "suggestedFollowUps": ["advisory next step grounded in the record"]
                }
                """);
        return sb.toString();
    }

    private String extractContent(TensorXApi.ChatResponse response) {
        String content = Optional.ofNullable(response)
                .map(TensorXApi.ChatResponse::choices)
                .filter(c -> !c.isEmpty())
                .map(c -> c.get(0))
                .map(TensorXApi.Choice::message)
                .map(TensorXApi.Message::content)
                .orElseThrow(() -> new ClinicalReasoningException("TensorX returned an empty response"));
        return stripFences(content);
    }

    /** Models occasionally wrap JSON in ```json fences despite instructions — strip them. */
    private String stripFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.trim();
    }

    private AiPayload parse(String json) {
        try {
            return objectMapper.readValue(json, AiPayload.class);
        } catch (Exception ex) {
            log.warn("Could not parse TensorX response as JSON: {}", ex.getMessage());
            throw new ClinicalReasoningException("TensorX returned an unparseable assessment", ex);
        }
    }

    private SafetyAssessment toAssessment(AiPayload p) {
        List<DrugInteraction> interactions = Optional.ofNullable(p.interactions()).orElse(List.of())
                .stream()
                .map(i -> new DrugInteraction(i.drugA(), i.drugB(), Severity.fromString(i.severity()),
                        i.mechanism(), i.clinicalConsequence(), i.management()))
                .toList();

        List<AllergyConflict> conflicts = Optional.ofNullable(p.allergyConflicts()).orElse(List.of())
                .stream()
                .map(a -> new AllergyConflict(a.medication(), a.allergen(),
                        Severity.fromString(a.severity()), a.note()))
                .toList();

        Severity overall = Severity.fromString(p.overallRisk());
        // Defensive: an allergy conflict always warrants review regardless of what the model said.
        boolean requiresReview = p.requiresPharmacistReview() != null
                ? p.requiresPharmacistReview()
                : (overall.isHigh() || !conflicts.isEmpty());

        return new SafetyAssessment(
                overall,
                Optional.ofNullable(p.summary()).orElse(""),
                Optional.ofNullable(p.recommendation()).orElse(""),
                requiresReview || overall.isHigh() || !conflicts.isEmpty(),
                interactions,
                conflicts,
                props.model()
        );
    }

    private AiLabPayload parseLab(String json) {
        try {
            return objectMapper.readValue(json, AiLabPayload.class);
        } catch (Exception ex) {
            log.warn("Could not parse TensorX lab response as JSON: {}", ex.getMessage());
            throw new ClinicalReasoningException("TensorX returned an unparseable lab explanation", ex);
        }
    }

    private LabResultExplanation toExplanation(AiLabPayload p) {
        List<LabResultDetail> details = Optional.ofNullable(p.results()).orElse(List.of())
                .stream()
                .map(d -> new LabResultDetail(d.testName(), d.interpretation(),
                        d.explanation(), d.clinicalSignificance()))
                .toList();

        return new LabResultExplanation(
                Optional.ofNullable(p.overallSummary()).orElse(""),
                Optional.ofNullable(p.patientFriendlySummary()).orElse(""),
                Optional.ofNullable(p.suggestedFollowUp()).orElse(""),
                ClinicalUrgency.fromString(p.urgency()),
                details,
                props.model()
        );
    }

    private AiTriagePayload parseTriage(String json) {
        try {
            return objectMapper.readValue(json, AiTriagePayload.class);
        } catch (Exception ex) {
            log.warn("Could not parse TensorX triage response as JSON: {}", ex.getMessage());
            throw new ClinicalReasoningException("TensorX returned an unparseable triage assessment", ex);
        }
    }

    private TriageAssessment toTriage(AiTriagePayload p) {
        TriageUrgency urgency = TriageUrgency.fromString(p.urgency());
        List<String> redFlags = Optional.ofNullable(p.redFlags()).orElse(List.of());

        // Defensive: self-care advice is never appropriate alongside an urgent/emergency triage.
        String selfCare = (urgency == TriageUrgency.ROUTINE || urgency == TriageUrgency.SOON)
                ? blankToNull(p.selfCareAdvice())
                : null;

        return new TriageAssessment(
                urgency,
                Optional.ofNullable(p.recommendedSpecialty()).orElse(""),
                redFlags,
                Optional.ofNullable(p.rationale()).orElse(""),
                selfCare,
                props.model()
        );
    }

    private AiSoapPayload parseSoap(String json) {
        try {
            return objectMapper.readValue(json, AiSoapPayload.class);
        } catch (Exception ex) {
            log.warn("Could not parse TensorX SOAP response as JSON: {}", ex.getMessage());
            throw new ClinicalReasoningException("TensorX returned an unparseable SOAP note", ex);
        }
    }

    private SoapNote toSoapNote(AiSoapPayload p) {
        return new SoapNote(
                orNotDocumented(p.subjective()),
                orNotDocumented(p.objective()),
                orNotDocumented(p.assessment()),
                orNotDocumented(p.plan()),
                Optional.ofNullable(p.assessmentProblems()).orElse(List.of()),
                blankToNull(p.followUp()),
                props.model()
        );
    }

    private AiIcdPayload parseIcd(String json) {
        try {
            return objectMapper.readValue(json, AiIcdPayload.class);
        } catch (Exception ex) {
            log.warn("Could not parse TensorX ICD response as JSON: {}", ex.getMessage());
            throw new ClinicalReasoningException("TensorX returned unparseable ICD-10 suggestions", ex);
        }
    }

    private IcdCodeSuggestions toIcdSuggestions(AiIcdPayload p) {
        List<IcdCodeSuggestion> suggestions = Optional.ofNullable(p.suggestions()).orElse(List.of())
                .stream()
                .map(s -> new IcdCodeSuggestion(
                        Optional.ofNullable(s.code()).orElse(""),
                        Optional.ofNullable(s.description()).orElse(""),
                        IcdConfidence.fromString(s.confidence()),
                        Optional.ofNullable(s.rationale()).orElse("")))
                .toList();

        return new IcdCodeSuggestions(suggestions, props.model());
    }

    private AiHistoryPayload parseHistory(String json) {
        try {
            return objectMapper.readValue(json, AiHistoryPayload.class);
        } catch (Exception ex) {
            log.warn("Could not parse TensorX history response as JSON: {}", ex.getMessage());
            throw new ClinicalReasoningException("TensorX returned an unparseable history summary", ex);
        }
    }

    private PatientHistorySummary toHistorySummary(AiHistoryPayload p) {
        return new PatientHistorySummary(
                Optional.ofNullable(p.keyConditions()).orElse(List.of()),
                Optional.ofNullable(p.activeMedications()).orElse(List.of()),
                Optional.ofNullable(p.criticalAllergies()).orElse(List.of()),
                Optional.ofNullable(p.recentAbnormalFindings()).orElse(List.of()),
                Optional.ofNullable(p.redFlags()).orElse(List.of()),
                Optional.ofNullable(p.narrativeSummary()).orElse(""),
                Optional.ofNullable(p.suggestedFollowUps()).orElse(List.of()),
                props.model()
        );
    }

    private static String orNotDocumented(String s) {
        return (s == null || s.isBlank()) ? "Not documented." : s;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String trimmed = s.trim();
        // JSON-mode models sometimes emit the literal string "null" for absent fields.
        return "null".equalsIgnoreCase(trimmed) ? null : s;
    }

    private static String orUnknown(String s) {
        return (s == null || s.isBlank()) ? "unknown" : s;
    }

    private static String joinOrNone(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "none reported";
        }
        return String.join(", ", items);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiPayload(
            String overallRisk,
            String summary,
            String recommendation,
            Boolean requiresPharmacistReview,
            List<AiInteraction> interactions,
            List<AiAllergy> allergyConflicts
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiInteraction(String drugA, String drugB, String severity,
                         String mechanism, String clinicalConsequence, String management) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiAllergy(String medication, String allergen, String severity, String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiLabPayload(
            String overallSummary,
            String patientFriendlySummary,
            String suggestedFollowUp,
            String urgency,
            List<AiLabDetail> results
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiLabDetail(String testName, String interpretation, String explanation, String clinicalSignificance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiTriagePayload(
            String urgency,
            String recommendedSpecialty,
            List<String> redFlags,
            String rationale,
            String selfCareAdvice
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiSoapPayload(
            String subjective,
            String objective,
            String assessment,
            String plan,
            List<String> assessmentProblems,
            String followUp
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiIcdPayload(List<AiIcdSuggestion> suggestions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiIcdSuggestion(String code, String description, String confidence, String rationale) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AiHistoryPayload(
            List<String> keyConditions,
            List<String> activeMedications,
            List<String> criticalAllergies,
            List<String> recentAbnormalFindings,
            List<String> redFlags,
            String narrativeSummary,
            List<String> suggestedFollowUps
    ) {
    }
}
