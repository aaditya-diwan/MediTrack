package com.meditrack.patient.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationApiClient {
    // Stub — replace with RestTemplate/WebClient call to the real notification service.
    public void sendNotification(String patientId, String message) {
        log.info("Stub: sending notification [patientId={}, message={}]", patientId, message);
        log.debug("Stub: notification dispatched successfully [patientId={}]", patientId);
    }

    public void sendEmail(String recipientEmail, String subject, String body) {
        // Do not log body — may contain PHI
        log.info("Stub: sending email [recipient={}, subject={}]", recipientEmail, subject);
        log.debug("Stub: email dispatched successfully [recipient={}]", recipientEmail);
    }

    public void sendSms(String phoneNumber, String message) {
        // Do not log phoneNumber at INFO level — PII
        log.info("Stub: sending SMS notification");
        log.debug("Stub: SMS dispatched successfully [phoneNumber={}]", phoneNumber);
    }
}
