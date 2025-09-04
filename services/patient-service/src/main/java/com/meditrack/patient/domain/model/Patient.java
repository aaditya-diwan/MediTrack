package com.meditrack.patient.domain.model;

import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Patient {
    private PatientId id;
    private MRN mrn;
    private SSN ssn;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private ContactInfo contactInfo;
    private Insurance insurance;
    private List<MedicalRecord> medicalHistory;
}
