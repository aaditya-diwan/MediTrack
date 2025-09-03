package com.meditrack.patient.interfaces.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateMedicalRecordRequest {
    private String diagnosis;
    private String treatment;
    private LocalDate date;
}
