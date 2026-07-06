package com.meditrack.prescription.interfaces.rest;

import com.meditrack.prescription.application.exception.PrescriptionNotFoundException;
import com.meditrack.prescription.application.exception.PrescriptionNotIssuedException;
import com.meditrack.prescription.application.exception.PrescriptionSafetyRejectedException;
import com.meditrack.prescription.domain.port.SafetyScreenResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PrescriptionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(PrescriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(PrescriptionNotIssuedException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(PrescriptionNotIssuedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(PrescriptionSafetyRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleSafetyRejected(PrescriptionSafetyRejectedException ex) {
        SafetyScreenResult screen = ex.getScreenResult();
        List<Map<String, Object>> findings = screen.findings().stream()
                .map(f -> Map.<String, Object>of(
                        "type", f.type() == null ? "UNKNOWN" : f.type(),
                        "severity", f.severity() == null ? "UNKNOWN" : f.severity(),
                        "description", f.description() == null ? "" : f.description()))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("severity", screen.highestSeverity());
        body.put("summary", screen.summary());
        body.put("requiresPharmacistReview", screen.requiresPharmacistReview());
        body.put("findings", findings);
        body.put("overrideAllowed", true);
        body.put("overrideHint", "Re-issue with body {\"override\": true, \"overrideReason\": \"...\"} to proceed despite the findings.");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "timestamp", Instant.now().toString()));
    }
}
