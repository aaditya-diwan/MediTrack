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
}
