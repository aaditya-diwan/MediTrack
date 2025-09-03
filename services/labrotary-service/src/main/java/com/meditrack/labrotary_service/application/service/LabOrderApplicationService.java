package com.meditrack.labrotary_service.application.service;

import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.application.dto.LabOrderResponse;
import com.meditrack.labrotary_service.application.mapper.LabOrderMapper;
import com.meditrack.labrotary_service.application.usecase.CreateLabOrderUseCase;
import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import com.meditrack.labrotary_service.infrastructure.messaging.LabOrderEventPublisher;
import com.meditrack.labrotary_service.infrastructure.messaging.event.LabOrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabOrderApplicationService implements CreateLabOrderUseCase {

    private final LabOrderRepository labOrderRepository;
    private final LabOrderMapper labOrderMapper;
    private final LabOrderEventPublisher eventPublisher;

    @Transactional
    @Override
    public LabOrderResponse createLabOrder(LabOrderRequest request) {
        LabOrder labOrder = labOrderMapper.toDomain(request);
        labOrder.initialize();

        LabOrder savedOrder = labOrderRepository.save(labOrder);

        LabOrderEvent event = new LabOrderEvent(savedOrder.getId(), savedOrder.getPatientId(), "lab.test.ordered.v1");
        eventPublisher.publishLabOrderCreatedEvent(event);

        return new LabOrderResponse(savedOrder.getId());
    }
}
