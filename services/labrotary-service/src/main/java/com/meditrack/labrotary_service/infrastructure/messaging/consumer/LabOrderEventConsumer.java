package com.meditrack.labrotary_service.infrastructure.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.labrotary_service.application.dto.DiagnosisCodeDto;
import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.application.dto.TestInfoDto;
import com.meditrack.labrotary_service.application.service.LabOrderApplicationService;
import com.meditrack.labrotary_service.domain.model.Priority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Kafka consumer that listens to patient lab order events
 * and creates corresponding lab orders in the laboratory service.
 *
 * This implements the event-driven communication pattern between
 * Patient Service and Laboratory Service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LabOrderEventConsumer {

    private final LabOrderApplicationService labOrderApplicationService;
    private final ObjectMapper objectMapper;

    /**
     * Consumes lab test ordered events from patient service
     * Topic: patient-events (configured in application.yml)
     */
    @KafkaListener(
        topics = "${spring.kafka.topics.patient-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePatientLabOrderEvent(String eventJson, Acknowledgment acknowledgment) {
        try {
            log.info("Received patient lab order event: {}", eventJson);

            // Parse the event
            LabTestOrderedEvent event = objectMapper.readValue(eventJson, LabTestOrderedEvent.class);

            // Only process lab.test.ordered.v1 events
            if ("lab.test.ordered.v1".equals(event.getEventType())) {
                processLabOrderEvent(event);

                // Manually acknowledge successful processing
                acknowledgment.acknowledge();
                log.info("Successfully processed lab order event: orderId={}, patientId={}",
                    event.getOrder().getOrderId(), event.getOrder().getPatientId());
            } else {
                log.debug("Ignoring event type: {}", event.getEventType());
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Error processing lab order event: {}", e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed or sent to DLQ based on config
            throw new RuntimeException("Failed to process lab order event", e);
        }
    }

    /**
     * Processes the lab order event and creates a lab order
     */
    private void processLabOrderEvent(LabTestOrderedEvent event) {
        LabTestOrderedEvent.Order orderData = event.getOrder();
        LabTestOrderedEvent.PatientSnapshot patient = event.getPatientSnapshot();

        // Convert event to lab order request
        LabOrderRequest request = new LabOrderRequest();
        request.setPatientId(orderData.getPatientId());
        request.setOrderingPhysicianId(orderData.getDoctorId());
        request.setOrderTimestamp(OffsetDateTime.now());
        request.setPriority(mapPriority(orderData.getPriority()));

        // Set facility ID (could be derived from event source or configured)
        request.setFacilityId(event.getSource());

        // Convert test code to test info
        TestInfoDto testInfo = new TestInfoDto();
        testInfo.setTestCode(orderData.getTestCode());
        testInfo.setNotes(orderData.getNotes());
        request.setTests(Collections.singletonList(testInfo));

        // For now, we don't have diagnosis codes in the event
        // In a real system, these would be included
        request.setDiagnosisCodes(Collections.emptyList());

        // Create the lab order
        log.info("Creating lab order from event: patientId={}, testCode={}, priority={}",
            orderData.getPatientId(), orderData.getTestCode(), orderData.getPriority());

        labOrderApplicationService.createLabOrder(request);

        log.info("Lab order created successfully from patient event");
    }

    /**
     * Maps string priority from event to domain Priority enum
     */
    private Priority mapPriority(String priority) {
        if (priority == null) {
            return Priority.ROUTINE;
        }

        try {
            return Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown priority '{}', defaulting to ROUTINE", priority);
            return Priority.ROUTINE;
        }
    }

    // ==========================================
    // Event DTOs - Mirror patient service events
    // ==========================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabTestOrderedEvent {
        private UUID eventId;
        private String eventType;
        private long timestamp;
        private String source;
        private Order order;
        private PatientSnapshot patientSnapshot;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Order {
            private UUID orderId;
            private String patientId;
            private String doctorId;
            private String testCode;
            private String priority;
            private String notes;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PatientSnapshot {
            private String mrn;
            private String firstName;
            private String lastName;
            private String dateOfBirth;
        }
    }
}
