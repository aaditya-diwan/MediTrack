package com.meditrack.labrotary_service.infrastructure.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class TestInfoEntity {
    private String testCode;
    private String testName;
    private String specimenType;
    private String clinicalNotes;
}
