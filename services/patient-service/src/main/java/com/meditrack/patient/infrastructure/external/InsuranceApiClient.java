package com.meditrack.patient.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class InsuranceApiClient {
    // Stub — replace with RestTemplate/WebClient call to the real insurance service.
    public boolean verifyInsurance(String policyNumber) {
        log.info("Stub: verifying insurance policy [policyNumber={}]", policyNumber);
        // Simulate success or failure
        boolean result = new Random().nextBoolean();
        log.debug("Stub: insurance verification result [policyNumber={}, verified={}]", policyNumber, result);
        return result;
    }

    public String getInsuranceDetails(String policyNumber) {
        log.info("Stub: fetching insurance details [policyNumber={}]", policyNumber);
        return "Details for policy " + policyNumber + ": Active, Coverage: Full";
    }
}
