package com.meditrack.prescription.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Drug-safety screen outcome attached to a prescription response. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyScreenResponse {

    /** False when the AI screen could not be performed (fail-open issue). */
    private boolean checked;

    /** NONE / MINOR / MODERATE / MAJOR / CONTRAINDICATED — null when not checked. */
    private String severity;

    private String summary;

    /** Only populated on the issue response (not persisted). */
    private Boolean requiresPharmacistReview;

    private boolean overridden;
    private String overrideReason;

    /** Individual findings — only populated on the issue response. */
    private List<SafetyFindingResponse> findings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SafetyFindingResponse {
        private String type;
        private String severity;
        private String description;
    }
}
