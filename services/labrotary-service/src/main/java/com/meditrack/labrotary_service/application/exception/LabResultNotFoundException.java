package com.meditrack.labrotary_service.application.exception;

import java.util.UUID;

public class LabResultNotFoundException extends LabServiceException {
    public LabResultNotFoundException(UUID resultId) {
        super("Lab result not found: " + resultId);
    }
}
