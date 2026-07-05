package com.meditrack.ai.domain.port;

import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.domain.model.SafetyCheckCommand;

/**
 * Outbound port for clinical reasoning. The domain depends on this abstraction,
 * not on any particular LLM vendor — the TensorX adapter is one implementation,
 * and it can be swapped for another inference provider (or a rules engine)
 * without touching the application or domain layers.
 */
public interface ClinicalReasoningPort {

    SafetyAssessment assess(SafetyCheckCommand command);
}
