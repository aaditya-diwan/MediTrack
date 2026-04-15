package com.meditrack.insurance_service.application.exception;

public class DuplicatePolicyException extends InsuranceServiceException {

    public DuplicatePolicyException(String policyNumber) {
        super("Insurance policy already exists with number: " + policyNumber);
    }
}
