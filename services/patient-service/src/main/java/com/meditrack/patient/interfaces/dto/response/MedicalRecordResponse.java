package com.meditrack.patient.interfaces.dto.response;

import java.io.Serializable;
import lombok.Data;
import java.time.LocalDate;

@Data
public class MedicalRecordResponse implements Serializable {
    private String recordId;
    private String diagnosis;
    private String treatment;
    private LocalDate date;
}
