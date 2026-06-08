package com.meditrack.prescription.application.exception;

public class PrescriptionNotIssuedException extends RuntimeException {
    public PrescriptionNotIssuedException(String message) {
        super(message);
    }
}
