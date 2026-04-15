package com.meditrack.insurance_service.application.dto;

import com.meditrack.insurance_service.domain.model.Relationship;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PolicyResponse {

    UUID policyId;
    UUID patientId;
    String policyNumber;
    String payerId;
    String payerName;
    String planName;
    String groupNumber;
    String subscriberId;
    String subscriberName;
    Relationship relationship;
    LocalDate effectiveDate;
    LocalDate terminationDate;
    boolean active;
    BigDecimal copayAmount;
    BigDecimal deductibleAmount;
    BigDecimal deductibleMet;
    BigDecimal outOfPocketMax;
    BigDecimal outOfPocketMet;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
