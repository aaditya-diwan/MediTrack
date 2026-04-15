package com.meditrack.labrotary_service.infrastructure.outbox;

import java.util.List;

/**
 * Domain-facing port for outbox persistence.
 */
public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPending();
}
