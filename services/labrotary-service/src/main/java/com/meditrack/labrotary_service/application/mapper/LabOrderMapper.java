package com.meditrack.labrotary_service.application.mapper;

import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.domain.model.LabOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LabOrderMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LabOrder toDomain(LabOrderRequest request);
}
