package com.meditrack.patient.interfaces.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateMedicalRecordRequest {
    private UUID patientId;
    private String diagnosis;
    private String treatment;
    private LocalDate date;
}
