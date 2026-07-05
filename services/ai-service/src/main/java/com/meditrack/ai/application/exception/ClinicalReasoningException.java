package com.meditrack.ai.application.exception;

/**
 * Raised when the clinical reasoning engine cannot produce a usable assessment
 * (misconfiguration, upstream inference failure, or an unparseable response).
 * Surfaced to the client as 502 Bad Gateway — the request was valid, but the
 * downstream AI dependency failed.
 */
public class ClinicalReasoningException extends RuntimeException {

    public ClinicalReasoningException(String message) {
        super(message);
    }

    public ClinicalReasoningException(String message, Throwable cause) {
        super(message, cause);
    }
}
