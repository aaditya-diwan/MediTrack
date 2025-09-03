package com.meditrack.labrotary_service.infrastructure.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class DiagnosisCodeEntity {
    private String system;
    private String code;
    private String description;
}
