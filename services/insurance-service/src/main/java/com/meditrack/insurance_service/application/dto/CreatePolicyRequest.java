package com.meditrack.insurance_service.application.dto;

import com.meditrack.insurance_service.domain.model.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreatePolicyRequest {

    @NotNull(message = "patientId is required")
    private UUID patientId;

    @NotBlank(message = "policyNumber is required")
    private String policyNumber;

    @NotBlank(message = "payerId is required")
    private String payerId;

    @NotBlank(message = "payerName is required")
    private String payerName;

    private String planName;

    private String groupNumber;

    @NotBlank(message = "subscriberId is required")
    private String subscriberId;

    private String subscriberName;

    @NotNull(message = "relationship is required")
    private Relationship relationship;

    @NotNull(message = "effectiveDate is required")
    private LocalDate effectiveDate;

    private LocalDate terminationDate;

    private BigDecimal copayAmount;
    private BigDecimal deductibleAmount;
    private BigDecimal outOfPocketMax;
}
