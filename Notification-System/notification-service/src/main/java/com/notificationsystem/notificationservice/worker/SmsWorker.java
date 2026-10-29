package com.notificationsystem.notificationservice.worker;

import com.notificationsystem.notificationservice.event.NotificationEvent;
import com.notificationsystem.notificationservice.service.NotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * SmsWorker = the last step for SMS notifications.
 *
 * Beginner view: same idea as EmailWorker, but it listens to the SMS topic.
 * Later this would call Twilio (or another SMS company). Today it only prints.
 */
@Service
public class SmsWorker {

    private static final Logger log = LoggerFactory.getLogger(SmsWorker.class);

    private final NotificationDeliveryService deliveryService;

    public SmsWorker(NotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(topics = "${notification.kafka.topics.sms}", groupId = "sms-worker")
    public void sendSms(NotificationEvent event) {
        log.info("Processing SMS notification for user {}", event.getUserId());

        // Idempotency check: skip if already SENT
        if (deliveryService.isAlreadySent(event.getId())) {
            log.info("Notification {} is already SENT, skipping SMS send", event.getId());
            return;
        }

        log.info("Sending SMS to user {}", event.getUserId());
        System.out.println("Sending SMS to user " + event.getUserId());
        deliveryService.markSent(event.getId());
    }
}
