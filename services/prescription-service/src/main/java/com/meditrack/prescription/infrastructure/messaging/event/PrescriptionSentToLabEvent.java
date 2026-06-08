package com.meditrack.prescription.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionSentToLabEvent {
    private UUID prescriptionId;
    private UUID patientId;
    private UUID doctorId;
    private List<LabItem> labOrders;
    private Instant occurredAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LabItem {
        private String testCode;
        private String testName;
        private String clinicalIndication;
        private String urgency;
    }
}
