package com.meditrack.patient.interfaces.dto.response;

import java.io.Serializable;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PatientResponse implements Serializable {
    private String id;
    private String mrn;
    private String ssn;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String email;
    private String phoneNumber;
    private String address;
    private String insuranceProvider;
    private String insurancePolicyNumber;
    private List<MedicalRecordResponse> medicalHistory;
}
