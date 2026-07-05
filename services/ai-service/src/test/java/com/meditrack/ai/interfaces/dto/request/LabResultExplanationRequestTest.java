package com.meditrack.ai.interfaces.dto.request;

import com.meditrack.ai.domain.model.LabResultExplanationCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LabResultExplanationRequestTest {

    @Test
    void mapsAllFieldsToCommand() {
        LabResultExplanationRequest req = new LabResultExplanationRequest(
                List.of(new LabValueInput("Glucose", "250", "mg/dL", "70-99", "H")),
                45, "FEMALE", "fasting");

        LabResultExplanationCommand cmd = req.toCommand();

        assertThat(cmd.patientAgeYears()).isEqualTo(45);
        assertThat(cmd.patientSex()).isEqualTo("FEMALE");
        assertThat(cmd.context()).isEqualTo("fasting");
        assertThat(cmd.results()).hasSize(1);
        assertThat(cmd.results().get(0).testName()).isEqualTo("Glucose");
        assertThat(cmd.results().get(0).referenceRange()).isEqualTo("70-99");
        assertThat(cmd.results().get(0).flag()).isEqualTo("H");
    }

    @Test
    void nullResultsBecomeEmptyListRatherThanNpe() {
        LabResultExplanationRequest req =
                new LabResultExplanationRequest(null, null, null, null);

        LabResultExplanationCommand cmd = req.toCommand();

        assertThat(cmd.results()).isEmpty();
        assertThat(cmd.patientAgeYears()).isNull();
    }
}
