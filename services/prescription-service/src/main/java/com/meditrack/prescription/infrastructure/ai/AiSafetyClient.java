package com.meditrack.prescription.infrastructure.ai;

import com.meditrack.prescription.domain.model.Prescription;
import com.meditrack.prescription.domain.model.PrescriptionMedication;
import com.meditrack.prescription.domain.port.PatientSafetyContext;
import com.meditrack.prescription.domain.port.PrescriptionSafetyPort;
import com.meditrack.prescription.domain.port.SafetyScreenResult;
import com.meditrack.prescription.domain.port.SafetyScreenResult.SafetyFinding;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Adapter for the ai-service drug-safety endpoint
 * ({@code POST /api/v1/ai/prescription-safety}).
 *
 * <p>Authenticates with a short-lived internal service JWT signed with the
 * platform-wide shared {@code jwt.secret} (HS256) — the same scheme the
 * patient-service uses to mint user tokens, so ai-service's JwtRequestFilter
 * accepts it unchanged. The token carries subject "prescription-service" and
 * the ROLE_DOCTOR authority required by the endpoint's @PreAuthorize.
 *
 * <p>Fail-open by contract: any transport, auth or configuration failure is
 * logged and surfaced as {@link SafetyScreenResult#notChecked(String)} —
 * never an exception.
 */
@Slf4j
@Component
public class AiSafetyClient implements PrescriptionSafetyPort {

    private static final String SAFETY_PATH = "/api/v1/ai/prescription-safety";
    private static final String SERVICE_SUBJECT = "prescription-service";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final SecretKey signingKey;

    public AiSafetyClient(
            @Value("${meditrack.ai-service.base-url:http://localhost:8089}") String baseUrl,
            @Value("${jwt.secret:}") String jwtSecret) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        this.signingKey = (jwtSecret == null || jwtSecret.isBlank())
                ? null
                : Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        if (this.signingKey == null) {
            log.warn("jwt.secret is not configured — AI drug-safety screening will be skipped (fail-open)");
        }
    }

    @Override
    public SafetyScreenResult screen(Prescription prescription, PatientSafetyContext context) {
        if (signingKey == null) {
            return SafetyScreenResult.notChecked("JWT secret not configured");
        }
        try {
            SafetyRequest request = toRequest(prescription, context);
            SafetyResponse response = restClient.post()
                    .uri(SAFETY_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mintServiceToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SafetyResponse.class);

            if (response == null) {
                log.warn("ai-service returned an empty safety assessment for prescription {} — failing open",
                        prescription.getId());
                return SafetyScreenResult.notChecked("empty response from ai-service");
            }
            return toResult(response);
        } catch (Exception ex) {
            log.warn("AI drug-safety screen failed for prescription {} — failing open: {}",
                    prescription.getId(), ex.getMessage());
            return SafetyScreenResult.notChecked(ex.getMessage());
        }
    }

    private String mintServiceToken() {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(SERVICE_SUBJECT)
                .claim("roles", List.of("ROLE_DOCTOR"))
                .issuedAt(new Date(now))
                .expiration(new Date(now + TOKEN_TTL.toMillis()))
                .signWith(signingKey)
                .compact();
    }

    private SafetyRequest toRequest(Prescription prescription, PatientSafetyContext context) {
        List<PrescriptionMedication> meds = prescription.getMedications() == null
                ? List.of() : prescription.getMedications();
        List<MedicationInput> proposed = meds.stream()
                .map(m -> new MedicationInput(m.getMedicationName(), m.getDosage(), m.getRoute()))
                .toList();

        // TODO: patientAgeYears / patientSex from patient-service once the
        // PatientSafetyContext enrichment lands.
        return new SafetyRequest(
                proposed,
                context.currentMedications(),
                context.allergies(),
                null,
                null,
                prescription.getId(),
                prescription.getPatientId());
    }

    private SafetyScreenResult toResult(SafetyResponse response) {
        List<SafetyFinding> findings = new ArrayList<>();
        if (response.interactions() != null) {
            for (InteractionView i : response.interactions()) {
                findings.add(new SafetyFinding(
                        "INTERACTION",
                        normalizeSeverity(i.severity()),
                        i.drugA() + " + " + i.drugB() + ": " + nullSafe(i.clinicalConsequence())
                                + (i.management() != null ? " Management: " + i.management() : "")));
            }
        }
        if (response.allergyConflicts() != null) {
            for (AllergyConflictView c : response.allergyConflicts()) {
                findings.add(new SafetyFinding(
                        "ALLERGY_CONFLICT",
                        normalizeSeverity(c.severity()),
                        c.medication() + " conflicts with known allergy '" + c.allergen() + "': "
                                + nullSafe(c.note())));
            }
        }
        return new SafetyScreenResult(
                true,
                normalizeSeverity(response.overallRisk()),
                response.summary(),
                response.requiresPharmacistReview(),
                findings);
    }

    /** Maps ai-service Severity names, treating the legacy "SEVERE" label as MAJOR. */
    private static String normalizeSeverity(String raw) {
        if (raw == null || raw.isBlank()) {
            return "NONE";
        }
        String upper = raw.trim().toUpperCase();
        return "SEVERE".equals(upper) ? "MAJOR" : upper;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    // --- Wire DTOs mirroring ai-service's PrescriptionSafetyRequest / SafetyAssessmentResponse ---

    record MedicationInput(String name, String dosage, String route) {
    }

    record SafetyRequest(
            List<MedicationInput> medications,
            List<String> currentMedications,
            List<String> knownAllergies,
            Integer patientAgeYears,
            String patientSex,
            UUID prescriptionId,
            UUID patientId) {
    }

    record InteractionView(String drugA, String drugB, String severity,
                           String mechanism, String clinicalConsequence, String management) {
    }

    record AllergyConflictView(String medication, String allergen, String severity, String note) {
    }

    record SafetyResponse(
            String overallRisk,
            boolean requiresPharmacistReview,
            String summary,
            String recommendation,
            List<InteractionView> interactions,
            List<AllergyConflictView> allergyConflicts,
            String modelUsed,
            String disclaimer,
            Instant generatedAt) {
    }
}
