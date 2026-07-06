package com.meditrack.labrotary_service.domain.repository;

import com.meditrack.labrotary_service.domain.model.LabOrder;

import java.util.Optional;
import java.util.UUID;

public interface LabOrderRepository {
    LabOrder save(LabOrder labOrder);
    Optional<LabOrder> findById(UUID id);

    /**
     * Returns true when an order already exists for the given external reference
     * (e.g. source prescriptionId) containing a test with the given test code.
     * Used to make event consumption idempotent.
     */
    boolean existsByExternalReferenceAndTestCode(String externalReference, String testCode);
}
