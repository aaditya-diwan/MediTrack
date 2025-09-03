package com.meditrack.patient.infrastructure.persistence.mapper;

import com.meditrack.patient.domain.model.*;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.infrastructure.persistence.entity.ContactInfoEntity;
import com.meditrack.patient.infrastructure.persistence.entity.PatientEntity;

import java.util.stream.Collectors;

public class PatientMapper {

    public static Patient toDomain(PatientEntity entity) {
        if (entity == null) {
            return null;
        }

        Patient domain = new Patient();
        domain.setId(new PatientId(entity.getId()));
        domain.setMrn(new MRN(entity.getMrn()));
        domain.setSsn(new SSN(entity.getSsn()));
        domain.setFirstName(entity.getFirstName());
        domain.setLastName(entity.getLastName());
        domain.setDateOfBirth(entity.getDateOfBirth());

        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmail(entity.getContactInfo().getEmail());
        contactInfo.setPhoneNumber(entity.getContactInfo().getPhoneNumber());
        contactInfo.setAddress(entity.getContactInfo().getAddress());
        domain.setContactInfo(contactInfo);

        Insurance insurance = new Insurance();
        insurance.setProvider(entity.getInsuranceProvider());
        insurance.setPolicyNumber(entity.getInsurancePolicyNumber());
        domain.setInsurance(insurance);

        if (entity.getMedicalHistory() != null) {
            domain.setMedicalHistory(entity.getMedicalHistory().stream()
                    .map(MedicalRecordMapper::toDomain)
                    .collect(Collectors.toList()));
        }

        return domain;
    }

    public static PatientEntity toEntity(Patient domain) {
        if (domain == null) {
            return null;
        }

        PatientEntity entity = new PatientEntity();
        entity.setId(domain.getId().getId());
        entity.setMrn(domain.getMrn().getValue());
        entity.setSsn(domain.getSsn().getValue());
        entity.setFirstName(domain.getFirstName());
        entity.setLastName(domain.getLastName());
        entity.setDateOfBirth(domain.getDateOfBirth());

        ContactInfoEntity contactInfoEntity = new ContactInfoEntity();
        contactInfoEntity.setEmail(domain.getContactInfo().getEmail());
        contactInfoEntity.setPhoneNumber(domain.getContactInfo().getPhoneNumber());
        contactInfoEntity.setAddress(domain.getContactInfo().getAddress());
        entity.setContactInfo(contactInfoEntity);

        entity.setInsuranceProvider(domain.getInsurance().getProvider());
        entity.setInsurancePolicyNumber(domain.getInsurance().getPolicyNumber());

        if (domain.getMedicalHistory() != null) {
            entity.setMedicalHistory(domain.getMedicalHistory().stream()
                    .map(record -> MedicalRecordMapper.toEntity(record, entity))
                    .collect(Collectors.toList()));
        }

        return entity;
    }
}
