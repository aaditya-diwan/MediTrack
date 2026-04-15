package com.meditrack.labrotary_service.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Polls the outbox table for PENDING events and publishes them to Kafka.
 *
 * Runs every 5 seconds. Events that fail after {@value #MAX_RETRIES} attempts
 * are marked FAILED so they can be investigated without blocking the relay.
 *
 * Guarantees:
 * - At-least-once delivery: a message may be published more than once if the
 *   relay crashes between publish and status update. Consumers must be idempotent.
 * - Ordering within a partition: messages are keyed by aggregateId, so events
 *   for the same aggregate land on the same partition in order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:5000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxRepository.findPending();

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Outbox relay: processing {} pending event(s)", pending.size());

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(); // block to confirm send before marking PROCESSED

                event.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
                event.setProcessedAt(OffsetDateTime.now());
                outboxRepository.save(event);

                log.info("Outbox relay: published event [id={}, topic={}, aggregateId={}]",
                        event.getId(), event.getTopic(), event.getAggregateId());

            } catch (Exception e) {
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                event.setErrorMessage(e.getMessage());

                if (retries >= MAX_RETRIES) {
                    event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                    log.error("Outbox relay: marking event FAILED after {} retries [id={}, topic={}]",
                            MAX_RETRIES, event.getId(), event.getTopic(), e);
                } else {
                    log.warn("Outbox relay: publish failed, will retry [id={}, attempt={}/{}]: {}",
                            event.getId(), retries, MAX_RETRIES, e.getMessage());
                }

                outboxRepository.save(event);
            }
        }
    }
}
