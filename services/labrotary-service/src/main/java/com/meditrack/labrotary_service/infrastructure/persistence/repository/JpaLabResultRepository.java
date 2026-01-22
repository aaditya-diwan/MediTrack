package com.meditrack.labrotary_service.infrastructure.persistence.repository;

import com.meditrack.labrotary_service.domain.model.AbnormalFlag;
import com.meditrack.labrotary_service.infrastructure.persistence.entity.LabResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for lab results
 */
@Repository
public interface JpaLabResultRepository extends JpaRepository<LabResultEntity, UUID> {

    /**
     * Find all results for a specific order
     */
    List<LabResultEntity> findByOrderId(UUID orderId);

    /**
     * Find all critical results
     */
    @Query("SELECT r FROM LabResultEntity r WHERE r.abnormalFlag IN ('CRITICALLY_LOW', 'CRITICALLY_HIGH')")
    List<LabResultEntity> findCriticalResults();

    /**
     * Check if results exist for an order
     */
    boolean existsByOrderId(UUID orderId);
}
