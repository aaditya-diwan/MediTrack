package com.meditrack.labrotary_service.infrastructure.persistence.mapper;

import com.meditrack.labrotary_service.domain.model.DiagnosisCode;
import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.model.OrderStatus;
import com.meditrack.labrotary_service.domain.model.TestInfo;
import com.meditrack.labrotary_service.infrastructure.persistence.entity.LabOrderEntity;
import com.meditrack.labrotary_service.infrastructure.persistence.entity.LabTestEntity;
import com.meditrack.labrotary_service.infrastructure.persistence.entity.OrderDiagnosisEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hand-written mapper between the {@link LabOrder} aggregate and its JPA entities.
 * Replaces the previous MapStruct mapper because the comprehensive schema needs
 * child back-references, String↔UUID conversion, and NOT NULL defaults that are
 * clearer to express explicitly.
 */
@Component
public class PersistenceLabOrderMapper {

    private static final String DEFAULT_CODE_SYSTEM = "ICD-10";

    public LabOrderEntity toEntity(LabOrder domain) {
        if (domain == null) {
            return null;
        }
        LabOrderEntity entity = new LabOrderEntity();
        entity.setId(domain.getId());
        entity.setPatientId(domain.getPatientId() == null ? null : UUID.fromString(domain.getPatientId()));
        entity.setMrn(domain.getMrn());
        entity.setOrderingProviderId(domain.getOrderingPhysicianId());
        entity.setOrderingProviderName(domain.getOrderingProviderName());
        entity.setOrderingFacilityId(domain.getFacilityId());
        entity.setExternalReference(domain.getExternalReference());
        entity.setOrderDate(domain.getOrderTimestamp() != null ? domain.getOrderTimestamp() : OffsetDateTime.now());
        entity.setPriority(domain.getPriority());
        // RECEIVED is not a valid lab_orders status (CHECK constraint) — persist as PENDING.
        entity.setStatus(domain.getStatus() == OrderStatus.RECEIVED ? OrderStatus.PENDING : domain.getStatus());

        if (domain.getTests() != null) {
            List<LabTestEntity> tests = domain.getTests().stream().map(t -> {
                LabTestEntity te = new LabTestEntity();
                te.setOrder(entity);
                te.setTestCode(t.getTestCode());
                te.setTestName(t.getTestName());
                return te;
            }).collect(Collectors.toList());
            entity.setTests(tests);
        }

        if (domain.getDiagnosisCodes() != null) {
            List<OrderDiagnosisEntity> diagnoses = domain.getDiagnosisCodes().stream().map(d -> {
                OrderDiagnosisEntity de = new OrderDiagnosisEntity();
                de.setOrder(entity);
                de.setCode(d.getCode());
                de.setCodeSystem(d.getSystem() != null ? d.getSystem() : DEFAULT_CODE_SYSTEM);
                de.setDescription(d.getDescription());
                return de;
            }).collect(Collectors.toList());
            entity.setDiagnoses(diagnoses);
        }

        return entity;
    }

    public LabOrder toDomain(LabOrderEntity entity) {
        if (entity == null) {
            return null;
        }
        LabOrder domain = LabOrder.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId() == null ? null : entity.getPatientId().toString())
                .mrn(entity.getMrn())
                .orderingPhysicianId(entity.getOrderingProviderId())
                .orderingProviderName(entity.getOrderingProviderName())
                .facilityId(entity.getOrderingFacilityId())
                .externalReference(entity.getExternalReference())
                .orderTimestamp(entity.getOrderDate())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        if (entity.getTests() != null) {
            domain.setTests(entity.getTests().stream()
                    .map(t -> new TestInfo(t.getTestCode(), t.getTestName(), null, null))
                    .collect(Collectors.toList()));
        }
        if (entity.getDiagnoses() != null) {
            domain.setDiagnosisCodes(entity.getDiagnoses().stream()
                    .map(d -> new DiagnosisCode(d.getCodeSystem(), d.getCode(), d.getDescription()))
                    .collect(Collectors.toList()));
        }

        return domain;
    }
}
