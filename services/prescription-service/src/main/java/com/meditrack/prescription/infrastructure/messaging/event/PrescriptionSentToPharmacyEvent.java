package com.meditrack.prescription.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionSentToPharmacyEvent {
    private UUID prescriptionId;
    private UUID patientId;
    private List<MedicationItem> medications;
    private Instant occurredAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MedicationItem {
        private String medicationName;
        private String dosage;
        private String frequency;
        private String duration;
    }
}
