package com.meditrack.labrotary_service.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.application.dto.LabOrderResponse;
import com.meditrack.labrotary_service.application.mapper.LabOrderMapper;
import com.meditrack.labrotary_service.application.usecase.CreateLabOrderUseCase;
import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import com.meditrack.labrotary_service.infrastructure.messaging.event.EventTopics;
import com.meditrack.labrotary_service.infrastructure.messaging.event.LabOrderEvent;
import com.meditrack.labrotary_service.infrastructure.outbox.OutboxEvent;
import com.meditrack.labrotary_service.infrastructure.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Creates a lab order and writes a corresponding outbox event in the same transaction.
 *
 * The {@link com.meditrack.labrotary_service.infrastructure.outbox.OutboxRelay} picks up
 * PENDING outbox events and publishes them to Kafka, guaranteeing at-least-once delivery
 * even if Kafka is temporarily unavailable at order-creation time.
 */
@Slf4j
@Service
public class LabOrderApplicationService implements CreateLabOrderUseCase {

    private final LabOrderRepository labOrderRepository;
    private final LabOrderMapper labOrderMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Counter labOrdersCreatedTotal;

    public LabOrderApplicationService(LabOrderRepository labOrderRepository,
                                      LabOrderMapper labOrderMapper,
                                      OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper,
                                      MeterRegistry meterRegistry) {
        this.labOrderRepository = labOrderRepository;
        this.labOrderMapper = labOrderMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.labOrdersCreatedTotal = Counter.builder("meditrack_lab_orders_created_total")
                .description("Total number of lab orders created")
                .register(meterRegistry);
    }

    @Transactional
    @Override
    public LabOrderResponse createLabOrder(LabOrderRequest request) {
        LabOrder labOrder = labOrderMapper.toDomain(request);
        labOrder.initialize();

        LabOrder savedOrder = labOrderRepository.save(labOrder);
        labOrdersCreatedTotal.increment();
        log.info("Lab order created [orderId={}]", savedOrder.getId());

        // Write event to outbox within the same transaction — Kafka publish happens async via OutboxRelay
        writeToOutbox(savedOrder);

        return new LabOrderResponse(savedOrder.getId());
    }

    private void writeToOutbox(LabOrder order) {
        LabOrderEvent event = new LabOrderEvent(
                order.getId(),
                order.getPatientId(),
                EventTopics.EVENT_TYPE_LAB_ORDER_CREATED);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // Should never happen for a simple POJO; if it does we want the transaction to roll back
            throw new RuntimeException("Failed to serialize LabOrderEvent [orderId=" + order.getId() + "]", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setTopic(EventTopics.LAB_ORDER_CREATED);
        outboxEvent.setAggregateId(order.getId().toString());
        outboxEvent.setEventType(EventTopics.EVENT_TYPE_LAB_ORDER_CREATED);
        outboxEvent.setPayload(payload);

        outboxEventRepository.save(outboxEvent);
        log.debug("Outbox event written [outboxId={}, orderId={}]", outboxEvent.getId(), order.getId());
    }
}
