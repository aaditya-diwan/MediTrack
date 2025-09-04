package com.meditrack.patient.application.service;

import com.meditrack.patient.application.mapper.ApplicationPatientMapper;
import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.domain.repository.PatientRepository;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;
import com.meditrack.patient.interfaces.dto.response.PatientTimelineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.meditrack.patient.application.exception.PatientNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientQueryService {

    private final PatientRepository patientRepository;

    @Cacheable(value = "patients", key = "#id")
    public PatientResponse getPatientById(UUID id) {
        return patientRepository.findById(new PatientId(id))
                .map(ApplicationPatientMapper::toResponse)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found"));
    }

    @Cacheable(value = "patientSsn", key = "#ssn")
    public Optional<Patient> getPatientBySSN(SSN ssn) {
        return patientRepository.findBySsn(ssn);
    }

    @Cacheable(value = "patient_search", key = "#query")
    public List<PatientResponse> searchPatients(String query) {
        if (query != null) {
            if (query.startsWith("mrn:")) {
                String mrnValue = query.substring(4);
                return patientRepository.findByMrn(new MRN(mrnValue))
                        .map(ApplicationPatientMapper::toResponse)
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            } else if (query.startsWith("ssn:")) {
                String ssnValue = query.substring(4);
                return patientRepository.findBySsn(new SSN(ssnValue))
                        .map(ApplicationPatientMapper::toResponse)
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            } else if (query.startsWith("firstName:")) {
                String firstName = query.substring(10);
                return patientRepository.findByFirstName(firstName).stream()
                        .map(ApplicationPatientMapper::toResponse)
                        .collect(Collectors.toList());
            } else if (query.startsWith("lastName:")) {
                String lastName = query.substring(9);
                return patientRepository.findByLastName(lastName).stream()
                        .map(ApplicationPatientMapper::toResponse)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    public PatientTimelineResponse getPatientTimeline(UUID patientId) {
        // This is a simplified implementation. A real implementation would fetch
        // specific timeline events, not just the whole patient object.
        Patient patient = patientRepository.findById(new PatientId(patientId))
                .orElseThrow(() -> new PatientNotFoundException("Patient not found"));

        PatientTimelineResponse response = new PatientTimelineResponse();
        response.setPatientId(patient.getId().getId().toString());
        response.setTimeline(Collections.emptyList()); // Placeholder
        return response;
    }
}
