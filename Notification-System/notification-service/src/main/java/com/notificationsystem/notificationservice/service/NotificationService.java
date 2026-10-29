package com.notificationsystem.notificationservice.service;

import com.notificationsystem.notificationservice.domain.Notification;
import com.notificationsystem.notificationservice.dto.NotificationRequest;
import com.notificationsystem.notificationservice.event.NotificationEvent;
import com.notificationsystem.notificationservice.model.NotificationStatus;
import com.notificationsystem.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * NotificationService = the brain of "create a notification".
 *
 * Beginner view: the controller should stay thin. This class does the real steps:
 * 1) save a row in MySQL (status PENDING)
 * 2) if the time is now (or already past) → publish to Kafka
 * 3) if the time is in the future → do nothing; NotificationScheduler will publish later
 *
 * KafkaTemplate is the Kafka PRODUCER (the sender).
 */
@Service
public class NotificationService {

    // KafkaTemplate<key type, value type>
    // key = String (userId)   value = NotificationEvent (JSON)
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final NotificationRepository notificationRepository;
    private final String notificationsTopic;

    public NotificationService(
            KafkaTemplate<String, NotificationEvent> kafkaTemplate,
            NotificationRepository notificationRepository,
            @Value("${notification.kafka.topics.notifications}") String notificationsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationRepository = notificationRepository;
        this.notificationsTopic = notificationsTopic;
    }

    public void create(NotificationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID().toString()); // unique id for DB + Kafka event
        notification.setUserId(request.getUserId());
        notification.setChannel(request.getChannel());
        notification.setMessage(request.getMessage());
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setStatus(NotificationStatus.PENDING);
        // No scheduledAt from client → treat as "send now"
        notification.setScheduledAt(request.getScheduledAt() != null ? request.getScheduledAt() : now);
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        // Commit the row before Kafka so the worker can find it
        notificationRepository.saveAndFlush(notification);

        // isAfter(now) means the time is still in the future → wait for the scheduler
        if (!notification.getScheduledAt().isAfter(now)) {
            publish(notification);
        }
    }

    /**
     * Put the job on Kafka topic "notifications".
     * Called from create() for immediate jobs, and from the scheduler for future jobs.
     */
    public void publish(Notification notification) {
        // Claim the row: PENDING → PROCESSING. claimed == 0 means someone else already took it.
        int claimed = notificationRepository.updateStatusIfCurrent(
                notification.getId(),
                NotificationStatus.PENDING,
                NotificationStatus.PROCESSING
        );
        if (claimed == 0) {
            return;
        }

        sendToKafka(notification);
    }

    public void recoverStuck(Notification notification) {
        int reset = notificationRepository.updateStatusIfCurrent(
                notification.getId(),
                NotificationStatus.PROCESSING,
                NotificationStatus.PENDING
        );
        if (reset == 0) {
            return;
        }
        publish(notification);
    }

    private void sendToKafka(Notification notification) {
        NotificationEvent event = new NotificationEvent(
                notification.getId(),
                notification.getUserId(),
                notification.getChannel(),
                notification.getMessage(),
                notification.getRecipientEmail()
        );

        try {
            kafkaTemplate.send(
                    notificationsTopic,
                    notification.getUserId().toString(),
                    event
            ).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            notificationRepository.updateStatusIfCurrent(
                    notification.getId(),
                    NotificationStatus.PROCESSING,
                    NotificationStatus.PENDING
            );
            throw new IllegalStateException("Failed to publish notification " + notification.getId(), e);
        }
    }
}
