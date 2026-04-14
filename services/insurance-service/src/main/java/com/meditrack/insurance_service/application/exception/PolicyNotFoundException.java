package com.meditrack.insurance_service.application.exception;

import java.util.UUID;

public class PolicyNotFoundException extends InsuranceServiceException {
    public PolicyNotFoundException(UUID policyId) {
        super("Insurance policy not found: " + policyId);
    }

    public PolicyNotFoundException(String policyNumber) {
        super("Insurance policy not found for policy number: " + policyNumber);
    }
}
