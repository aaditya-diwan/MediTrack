package com.meditrack.labrotary_service.domain.repository;

import com.meditrack.labrotary_service.domain.model.LabOrder;

import java.util.Optional;
import java.util.UUID;

public interface LabOrderRepository {
    LabOrder save(LabOrder labOrder);
    Optional<LabOrder> findById(UUID id);
}
