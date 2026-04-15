package com.meditrack.labrotary_service.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final JpaOutboxEventRepository jpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return jpaRepository.save(event);
    }

    @Override
    public List<OutboxEvent> findPending() {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);
    }
}
