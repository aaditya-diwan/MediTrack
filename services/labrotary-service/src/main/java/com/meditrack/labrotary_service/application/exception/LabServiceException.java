package com.meditrack.labrotary_service.application.exception;

public class LabServiceException extends RuntimeException {
    public LabServiceException(String message) {
        super(message);
    }

    public LabServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
