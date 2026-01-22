package com.meditrack.labrotary_service.infrastructure.messaging;

import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.model.LabResult;
import com.meditrack.labrotary_service.infrastructure.messaging.event.LabResultsAvailableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Publisher for lab result events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LabResultEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topics.lab-events}")
    private String labEventsTopic;

    /**
     * Publish lab results available event
     */
    public void publishLabResultsAvailable(LabOrder order, List<LabResult> results) {
        try {
            // Check if any results are critical
            boolean hasCritical = results.stream()
                .anyMatch(LabResult::isCritical);

            // Build the event
            LabResultsAvailableEvent event = LabResultsAvailableEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("lab.results.available.v1")
                .timestamp(Instant.now().toEpochMilli())
                .source("lab-service")
                .order(buildOrderInfo(order))
                .results(buildResultInfoList(results))
                .hasCriticalResults(hasCritical)
                .build();

            // Publish to Kafka
            kafkaTemplate.send(labEventsTopic, order.getPatientId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published lab results available event: orderId={}, resultCount={}, critical={}",
                            order.getId(), results.size(), hasCritical);
                    } else {
                        log.error("Failed to publish lab results available event: {}", ex.getMessage(), ex);
                    }
                });

        } catch (Exception e) {
            log.error("Error publishing lab results available event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish critical result event (high priority)
     */
    public void publishCriticalResult(LabOrder order, LabResult result) {
        try {
            LabResultsAvailableEvent event = LabResultsAvailableEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("lab.critical.result.v1")
                .timestamp(Instant.now().toEpochMilli())
                .source("lab-service")
                .order(buildOrderInfo(order))
                .results(List.of(buildResultInfo(result)))
                .hasCriticalResults(true)
                .build();

            // Publish with high priority
            kafkaTemplate.send(labEventsTopic, order.getPatientId(), event)
                .whenComplete((result1, ex) -> {
                    if (ex == null) {
                        log.warn("Published CRITICAL lab result event: orderId={}, testCode={}, resultId={}",
                            order.getId(), result.getTestCode(), result.getId());
                    } else {
                        log.error("Failed to publish critical result event: {}", ex.getMessage(), ex);
                    }
                });

        } catch (Exception e) {
            log.error("Error publishing critical result event: {}", e.getMessage(), e);
        }
    }

    private LabResultsAvailableEvent.OrderInfo buildOrderInfo(LabOrder order) {
        return LabResultsAvailableEvent.OrderInfo.builder()
            .orderId(order.getId())
            .patientId(order.getPatientId())
            .orderingPhysicianId(order.getOrderingPhysicianId())
            .facilityId(order.getFacilityId())
            .orderTimestamp(order.getOrderTimestamp())
            .build();
    }

    private List<LabResultsAvailableEvent.ResultInfo> buildResultInfoList(List<LabResult> results) {
        return results.stream()
            .map(this::buildResultInfo)
            .collect(Collectors.toList());
    }

    private LabResultsAvailableEvent.ResultInfo buildResultInfo(LabResult result) {
        return LabResultsAvailableEvent.ResultInfo.builder()
            .resultId(result.getId())
            .testCode(result.getTestCode())
            .testName(result.getTestName())
            .loincCode(result.getLoincCode())
            .resultValue(result.getResultValue())
            .resultUnit(result.getResultUnit())
            .referenceRange(result.getReferenceRange())
            .abnormalFlag(result.getAbnormalFlag())
            .status(result.getStatus())
            .performedAt(result.getPerformedAt())
            .verifiedAt(result.getVerifiedAt())
            .critical(result.isCritical())
            .build();
    }
}
