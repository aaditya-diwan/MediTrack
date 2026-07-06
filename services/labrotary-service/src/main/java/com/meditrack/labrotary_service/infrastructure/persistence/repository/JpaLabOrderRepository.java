package com.meditrack.labrotary_service.infrastructure.persistence.repository;

import com.meditrack.labrotary_service.infrastructure.persistence.entity.LabOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaLabOrderRepository extends JpaRepository<LabOrderEntity, UUID> {

    boolean existsByExternalReferenceAndTests_TestCode(String externalReference, String testCode);
}
