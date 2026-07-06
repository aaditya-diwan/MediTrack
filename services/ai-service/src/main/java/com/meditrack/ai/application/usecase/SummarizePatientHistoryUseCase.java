package com.meditrack.ai.application.usecase;

import com.meditrack.ai.domain.model.HistorySummaryCommand;
import com.meditrack.ai.domain.model.PatientHistorySummary;

/** Distil a patient's record into a pre-consultation brief. */
public interface SummarizePatientHistoryUseCase {

    PatientHistorySummary summarize(HistorySummaryCommand command);
}
