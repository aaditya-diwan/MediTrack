package com.meditrack.insurance_service.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "insurance_policies")
public class InsurancePolicyEntity {

    @Id
    @Column(name = "policy_id")
    private UUID policyId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    @Column(name = "payer_id", nullable = false)
    private String payerId;

    @Column(name = "payer_name", nullable = false)
    private String payerName;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "group_number")
    private String groupNumber;

    @Column(name = "subscriber_id", nullable = false)
    private String subscriberId;

    @Column(name = "subscriber_name")
    private String subscriberName;

    @Column(name = "relationship", nullable = false)
    private String relationship;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "copay_amount", precision = 10, scale = 2)
    private BigDecimal copayAmount;

    @Column(name = "deductible_amount", precision = 10, scale = 2)
    private BigDecimal deductibleAmount;

    @Column(name = "deductible_met", precision = 10, scale = 2)
    private BigDecimal deductibleMet;

    @Column(name = "out_of_pocket_max", precision = 10, scale = 2)
    private BigDecimal outOfPocketMax;

    @Column(name = "out_of_pocket_met", precision = 10, scale = 2)
    private BigDecimal outOfPocketMet;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;
}
