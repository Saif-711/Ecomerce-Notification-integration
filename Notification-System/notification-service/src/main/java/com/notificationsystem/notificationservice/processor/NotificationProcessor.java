package com.notificationsystem.notificationservice.processor;

import com.notificationsystem.notificationservice.event.NotificationEvent;
import com.notificationsystem.notificationservice.service.NotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * NotificationProcessor = the ROUTER.
 *
 * Beginner view: it does NOT send email. It only reads from topic "notifications"
 * and forwards the same event to the matching channel topic:
 *
 *   EMAIL   → email-notifications
 *   SMS     → sms-notifications
 *   WEBHOOK → webhook-notifications
 *
 * Why extra hop? So each worker can scale alone (many email workers, few SMS workers).
 *
 * @KafkaListener = this method is a Kafka CONSUMER. Spring calls it for each new message.
 */
@Service
public class NotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final NotificationDeliveryService deliveryService;
    private final String emailTopic;
    private final String smsTopic;
    private final String webhookTopic;

    public NotificationProcessor(
            KafkaTemplate<String, NotificationEvent> kafkaTemplate,
            NotificationDeliveryService deliveryService,
            @Value("${notification.kafka.topics.email}") String emailTopic,
            @Value("${notification.kafka.topics.sms}") String smsTopic,
            @Value("${notification.kafka.topics.webhook}") String webhookTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.deliveryService = deliveryService;
        this.emailTopic = emailTopic;
        this.smsTopic = smsTopic;
        this.webhookTopic = webhookTopic;
    }

    // groupId = team name of consumers. Several instances with the SAME group share the work.
    @KafkaListener(topics = "${notification.kafka.topics.notifications}",
            groupId = "notification-processor")
    public void process(NotificationEvent event) {
        log.info("Processing notification {}", event);

        switch (event.getChannel()) {
            case EMAIL -> forward(emailTopic, event);
            case SMS -> forward(smsTopic, event);
            case WEBHOOK -> forward(webhookTopic, event);
            default -> {
                log.warn("Unknown channel {} for notification {}", event.getChannel(), event);
                deliveryService.markFailed(event.getId());
            }
        }
    }

    private void forward(String topic, NotificationEvent event) {
        try {
            kafkaTemplate.send(topic, event.getUserId().toString(), event).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to forward notification " + event.getId() + " to " + topic, e);
        }
    }
}
