package com.notificationsystem.notificationservice.worker;

import com.notificationsystem.notificationservice.event.NotificationEvent;
import com.notificationsystem.notificationservice.service.NotificationDeliveryService;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * EmailWorker = the last step for EMAIL notifications.
 *
 * Uses Resend API for real email delivery (optional).
 * If Resend is not configured, falls back to console logging.
 * Flow: API → Kafka → Processor → this class → mark SENT in MySQL
 *
 * Different groupId from the Processor ("email-worker") so this team
 * only reads the email topic, not the main "notifications" topic.
 */
@Service
public class EmailWorker {

    private static final Logger log = LoggerFactory.getLogger(EmailWorker.class);

    private final NotificationDeliveryService deliveryService;
    private final Resend resendClient;
    private final String fromEmail;

    public EmailWorker(
            NotificationDeliveryService deliveryService,
            @Autowired(required = false) Resend resendClient,
            @Value("${notification.resend.from-email:}") String fromEmail) {
        this.deliveryService = deliveryService;
        this.resendClient = resendClient;
        this.fromEmail = fromEmail;
    }

    @KafkaListener(topics = "${notification.kafka.topics.email}", groupId = "email-worker")
    public void handleEmailNotification(NotificationEvent event) {
        log.info("Processing email notification for user {}", event.getUserId());

        // Idempotency check: skip if already SENT
        if (deliveryService.isAlreadySent(event.getId())) {
            log.info("Notification {} is already SENT, skipping email send", event.getId());
            return;
        }

        String toEmail = event.getRecipientEmail();
        if (toEmail == null || toEmail.isEmpty()) {
            toEmail = "fallback@example.com";
            log.warn("No recipient email provided, using fallback: {}", toEmail);
        }

        log.info("Resend client is null: {}, fromEmail is null: {}, fromEmail empty: {}",
                resendClient == null, fromEmail == null, fromEmail != null && fromEmail.isEmpty());

        if (resendClient == null || fromEmail == null || fromEmail.isEmpty()) {
            log.warn("Resend not configured. Using console fallback for email to {}", toEmail);
            log.info("Email would be sent to: {} | Message: {}", toEmail, event.getMessage());
            // Mark as sent anyway for testing purposes
            deliveryService.markSent(event.getId());
            return;
        }

        log.info("Sending email to user {} at {}", event.getUserId(), toEmail);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Notification")
                .html("<p>" + event.getMessage() + "</p>")
                .build();

        try {
            log.info("Attempting to send email via Resend API to: {}", toEmail);
            CreateEmailResponse response = resendClient.emails().send(params);
            log.info("Email sent successfully via Resend. Resend ID: {}", response.getId());
            log.info("Now marking notification {} as SENT in database", event.getId());
            deliveryService.markSent(event.getId());
            log.info("Successfully marked notification {} as SENT", event.getId());
        } catch (Exception e) {
            log.error("=== EMAIL SEND FAILED ===");
            log.error("Notification ID: {}", event.getId());
            log.error("Recipient: {}", toEmail);
            log.error("From: {}", fromEmail);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Full exception:", e);
            log.error("========================");

            // Resend's Java SDK throws RuntimeException (not ResendException) on 401
            if (isPermanentFailure(e)) {
                log.error("Permanent email failure for notification {}: {}", event.getId(), e.getMessage());
                deliveryService.markFailed(event.getId());
                return;
            }
            log.error("Temporary email failure for notification {}. Will retry via Kafka.", event.getId());
            throw new IllegalStateException("Failed to send email for notification " + event.getId(), e);
        }
    }

    private boolean isPermanentFailure(Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("api key is invalid")
                || lower.contains("401")
                || lower.contains("403")
                || lower.contains("statuscode\":400")
                || lower.contains("400")
                || lower.contains("invalid")
                || lower.contains("unverified")
                || lower.contains("not verified");
    }
}
