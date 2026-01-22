package com.meditrack.labrotary_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lab Result domain model
 *
 * Represents a single test result within a lab order.
 * Each test in an order will have its own result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResult {
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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public void initialize() {
        this.id = UUID.randomUUID();
        this.status = ResultStatus.PRELIMINARY;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void verify(String verifier) {
        this.verifiedBy = verifier;
        this.verifiedAt = OffsetDateTime.now();
        this.status = ResultStatus.FINAL;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isCritical() {
        return abnormalFlag == AbnormalFlag.CRITICALLY_LOW ||
               abnormalFlag == AbnormalFlag.CRITICALLY_HIGH;
    }
}
