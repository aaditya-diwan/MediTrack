package com.meditrack.patient.application.exception;

public class MedicalRecordNotFoundException extends ResourceNotFoundException {
    public MedicalRecordNotFoundException(String message) {
        super(message);
    }
}
