package com.meditrack.patient.infrastructure.messaging;

import com.meditrack.patient.infrastructure.messaging.event.PatientCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PatientEventProducer producer;

    private final String patientId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        producer = new PatientEventProducer(kafkaTemplate);
    }

    private PatientCreatedEvent event() {
        return PatientCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EventTopics.EVENT_TYPE_PATIENT_CREATED)
                .timestamp(Instant.now().toEpochMilli())
                .source("patient-service")
                .patient(PatientCreatedEvent.PatientData.builder()
                        .patientId(patientId)
                        .mrn("MRN123")
                        .firstName("John")
                        .lastName("Doe")
                        .dateOfBirth("1990-01-01")
                        .createdAt(Instant.now())
                        .build())
                .build();
    }

    @Test
    void publishesPatientCreatedToPatientEventsTopicKeyedByPatientId() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        PatientCreatedEvent event = event();
        producer.publishPatientCreated(event);

        verify(kafkaTemplate).send(eq(EventTopics.PATIENT_EVENTS), eq(patientId), eq(event));
    }

    @Test
    void brokerFailureIsNonFatal() {
        // A broker outage surfaces as a synchronous exception from send(); it must be swallowed
        // so patient registration is never blocked or failed by Kafka being down.
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("broker unreachable"));

        assertThatCode(() -> producer.publishPatientCreated(event())).doesNotThrowAnyException();
    }
}
