package com.meditrack.prescription.domain.model;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PrescriptionLabOrder {
    private UUID id;
    private String testCode;
    private String testName;
    private String clinicalIndication;
    private LabTestUrgency urgency;
}
