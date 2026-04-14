package com.meditrack.labrotary_service.application.exception;

import java.util.UUID;

public class LabOrderNotFoundException extends LabServiceException {
    public LabOrderNotFoundException(UUID orderId) {
        super("Lab order not found: " + orderId);
    }
}
