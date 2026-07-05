package com.meditrack.ai.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalUrgencyTest {

    @Test
    void parsesKnownValuesCaseInsensitively() {
        assertThat(ClinicalUrgency.fromString("critical")).isEqualTo(ClinicalUrgency.CRITICAL);
        assertThat(ClinicalUrgency.fromString(" Urgent ")).isEqualTo(ClinicalUrgency.URGENT);
        assertThat(ClinicalUrgency.fromString("ROUTINE")).isEqualTo(ClinicalUrgency.ROUTINE);
    }

    @Test
    void blankOrNullDefaultsToRoutine() {
        assertThat(ClinicalUrgency.fromString(null)).isEqualTo(ClinicalUrgency.ROUTINE);
        assertThat(ClinicalUrgency.fromString("")).isEqualTo(ClinicalUrgency.ROUTINE);
    }

    @Test
    void unrecognisedLabelDefaultsToMonitorNotRoutine() {
        // Conservative: an odd value must not read as "nothing to do".
        assertThat(ClinicalUrgency.fromString("SEVERE")).isEqualTo(ClinicalUrgency.MONITOR);
    }

    @Test
    void urgentAndCriticalAreActionable() {
        assertThat(ClinicalUrgency.URGENT.isActionable()).isTrue();
        assertThat(ClinicalUrgency.CRITICAL.isActionable()).isTrue();
        assertThat(ClinicalUrgency.ROUTINE.isActionable()).isFalse();
        assertThat(ClinicalUrgency.MONITOR.isActionable()).isFalse();
    }
}
