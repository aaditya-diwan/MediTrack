package com.meditrack.labrotary_service.application.dto;

import com.meditrack.labrotary_service.domain.model.AbnormalFlag;
import com.meditrack.labrotary_service.domain.model.ResultStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for lab result response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultResponse {
    private UUID id;
    private UUID orderId;
    private String testCode;
    private String testName;
    private String loincCode;
    private String resultValue;
    private String resultUnit;
    private String referenceRange;
    private AbnormalFlag abnormalFlag;
    private String performedBy;
    private OffsetDateTime performedAt;
    private String verifiedBy;
    private OffsetDateTime verifiedAt;
    private ResultStatus status;
    private String notes;
    private boolean critical;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
