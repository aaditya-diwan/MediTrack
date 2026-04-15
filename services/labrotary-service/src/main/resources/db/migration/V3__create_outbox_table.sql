-- Transactional Outbox table
-- Lab service writes events here within the same DB transaction as the domain write.
-- The OutboxRelay component polls PENDING rows and publishes them to Kafka,
-- then marks them PROCESSED (or FAILED after max retries).
-- This guarantees at-least-once delivery without a distributed transaction.

CREATE TABLE outbox_events (
    id            UUID         NOT NULL PRIMARY KEY,
    topic         VARCHAR(255) NOT NULL,
    aggregate_id  VARCHAR(255) NOT NULL,
    event_type    VARCHAR(255) NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count   INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at  TIMESTAMP WITH TIME ZONE,
    error_message TEXT,

    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_outbox_status     ON outbox_events(status) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at ASC);
CREATE INDEX idx_outbox_aggregate  ON outbox_events(aggregate_id);
