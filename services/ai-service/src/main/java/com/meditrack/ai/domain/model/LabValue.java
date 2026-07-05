package com.meditrack.ai.domain.model;

/**
 * A single lab measurement to be explained.
 *
 * @param testName       e.g. "Potassium"
 * @param value          measured value as reported, e.g. "6.2"
 * @param unit           e.g. "mmol/L"
 * @param referenceRange normal range as reported, e.g. "3.5-5.1"
 * @param flag           lab flag if any, e.g. "H", "L", "CRITICAL"
 */
public record LabValue(
        String testName,
        String value,
        String unit,
        String referenceRange,
        String flag
) {
}
