package com.meditrack.labrotary_service.application.service;

import com.meditrack.labrotary_service.application.dto.LabResultRequest;
import com.meditrack.labrotary_service.application.dto.LabResultResponse;
import com.meditrack.labrotary_service.application.mapper.LabResultMapper;
import com.meditrack.labrotary_service.application.usecase.SubmitLabResultUseCase;
import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.model.LabResult;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import com.meditrack.labrotary_service.domain.repository.LabResultRepository;
import com.meditrack.labrotary_service.infrastructure.messaging.LabResultEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for lab results operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabResultApplicationService {

    private final SubmitLabResultUseCase submitLabResultUseCase;
    private final LabResultRepository labResultRepository;
    private final LabOrderRepository labOrderRepository;
    private final LabResultMapper mapper;
    private final LabResultEventPublisher eventPublisher;

    /**
     * Submit a lab result
     */
    @Transactional
    public LabResultResponse submitLabResult(LabResultRequest request) {
        log.info("Submitting lab result for orderId: {}, testCode: {}",
            request.getOrderId(), request.getTestCode());

        // Convert request to domain model
        LabResult labResult = mapper.toDomain(request);

        // Execute use case
        LabResult savedResult = submitLabResultUseCase.execute(labResult);

        // Get the order for event publishing
        LabOrder order = labOrderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Publish event if critical
        if (savedResult.isCritical()) {
            log.warn("Critical result detected for orderId: {}, testCode: {}",
                request.getOrderId(), request.getTestCode());
            eventPublisher.publishCriticalResult(order, savedResult);
        }

        // Check if all results are complete and publish
        List<LabResult> allResults = labResultRepository.findByOrderId(request.getOrderId());
        if (allResults.size() == order.getTests().size()) {
            log.info("All results complete for orderId: {}, publishing event", order.getId());
            eventPublisher.publishLabResultsAvailable(order, allResults);
        }

        return mapper.toResponse(savedResult);
    }

    /**
     * Get result by ID
     */
    @Cacheable(value = "lab_results", key = "#resultId")
    public LabResultResponse getResult(UUID resultId) {
        log.debug("Getting lab result: {}", resultId);

        LabResult result = labResultRepository.findById(resultId)
            .orElseThrow(() -> new IllegalArgumentException("Result not found: " + resultId));

        return mapper.toResponse(result);
    }

    /**
     * Get all results for an order
     */
    @Cacheable(value = "lab_results", key = "'order:' + #orderId")
    public List<LabResultResponse> getResultsByOrder(UUID orderId) {
        log.debug("Getting lab results for order: {}", orderId);

        List<LabResult> results = labResultRepository.findByOrderId(orderId);

        return results.stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all critical results
     */
    public List<LabResultResponse> getCriticalResults() {
        log.debug("Getting all critical results");

        List<LabResult> results = labResultRepository.findCriticalResults();

        return results.stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }
}
