package com.meditrack.labrotary_service.infrastructure.persistence.impl;

import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import com.meditrack.labrotary_service.infrastructure.persistence.entity.LabOrderEntity;
import com.meditrack.labrotary_service.infrastructure.persistence.mapper.PersistenceLabOrderMapper;
import com.meditrack.labrotary_service.infrastructure.persistence.repository.JpaLabOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LabOrderRepositoryImpl implements LabOrderRepository {

    private final JpaLabOrderRepository jpaRepository;
    private final PersistenceLabOrderMapper mapper;

    @Override
    public LabOrder save(LabOrder labOrder) {
        LabOrderEntity entity = mapper.toEntity(labOrder);
        LabOrderEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<LabOrder> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
