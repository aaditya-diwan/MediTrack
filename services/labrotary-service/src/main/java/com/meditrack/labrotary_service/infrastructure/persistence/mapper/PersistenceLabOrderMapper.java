package com.meditrack.labrotary_service.infrastructure.persistence.mapper;

import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.infrastructure.persistence.entity.LabOrderEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PersistenceLabOrderMapper {
    LabOrderEntity toEntity(LabOrder domain);
    LabOrder toDomain(LabOrderEntity entity);
}
