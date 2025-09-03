package com.meditrack.patient.application.mapper;

import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;

import java.util.stream.Collectors;

public class ApplicationPatientMapper {

    public static PatientResponse toResponse(Patient patient) {
        if (patient == null) {
            return null;
        }

        PatientResponse response = new PatientResponse();
        response.setId(patient.getId().getId().toString());
        response.setMrn(patient.getMrn().getValue());
        response.setSsn(patient.getSsn().getValue());
        response.setFirstName(patient.getFirstName());
        response.setLastName(patient.getLastName());
        response.setDateOfBirth(patient.getDateOfBirth());

        if (patient.getContactInfo() != null) {
            response.setEmail(patient.getContactInfo().getEmail());
            response.setPhoneNumber(patient.getContactInfo().getPhoneNumber());
            response.setAddress(patient.getContactInfo().getAddress());
        }

        if (patient.getInsurance() != null) {
            response.setInsuranceProvider(patient.getInsurance().getProvider());
            response.setInsurancePolicyNumber(patient.getInsurance().getPolicyNumber());
        }

        if (patient.getMedicalHistory() != null) {
            response.setMedicalHistory(patient.getMedicalHistory().stream()
                    .map(ApplicationMedicalRecordMapper::toResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }
}
