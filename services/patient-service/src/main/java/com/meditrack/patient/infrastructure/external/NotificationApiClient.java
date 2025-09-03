package com.meditrack.patient.infrastructure.external;

import org.springframework.stereotype.Component;

@Component
public class NotificationApiClient {
    // This is a more realistic stub for an external API client.
    // An actual implementation would use RestTemplate or WebClient to send notifications (e.g., email, SMS).
    public void sendNotification(String patientId, String message) {
        System.out.println("Simulating external call: Sending notification to patient " + patientId + ": " + message);
        // Simulate network delay
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Notification sent successfully.");
    }

    public void sendEmail(String recipientEmail, String subject, String body) {
        System.out.println("Simulating external call: Sending email to " + recipientEmail + " with subject: " + subject);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Email sent successfully.");
    }

    public void sendSms(String phoneNumber, String message) {
        System.out.println("Simulating external call: Sending SMS to " + phoneNumber + ": " + message);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("SMS sent successfully.");
    }
}
