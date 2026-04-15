package com.meditrack.insurance_service.domain.model;

/**
 * Relationship of the subscriber to the patient (insured member).
 */
public enum Relationship {
    SELF,
    SPOUSE,
    CHILD,
    PARENT,
    DOMESTIC_PARTNER,
    OTHER
}
