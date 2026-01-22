package com.meditrack.labrotary_service.infrastructure.persistence.impl;

import com.meditrack.labrotary_service.domain.model.LabResult;
import com.meditrack.labrotary_service.domain.repository.LabResultRepository;
import com.meditrack.labrotary_service.infrastructure.persistence.entity.LabResultEntity;
import com.meditrack.labrotary_service.infrastructure.persistence.mapper.PersistenceLabResultMapper;
import com.meditrack.labrotary_service.infrastructure.persistence.repository.JpaLabResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of LabResultRepository using JPA
 */
@Repository
@RequiredArgsConstructor
public class LabResultRepositoryImpl implements LabResultRepository {

    private final JpaLabResultRepository jpaRepository;
    private final PersistenceLabResultMapper mapper;

    @Override
    public LabResult save(LabResult labResult) {
        LabResultEntity entity = mapper.toEntity(labResult);
        LabResultEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<LabResult> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public List<LabResult> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<LabResult> findCriticalResults() {
        return jpaRepository.findCriticalResults().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }
}
