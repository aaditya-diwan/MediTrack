package com.meditrack.labrotary_service.application.mapper;

import com.meditrack.labrotary_service.application.dto.LabResultRequest;
import com.meditrack.labrotary_service.application.dto.LabResultResponse;
import com.meditrack.labrotary_service.domain.model.LabResult;
import org.springframework.stereotype.Component;

/**
 * Mapper between LabResult domain model and DTOs
 */
@Component
public class LabResultMapper {

    public LabResult toDomain(LabResultRequest request) {
        if (request == null) {
            return null;
        }

        return LabResult.builder()
            .orderId(request.getOrderId())
            .testCode(request.getTestCode())
            .testName(request.getTestName())
            .loincCode(request.getLoincCode())
            .resultValue(request.getResultValue())
            .resultUnit(request.getResultUnit())
            .referenceRange(request.getReferenceRange())
            .abnormalFlag(request.getAbnormalFlag())
            .performedBy(request.getPerformedBy())
            .performedAt(request.getPerformedAt())
            .notes(request.getNotes())
            .build();
    }

    public LabResultResponse toResponse(LabResult labResult) {
        if (labResult == null) {
            return null;
        }

        return LabResultResponse.builder()
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
            .critical(labResult.isCritical())
            .createdAt(labResult.getCreatedAt())
            .updatedAt(labResult.getUpdatedAt())
            .build();
    }
}
