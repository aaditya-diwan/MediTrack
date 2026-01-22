package com.meditrack.labrotary_service.domain.repository;

import com.meditrack.labrotary_service.domain.model.LabResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Lab Results
 * Domain layer - defines the contract
 */
public interface LabResultRepository {

    /**
     * Save a lab result
     */
    LabResult save(LabResult labResult);

    /**
     * Find a result by ID
     */
    Optional<LabResult> findById(UUID id);

    /**
     * Find all results for a specific order
     */
    List<LabResult> findByOrderId(UUID orderId);

    /**
     * Find all critical results
     */
    List<LabResult> findCriticalResults();

    /**
     * Check if results exist for an order
     */
    boolean existsByOrderId(UUID orderId);
}
