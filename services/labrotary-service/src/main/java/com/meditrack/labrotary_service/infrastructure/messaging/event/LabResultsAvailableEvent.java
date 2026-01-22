package com.meditrack.labrotary_service.infrastructure.messaging.event;

import com.meditrack.labrotary_service.domain.model.AbnormalFlag;
import com.meditrack.labrotary_service.domain.model.ResultStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when lab results are available
 *
 * Consumers:
 * - Patient Service (updates medical records)
 * - Insurance Service (claims processing)
 * - Notification Service (alerts providers/patients)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultsAvailableEvent {

    private UUID eventId;
    private String eventType;  // "lab.results.available.v1"
    private long timestamp;
    private String source;     // "lab-service"

    private OrderInfo order;
    private List<ResultInfo> results;
    private boolean hasCriticalResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo {
        private UUID orderId;
        private String patientId;
        private String orderingPhysicianId;
        private String facilityId;
        private OffsetDateTime orderTimestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultInfo {
        private UUID resultId;
        private String testCode;
        private String testName;
        private String loincCode;
        private String resultValue;
        private String resultUnit;
        private String referenceRange;
        private AbnormalFlag abnormalFlag;
        private ResultStatus status;
        private OffsetDateTime performedAt;
        private OffsetDateTime verifiedAt;
        private boolean critical;
    }
}
