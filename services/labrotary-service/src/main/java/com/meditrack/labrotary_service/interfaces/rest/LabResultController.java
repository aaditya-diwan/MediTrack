package com.meditrack.labrotary_service.interfaces.rest;

import com.meditrack.labrotary_service.application.dto.LabResultRequest;
import com.meditrack.labrotary_service.application.dto.LabResultResponse;
import com.meditrack.labrotary_service.application.service.LabResultApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for lab results
 *
 * Endpoints:
 * - POST   /api/v1/lab/results - Submit lab result
 * - GET    /api/v1/lab/results/{id} - Get result by ID
 * - GET    /api/v1/lab/results/order/{orderId} - Get results by order
 * - GET    /api/v1/lab/results/critical - Get all critical results
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/lab/results")
@RequiredArgsConstructor
public class LabResultController {

    private final LabResultApplicationService labResultService;

    /**
     * Submit a lab result
     *
     * POST /api/v1/lab/results
     */
    @PostMapping
    public ResponseEntity<LabResultResponse> submitLabResult(
            @Valid @RequestBody LabResultRequest request) {

        log.info("Received lab result submission request for orderId: {}, testCode: {}",
            request.getOrderId(), request.getTestCode());

        LabResultResponse response = labResultService.submitLabResult(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get result by ID
     *
     * GET /api/v1/lab/results/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<LabResultResponse> getResult(@PathVariable UUID id) {
        log.info("Received request to get lab result: {}", id);

        LabResultResponse response = labResultService.getResult(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all results for a specific order
     *
     * GET /api/v1/lab/results/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<LabResultResponse>> getResultsByOrder(
            @PathVariable UUID orderId) {

        log.info("Received request to get results for order: {}", orderId);

        List<LabResultResponse> results = labResultService.getResultsByOrder(orderId);

        return ResponseEntity.ok(results);
    }

    /**
     * Get all critical results
     *
     * GET /api/v1/lab/results/critical
     */
    @GetMapping("/critical")
    public ResponseEntity<List<LabResultResponse>> getCriticalResults() {
        log.info("Received request to get all critical results");

        List<LabResultResponse> results = labResultService.getCriticalResults();

        return ResponseEntity.ok(results);
    }
}
