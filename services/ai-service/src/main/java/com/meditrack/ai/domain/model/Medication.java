package com.meditrack.ai.domain.model;

/** A medication being proposed on the prescription under review. */
public record Medication(String name, String dosage, String route) {
}
