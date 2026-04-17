package com.meditrack.labrotary_service.application.service;

import com.meditrack.labrotary_service.application.dto.LabResultRequest;
import com.meditrack.labrotary_service.application.dto.LabResultResponse;
import com.meditrack.labrotary_service.application.exception.LabOrderNotFoundException;
import com.meditrack.labrotary_service.application.exception.LabResultNotFoundException;
import com.meditrack.labrotary_service.application.mapper.LabResultMapper;
import com.meditrack.labrotary_service.application.usecase.SubmitLabResultUseCase;
import com.meditrack.labrotary_service.domain.model.AbnormalFlag;
import com.meditrack.labrotary_service.domain.model.LabOrder;
import com.meditrack.labrotary_service.domain.model.LabResult;
import com.meditrack.labrotary_service.domain.model.ResultStatus;
import com.meditrack.labrotary_service.domain.model.TestInfo;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import com.meditrack.labrotary_service.domain.repository.LabResultRepository;
import com.meditrack.labrotary_service.infrastructure.messaging.LabResultEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabResultApplicationServiceTest {

    @Mock
    private SubmitLabResultUseCase submitLabResultUseCase;

    @Mock
    private LabResultRepository labResultRepository;

    @Mock
    private LabOrderRepository labOrderRepository;

    @Mock
    private LabResultMapper mapper;

    @Mock
    private LabResultEventPublisher eventPublisher;

    @InjectMocks
    private LabResultApplicationService service;

    private UUID orderId;
    private UUID resultId;
    private LabResult labResult;
    private LabOrder labOrder;
    private LabResultResponse labResultResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        resultId = UUID.randomUUID();

        labResult = LabResult.builder()
                .id(resultId)
                .orderId(orderId)
                .testCode("CBC")
                .testName("Complete Blood Count")
                .resultValue("12.5")
                .resultUnit("g/dL")
                .abnormalFlag(AbnormalFlag.NORMAL)
                .performedBy("lab-tech-1")
                .status(ResultStatus.FINAL)
                .build();

        TestInfo test = new TestInfo();
        test.setTestCode("CBC");

        labOrder = LabOrder.builder()
                .id(orderId)
                .patientId("patient-123")
                .tests(List.of(test))
                .build();

        labResultResponse = LabResultResponse.builder()
                .id(resultId)
                .orderId(orderId)
                .testCode("CBC")
                .testName("Complete Blood Count")
                .resultValue("12.5")
                .status(ResultStatus.FINAL)
                .critical(false)
                .build();
    }

    @Test
    void submitLabResult_normalResult_savesAndReturnsResponse() {
        LabResultRequest request = new LabResultRequest();
        request.setOrderId(orderId);
        request.setTestCode("CBC");

        when(mapper.toDomain(request)).thenReturn(labResult);
        when(submitLabResultUseCase.execute(labResult)).thenReturn(labResult);
        when(labOrderRepository.findById(orderId)).thenReturn(Optional.of(labOrder));
        when(labResultRepository.findByOrderId(orderId)).thenReturn(List.of(labResult));
        when(mapper.toResponse(labResult)).thenReturn(labResultResponse);

        LabResultResponse response = service.submitLabResult(request);

        assertThat(response).isNotNull();
        assertThat(response.getTestCode()).isEqualTo("CBC");
        assertThat(response.isCritical()).isFalse();

        // Critical result event should NOT be published for normal results
        verify(eventPublisher, never()).publishCriticalResult(any(), any());
    }

    @Test
    void submitLabResult_criticalResult_publishesCriticalEvent() {
        LabResult criticalResult = LabResult.builder()
                .id(resultId)
                .orderId(orderId)
                .testCode("K")
                .testName("Potassium")
                .resultValue("7.2")
                .abnormalFlag(AbnormalFlag.CRITICALLY_HIGH)
                .performedBy("lab-tech-1")
                .status(ResultStatus.FINAL)
                .build();

        LabResultRequest request = new LabResultRequest();
        request.setOrderId(orderId);
        request.setTestCode("K");

        LabResultResponse criticalResponse = LabResultResponse.builder()
                .id(resultId)
                .orderId(orderId)
                .testCode("K")
                .critical(true)
                .build();

        when(mapper.toDomain(request)).thenReturn(criticalResult);
        when(submitLabResultUseCase.execute(criticalResult)).thenReturn(criticalResult);
        when(labOrderRepository.findById(orderId)).thenReturn(Optional.of(labOrder));
        when(labResultRepository.findByOrderId(orderId)).thenReturn(List.of(criticalResult));
        when(mapper.toResponse(criticalResult)).thenReturn(criticalResponse);

        LabResultResponse response = service.submitLabResult(request);

        assertThat(response.isCritical()).isTrue();
        verify(eventPublisher).publishCriticalResult(eq(labOrder), eq(criticalResult));
    }

    @Test
    void submitLabResult_orderNotFound_throwsLabOrderNotFoundException() {
        LabResultRequest request = new LabResultRequest();
        request.setOrderId(orderId);
        request.setTestCode("CBC");

        when(mapper.toDomain(request)).thenReturn(labResult);
        when(submitLabResultUseCase.execute(labResult)).thenReturn(labResult);
        when(labOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitLabResult(request))
                .isInstanceOf(LabOrderNotFoundException.class);
    }

    @Test
    void getResult_existingId_returnsResponse() {
        when(labResultRepository.findById(resultId)).thenReturn(Optional.of(labResult));
        when(mapper.toResponse(labResult)).thenReturn(labResultResponse);

        LabResultResponse response = service.getResult(resultId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(resultId);
    }

    @Test
    void getResult_notFound_throwsLabResultNotFoundException() {
        when(labResultRepository.findById(resultId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResult(resultId))
                .isInstanceOf(LabResultNotFoundException.class);
    }

    @Test
    void getCriticalResults_limitEnforced_returnsAtMostLimit() {
        List<LabResult> allCritical = List.of(
                buildCritical(), buildCritical(), buildCritical(), buildCritical(), buildCritical()
        );
        when(labResultRepository.findCriticalResults()).thenReturn(allCritical);
        when(mapper.toResponse(any(LabResult.class))).thenReturn(labResultResponse);

        List<LabResultResponse> results = service.getCriticalResults(3);

        assertThat(results).hasSize(3);
    }

    @Test
    void getCriticalResults_invalidLimit_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.getCriticalResults(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");

        assertThatThrownBy(() -> service.getCriticalResults(501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void getResultsByOrder_returnsAllForOrder() {
        when(labResultRepository.findByOrderId(orderId)).thenReturn(List.of(labResult));
        when(mapper.toResponse(labResult)).thenReturn(labResultResponse);

        List<LabResultResponse> results = service.getResultsByOrder(orderId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTestCode()).isEqualTo("CBC");
    }

    // --

    private LabResult buildCritical() {
        return LabResult.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .testCode("K")
                .abnormalFlag(AbnormalFlag.CRITICALLY_HIGH)
                .status(ResultStatus.FINAL)
                .build();
    }
}
