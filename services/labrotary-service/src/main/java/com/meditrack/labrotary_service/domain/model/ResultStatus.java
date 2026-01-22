package com.meditrack.labrotary_service.domain.model;

/**
 * Result Status enumeration
 *
 * Tracks the lifecycle of a lab result:
 * - PRELIMINARY: Initial result entered, not verified
 * - FINAL: Result verified and finalized
 * - CORRECTED: Result corrected after initial report
 * - AMENDED: Result amended with additional information
 */
public enum ResultStatus {
    PRELIMINARY,  // Initial result, pending verification
    FINAL,        // Verified and released result
    CORRECTED,    // Corrected result
    AMENDED       // Amended result with additional info
}
