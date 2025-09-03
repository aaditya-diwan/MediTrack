package com.meditrack.patient.application.mapper;

import com.meditrack.patient.domain.model.MedicalRecord;
import com.meditrack.patient.interfaces.dto.response.MedicalRecordResponse;

public class ApplicationMedicalRecordMapper {

    public static MedicalRecordResponse toResponse(MedicalRecord medicalRecord) {
        if (medicalRecord == null) {
            return null;
        }

        MedicalRecordResponse response = new MedicalRecordResponse();
        response.setRecordId(medicalRecord.getRecordId());
        response.setDiagnosis(medicalRecord.getDiagnosis());
        response.setTreatment(medicalRecord.getTreatment());
        response.setDate(medicalRecord.getDate());
        return response;
    }
}
