package com.meditrack.labrotary_service.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for the transactional outbox table.
 *
 * Written within the same transaction as the domain aggregate, then published
 * to Kafka by {@link OutboxRelay}.
 */
@Data
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    /** Kafka topic the event should be published to. */
    @Column(nullable = false)
    private String topic;

    /** Kafka message key (typically the aggregate/entity ID). */
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    /** Logical event type discriminator (e.g. "lab.test.ordered.v1"). */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** JSON-serialized event payload. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "error_message")
    private String errorMessage;

    public enum OutboxStatus {
        PENDING, PROCESSED, FAILED
    }
}
