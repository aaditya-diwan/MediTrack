package com.meditrack.prescription.application.exception;

import java.util.UUID;

public class PrescriptionNotFoundException extends RuntimeException {
    public PrescriptionNotFoundException(UUID id) {
        super("Prescription not found with id: " + id);
    }
}
