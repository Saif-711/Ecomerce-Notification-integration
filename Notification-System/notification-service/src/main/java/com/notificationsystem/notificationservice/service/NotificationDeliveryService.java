package com.notificationsystem.notificationservice.service;

import com.notificationsystem.notificationservice.domain.Notification;
import com.notificationsystem.notificationservice.model.NotificationStatus;
import com.notificationsystem.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * NotificationDeliveryService = update the database AFTER a worker "sends".
 *
 * Beginner view: EmailWorker / SmsWorker / WebhookWorker should not each copy
 * the same "set status = SENT" code. They all call this class.
 *
 * This is how we know later: yes, this notification left the system, and when.
 */
@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private final NotificationRepository notificationRepository;

    public NotificationDeliveryService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void markSent(String id) {
        log.info("markSent called for notification ID: {}", id);
        Notification notification = requireNotification(id);
        log.info("Found notification with current status: {}", notification.getStatus());
        notification.setStatus(NotificationStatus.SENT);
        LocalDateTime now = LocalDateTime.now();
        notification.setSentAt(now);
        notification.setUpdatedAt(now);
        notificationRepository.save(notification);
        log.info("Successfully saved notification {} with status SENT", id);
    }

    @Transactional(readOnly = true)
    public boolean isAlreadySent(String id) {
        return notificationRepository.findById(id)
                .map(n -> n.getStatus() == NotificationStatus.SENT)
                .orElse(false);
    }

    @Transactional
    public void markFailed(String id) {
        log.info("markFailed called for notification ID: {}", id);
        if (id == null || id.isBlank()) {
            log.warn("markFailed called with null or blank ID");
            return;
        }
        notificationRepository.findById(id).ifPresent(notification -> {
            log.info("Found notification with current status: {}", notification.getStatus());
            if (notification.getStatus() == NotificationStatus.SENT) {
                log.warn("Notification {} is already SENT, skipping markFailed", id);
                return;
            }
            notification.setStatus(NotificationStatus.FAILED);
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Successfully saved notification {} with status FAILED", id);
        });
    }

    private Notification requireNotification(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Notification not found: " + id));
    }
}
