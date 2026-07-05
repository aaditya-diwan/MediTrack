package com.meditrack.ai.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a prescription safety screen finds a high-risk interaction or an
 * allergy conflict. Consumed downstream by the pharmacy queue, notifications,
 * and the audit trail. Carries no free-text PHI beyond the clinical summary.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionSafetyFlaggedEvent {
    private UUID prescriptionId;
    private UUID patientId;
    private String overallRisk;
    private int interactionCount;
    private int allergyConflictCount;
    private boolean requiresPharmacistReview;
    private String summary;
    private String modelUsed;
    private Instant occurredAt;
}
