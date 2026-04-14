package com.meditrack.labrotary_service.infrastructure.messaging.event;

/**
 * Central registry of Kafka topic names used by the lab service.
 * Use these constants everywhere instead of inline strings.
 */
public final class EventTopics {

    private EventTopics() {}

    // Topics produced by this service
    public static final String LAB_ORDER_CREATED = "lab.test.ordered.v1";
    public static final String LAB_RESULTS_AVAILABLE = "lab.results.available.v1";
    public static final String LAB_CRITICAL_RESULT = "lab.critical.result.v1";

    // Event type discriminator values (carried inside the event payload)
    public static final String EVENT_TYPE_LAB_ORDER_CREATED = "lab.test.ordered.v1";
    public static final String EVENT_TYPE_LAB_RESULTS_AVAILABLE = "lab.results.available.v1";
    public static final String EVENT_TYPE_LAB_CRITICAL_RESULT = "lab.critical.result.v1";

    // Topics consumed from other services
    public static final String PATIENT_EVENTS_CONSUMER_TYPE = "lab.test.ordered.v1";
}
