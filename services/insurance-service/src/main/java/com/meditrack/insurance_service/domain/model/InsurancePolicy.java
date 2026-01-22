package com.meditrack.insurance_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Insurance Policy domain model (Aggregate Root)
 *
 * Represents a patient's insurance coverage with a specific payer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePolicy {
    private UUID policyId;
    private UUID patientId;
    private String policyNumber;
    private String payerId;
    private String payerName;
    private String planName;
    private String groupNumber;
    private String subscriberId;
    private String subscriberName;
    private Relationship relationship;
    private LocalDate effectiveDate;
    private LocalDate terminationDate;
    private boolean active;
    private BigDecimal copayAmount;
    private BigDecimal deductibleAmount;
    private BigDecimal deductibleMet;
    private BigDecimal outOfPocketMax;
    private BigDecimal outOfPocketMet;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public void initialize() {
        this.policyId = UUID.randomUUID();
        this.active = true;
        this.deductibleMet = BigDecimal.ZERO;
        this.outOfPocketMet = BigDecimal.ZERO;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isActiveOn(LocalDate date) {
        if (!active) {
            return false;
        }
        if (date.isBefore(effectiveDate)) {
            return false;
        }
        return terminationDate == null || !date.isAfter(terminationDate);
    }

    public boolean isCurrentlyActive() {
        return isActiveOn(LocalDate.now());
    }
}
