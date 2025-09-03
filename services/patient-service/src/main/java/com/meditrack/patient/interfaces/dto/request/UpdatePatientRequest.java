package com.meditrack.patient.interfaces.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdatePatientRequest {
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
}
