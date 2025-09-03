package com.meditrack.patient.domain.model;

import com.meditrack.patient.domain.model.valueobjects.PatientId;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicalRecord {
    private String recordId;
    private PatientId patientId;
    private String diagnosis;
    private String treatment;
    private LocalDate date;
}
