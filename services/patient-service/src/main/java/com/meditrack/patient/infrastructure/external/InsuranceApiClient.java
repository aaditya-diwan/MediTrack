package com.meditrack.patient.infrastructure.external;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class InsuranceApiClient {
    // This is a more realistic stub for an external API client.
    // An actual implementation would use RestTemplate or WebClient to call an external insurance service.
    public boolean verifyInsurance(String policyNumber) {
        System.out.println("Simulating external call: Verifying insurance policy: " + policyNumber);
        // Simulate network delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Simulate success or failure
        return new Random().nextBoolean();
    }

    public String getInsuranceDetails(String policyNumber) {
        System.out.println("Simulating external call: Getting insurance details for policy: " + policyNumber);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Details for policy " + policyNumber + ": Active, Coverage: Full";
    }
}
