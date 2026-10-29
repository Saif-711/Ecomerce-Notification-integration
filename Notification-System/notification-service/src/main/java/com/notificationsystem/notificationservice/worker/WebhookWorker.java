package com.notificationsystem.notificationservice.worker;

import com.notificationsystem.notificationservice.event.NotificationEvent;
import com.notificationsystem.notificationservice.service.NotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * WebhookWorker = the last step for WEBHOOK notifications.
 *
 * Beginner view: a webhook means "call another server with HTTP",
 * for example your exam-system URL. We do not HTTP-call yet; we only print.
 *
 * Same pattern as EmailWorker / SmsWorker so you can compare the three channels.
 */
@Service
public class WebhookWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookWorker.class);

    private final NotificationDeliveryService deliveryService;

    public WebhookWorker(NotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(topics = "${notification.kafka.topics.webhook}", groupId = "webhook-worker")
    public void sendWebhook(NotificationEvent event) {
        log.info("Processing webhook notification for user {}", event.getUserId());

        // Idempotency check: skip if already SENT
        if (deliveryService.isAlreadySent(event.getId())) {
            log.info("Notification {} is already SENT, skipping webhook send", event.getId());
            return;
        }

        log.info("Sending webhook for user {}", event.getUserId());
        System.out.println("Sending webhook for user " + event.getUserId());
        deliveryService.markSent(event.getId());
    }
}
