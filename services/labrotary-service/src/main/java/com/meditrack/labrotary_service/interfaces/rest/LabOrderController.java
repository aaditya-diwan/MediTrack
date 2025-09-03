package com.meditrack.labrotary_service.interfaces.rest;

import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.application.dto.LabOrderResponse;
import com.meditrack.labrotary_service.application.usecase.CreateLabOrderUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lab/orders")
@RequiredArgsConstructor
public class LabOrderController {

    private final CreateLabOrderUseCase createLabOrderUseCase;

    @PostMapping
    public ResponseEntity<LabOrderResponse> createLabOrder(@Valid @RequestBody LabOrderRequest request) {
        LabOrderResponse response = createLabOrderUseCase.createLabOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
