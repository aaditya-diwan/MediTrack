package com.meditrack.patient.infrastructure.messaging;

/**
 * Central registry of Kafka topic names published by the patient service.
 * Use these constants everywhere instead of inline strings.
 */
public final class EventTopics {

    private EventTopics() {}

    public static final String PATIENT_CREATED  = "patient.created.v1";
    public static final String PATIENT_UPDATED  = "patient.updated.v1";
    public static final String PATIENT_DELETED  = "patient.deleted.v1";

    /** Topic consumed by lab-service to create lab orders from patient-service lab test requests. */
    public static final String PATIENT_EVENTS   = "patient-events";

    public static final String EVENT_TYPE_LAB_TEST_ORDERED = "lab.test.ordered.v1";
}
