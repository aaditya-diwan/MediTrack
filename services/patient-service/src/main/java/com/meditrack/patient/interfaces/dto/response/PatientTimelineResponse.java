package com.meditrack.patient.interfaces.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class PatientTimelineResponse {
    private String patientId;
    private List<MedicalRecordResponse> timeline;
}
