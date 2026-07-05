package com.meditrack.ai.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.ai.application.exception.ClinicalReasoningException;
import com.meditrack.ai.domain.model.AllergyConflict;
import com.meditrack.ai.domain.model.DrugInteraction;
import com.meditrack.ai.domain.model.Medication;
import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.domain.model.SafetyCheckCommand;
import com.meditrack.ai.domain.model.Severity;
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

    private final RestClient tensorxRestClient;
    private final TensorXProperties props;
    private final ObjectMapper objectMapper;

    @Override
    public SafetyAssessment assess(SafetyCheckCommand command) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new ClinicalReasoningException(
                    "TensorX API key is not configured. Set the TENSORX_API_KEY environment variable.");
        }

        TensorXApi.ChatRequest request = new TensorXApi.ChatRequest(
                props.model(),
                List.of(
                        new TensorXApi.Message("system", SYSTEM_PROMPT),
                        new TensorXApi.Message("user", buildUserPrompt(command))
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

        String content = extractContent(response);
        return toAssessment(parse(content));
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
}
