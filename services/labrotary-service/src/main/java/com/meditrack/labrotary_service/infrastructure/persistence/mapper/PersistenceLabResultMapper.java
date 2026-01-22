package com.meditrack.labrotary_service.infrastructure.persistence.mapper;

import com.meditrack.labrotary_service.domain.model.LabResult;
import com.meditrack.labrotary_service.infrastructure.persistence.entity.LabResultEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between LabResult domain model and LabResultEntity
 */
@Component
public class PersistenceLabResultMapper {

    public LabResultEntity toEntity(LabResult labResult) {
        if (labResult == null) {
            return null;
        }

        return LabResultEntity.builder()
            .id(labResult.getId())
            .orderId(labResult.getOrderId())
            .testCode(labResult.getTestCode())
            .testName(labResult.getTestName())
            .loincCode(labResult.getLoincCode())
            .resultValue(labResult.getResultValue())
            .resultUnit(labResult.getResultUnit())
            .referenceRange(labResult.getReferenceRange())
            .abnormalFlag(labResult.getAbnormalFlag())
            .performedBy(labResult.getPerformedBy())
            .performedAt(labResult.getPerformedAt())
            .verifiedBy(labResult.getVerifiedBy())
            .verifiedAt(labResult.getVerifiedAt())
            .status(labResult.getStatus())
            .notes(labResult.getNotes())
            .createdAt(labResult.getCreatedAt())
            .updatedAt(labResult.getUpdatedAt())
            .build();
    }

    public LabResult toDomain(LabResultEntity entity) {
        if (entity == null) {
            return null;
        }

        return LabResult.builder()
            .id(entity.getId())
            .orderId(entity.getOrderId())
            .testCode(entity.getTestCode())
            .testName(entity.getTestName())
            .loincCode(entity.getLoincCode())
            .resultValue(entity.getResultValue())
            .resultUnit(entity.getResultUnit())
            .referenceRange(entity.getReferenceRange())
            .abnormalFlag(entity.getAbnormalFlag())
            .performedBy(entity.getPerformedBy())
            .performedAt(entity.getPerformedAt())
            .verifiedBy(entity.getVerifiedBy())
            .verifiedAt(entity.getVerifiedAt())
            .status(entity.getStatus())
            .notes(entity.getNotes())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
