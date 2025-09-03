package com.meditrack.labrotary_service.application.usecase;

import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.application.dto.LabOrderResponse;

public interface CreateLabOrderUseCase {
    LabOrderResponse createLabOrder(LabOrderRequest request);
}
