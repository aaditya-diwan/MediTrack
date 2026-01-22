package com.meditrack.labrotary_service.domain.model;

/**
 * Abnormal Flag enumeration for lab results
 *
 * Standard HL7 flags for indicating result status:
 * - N: Normal
 * - L: Low (below reference range)
 * - H: High (above reference range)
 * - LL: Critically Low
 * - HH: Critically High
 * - A: Abnormal (general)
 */
public enum AbnormalFlag {
    NORMAL,              // N - Within normal range
    LOW,                 // L - Below reference range
    HIGH,                // H - Above reference range
    CRITICALLY_LOW,      // LL - Critically low, immediate attention
    CRITICALLY_HIGH,     // HH - Critically high, immediate attention
    ABNORMAL             // A - Abnormal, unspecified
}
