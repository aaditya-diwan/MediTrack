package com.meditrack.labrotary_service.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.application.dto.LabOrderResponse;
import com.meditrack.labrotary_service.application.mapper.LabOrderMapper;
import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.model.OrderStatus;
import com.meditrack.labrotary_service.domain.model.Priority;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import com.meditrack.labrotary_service.infrastructure.outbox.OutboxEvent;
import com.meditrack.labrotary_service.infrastructure.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabOrderApplicationServiceTest {

    @Mock
    private LabOrderRepository labOrderRepository;

    @Mock
    private LabOrderMapper labOrderMapper;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private MeterRegistry meterRegistry;
    private LabOrderApplicationService service;

    private LabOrder savedOrder;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new LabOrderApplicationService(
                labOrderRepository, labOrderMapper, outboxEventRepository,
                new ObjectMapper(), meterRegistry);

        savedOrder = LabOrder.builder()
                .id(UUID.randomUUID())
                .patientId("patient-123")
                .facilityId("facility-1")
                .orderingPhysicianId("doctor-1")
                .priority(Priority.ROUTINE)
                .status(OrderStatus.RECEIVED)
                .tests(List.of())
                .build();
    }

    @Test
    void createLabOrder_savesOrderAndWritesOutboxEvent() {
        LabOrderRequest request = new LabOrderRequest();
        request.setPatientId("patient-123");
        request.setFacilityId("facility-1");
        request.setOrderingPhysicianId("doctor-1");
        request.setPriority(Priority.ROUTINE);
        request.setTests(List.of());

        LabOrder domainOrder = LabOrder.builder()
                .patientId("patient-123")
                .facilityId("facility-1")
                .orderingPhysicianId("doctor-1")
                .priority(Priority.ROUTINE)
                .tests(List.of())
                .build();

        when(labOrderMapper.toDomain(request)).thenReturn(domainOrder);
        when(labOrderRepository.save(any(LabOrder.class))).thenReturn(savedOrder);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));

        LabOrderResponse response = service.createLabOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(savedOrder.getId());

        // Verify order was saved
        verify(labOrderRepository).save(any(LabOrder.class));

        // Verify outbox event was written with correct content
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEvent outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.getId()).isNotNull();
        assertThat(outboxEvent.getAggregateId()).isEqualTo(savedOrder.getId().toString());
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(outboxEvent.getPayload()).contains(savedOrder.getPatientId());
    }

    @Test
    void createLabOrder_initializesOrderBeforeSaving() {
        LabOrderRequest request = new LabOrderRequest();
        request.setPatientId("patient-123");
        request.setFacilityId("facility-1");
        request.setOrderingPhysicianId("doctor-1");
        request.setPriority(Priority.STAT);
        request.setTests(List.of());

        LabOrder uninitializedOrder = new LabOrder();
        uninitializedOrder.setPatientId("patient-123");
        uninitializedOrder.setTests(List.of());

        when(labOrderMapper.toDomain(request)).thenReturn(uninitializedOrder);
        when(labOrderRepository.save(any(LabOrder.class))).thenReturn(savedOrder);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));

        service.createLabOrder(request);

        // Verify initialize() was called (sets id, status, createdAt)
        ArgumentCaptor<LabOrder> orderCaptor = ArgumentCaptor.forClass(LabOrder.class);
        verify(labOrderRepository).save(orderCaptor.capture());

        LabOrder capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getId()).isNotNull();
        assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(capturedOrder.getCreatedAt()).isNotNull();
    }

    @Test
    void createLabOrder_incrementsMetricCounter() {
        LabOrderRequest request = new LabOrderRequest();
        request.setPatientId("patient-123");
        request.setFacilityId("facility-1");
        request.setOrderingPhysicianId("doctor-1");
        request.setPriority(Priority.ROUTINE);
        request.setTests(List.of());

        when(labOrderMapper.toDomain(any())).thenReturn(new LabOrder());
        when(labOrderRepository.save(any())).thenReturn(savedOrder);
        when(outboxEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createLabOrder(request);
        service.createLabOrder(request);

        double count = meterRegistry.counter("meditrack_lab_orders_created_total").count();
        assertThat(count).isEqualTo(2.0);
    }
}
