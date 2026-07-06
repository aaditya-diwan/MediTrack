package com.meditrack.labrotary_service.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event consumed from prescription-service when a prescription containing lab orders
 * is sent to the lab. Mirrors the payload published on the
 * {@code prescription.sent_to_lab.v1} topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrescriptionSentToLabEvent {

    private UUID prescriptionId;
    private UUID patientId;
    private UUID doctorId;
    private List<LabItem> labOrders;
    private Instant occurredAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LabItem {
        private String testCode;
        private String testName;
        private String clinicalIndication;
        private String urgency;
    }
}
