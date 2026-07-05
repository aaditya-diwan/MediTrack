package com.meditrack.patient.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.patient.events.LabTestOrderedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabOrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private LabOrderEventPublisher publisher;
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        publisher = new LabOrderEventPublisher(kafkaTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(publisher, "topic", "patient-events");
    }

    private LabTestOrderedEvent event() {
        return LabTestOrderedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("lab.test.ordered.v1")
                .source("patient-service")
                .order(LabTestOrderedEvent.Order.builder()
                        .orderId(orderId)
                        .patientId(UUID.randomUUID().toString())
                        .testCode("CBC")
                        .build())
                .build();
    }

    @Test
    void publishesToConfiguredTopicKeyedByOrderId() {
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        publisher.publishLabTestOrder(event());

        verify(kafkaTemplate).send(eq("patient-events"), eq(orderId.toString()), anyString());
    }

    @Test
    void brokerFailureIsNonFatal() {
        // A broker outage surfaces as a synchronous exception from send(); it must be swallowed
        // so the lab-order request is never blocked or failed by Kafka being down.
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("broker unreachable"));

        assertThatCode(() -> publisher.publishLabTestOrder(event())).doesNotThrowAnyException();
    }
}
