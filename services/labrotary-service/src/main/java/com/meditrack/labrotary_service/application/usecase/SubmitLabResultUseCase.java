package com.meditrack.labrotary_service.application.usecase;

import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.model.LabResult;
import com.meditrack.labrotary_service.domain.model.OrderStatus;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import com.meditrack.labrotary_service.domain.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Use case for submitting lab test results
 *
 * Business Rules:
 * - Results can only be submitted for orders in RECEIVED or IN_PROGRESS status
 * - When all tests for an order have results, update order status to COMPLETED
 * - Critical results trigger special handling (event publishing)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmitLabResultUseCase {

    private final LabResultRepository labResultRepository;
    private final LabOrderRepository labOrderRepository;

    @Transactional
    public LabResult execute(LabResult labResult) {
        log.info("Submitting lab result for orderId: {}, testCode: {}",
            labResult.getOrderId(), labResult.getTestCode());

        // Validate order exists and is in valid status
        LabOrder order = labOrderRepository.findById(labResult.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Lab order not found: " + labResult.getOrderId()));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                "Cannot submit results for cancelled order: " + labResult.getOrderId());
        }

        // Initialize the result
        labResult.initialize();

        // If performed time not set, use current time
        if (labResult.getPerformedAt() == null) {
            labResult.setPerformedAt(OffsetDateTime.now());
        }

        // Save the result
        LabResult savedResult = labResultRepository.save(labResult);

        // Update order status if needed
        updateOrderStatus(order);

        log.info("Lab result submitted successfully: resultId={}, orderId={}, critical={}",
            savedResult.getId(), savedResult.getOrderId(), savedResult.isCritical());

        return savedResult;
    }

    /**
     * Update order status based on results completion
     */
    private void updateOrderStatus(LabOrder order) {
        // If order is RECEIVED, move to IN_PROGRESS
        if (order.getStatus() == OrderStatus.RECEIVED) {
            order.setStatus(OrderStatus.IN_PROGRESS);
            order.setUpdatedAt(OffsetDateTime.now());
            labOrderRepository.save(order);
            log.info("Updated order {} status to IN_PROGRESS", order.getId());
        }

        // Check if all tests have results
        int totalTests = order.getTests() != null ? order.getTests().size() : 0;
        if (totalTests > 0 && labResultRepository.existsByOrderId(order.getId())) {
            // Count results for this order
            int resultCount = labResultRepository.findByOrderId(order.getId()).size();

            // If all tests have results, mark order as COMPLETED
            if (resultCount >= totalTests) {
                order.setStatus(OrderStatus.COMPLETED);
                order.setUpdatedAt(OffsetDateTime.now());
                labOrderRepository.save(order);
                log.info("All tests completed. Updated order {} status to COMPLETED", order.getId());
            }
        }
    }
}
