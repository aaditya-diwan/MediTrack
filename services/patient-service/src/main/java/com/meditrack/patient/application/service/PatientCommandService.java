package com.meditrack.patient.application.service;

import com.meditrack.patient.application.mapper.ApplicationPatientMapper;
import com.meditrack.patient.application.usecase.CreatePatientUseCase;
import com.meditrack.patient.application.usecase.UpdatePatientUseCase;
import com.meditrack.patient.domain.model.ContactInfo;
import com.meditrack.patient.domain.model.Insurance;
import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.domain.repository.PatientRepository;
import com.meditrack.patient.interfaces.dto.request.CreatePatientRequest;
import com.meditrack.patient.interfaces.dto.request.UpdatePatientRequest;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import com.meditrack.patient.application.exception.PatientNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientCommandService {

    private final PatientRepository patientRepository;

    public PatientResponse createPatient(CreatePatientRequest request) {
        Patient patient = new Patient();
        patient.setId(PatientId.generate());
        patient.setMrn(new MRN(request.getMrn()));
        patient.setSsn(new SSN(request.getSsn()));
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());

        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmail(request.getEmail());
        contactInfo.setPhoneNumber(request.getPhoneNumber());
        contactInfo.setAddress(request.getAddress());
        patient.setContactInfo(contactInfo);

        Insurance insurance = new Insurance();
        insurance.setProvider(request.getInsuranceProvider());
        insurance.setPolicyNumber(request.getInsurancePolicyNumber());
        patient.setInsurance(insurance);

        Patient savedPatient = patientRepository.save(patient);
        return ApplicationPatientMapper.toResponse(savedPatient);
    }

    @CachePut(value = "patients", key = "#patientId")
    public PatientResponse updatePatient(UUID patientId, UpdatePatientRequest request) {
        Patient patient = patientRepository.findById(new PatientId(patientId))
                .orElseThrow(() -> new PatientNotFoundException("Patient not found"));

        // Update fields
        patient.setMrn(new MRN(request.getMrn()));
        patient.setSsn(new SSN(request.getSsn()));
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.getContactInfo().setEmail(request.getEmail());
        patient.getContactInfo().setPhoneNumber(request.getPhoneNumber());
        patient.getContactInfo().setAddress(request.getAddress());
        patient.getInsurance().setProvider(request.getInsuranceProvider());
        patient.getInsurance().setPolicyNumber(request.getInsurancePolicyNumber());

        Patient updatedPatient = patientRepository.save(patient);
        return ApplicationPatientMapper.toResponse(updatedPatient);
    }
}