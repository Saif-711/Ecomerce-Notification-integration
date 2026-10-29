package com.notificationsystem.notificationservice.scheduler;

import com.notificationsystem.notificationservice.domain.Notification;
import com.notificationsystem.notificationservice.model.NotificationStatus;
import com.notificationsystem.notificationservice.repository.NotificationRepository;
import com.notificationsystem.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationScheduler = a clock that looks at the database every second.
 *
 * Beginner view: HTTP requests only happen when a client calls us.
 * A scheduled notification has no client at 10:00, so we need a timer:
 *
 *   every 1 second → find PENDING rows where scheduledAt <= now → publish to Kafka
 *
 * Needs @EnableScheduling on NotificationServiceApplication, otherwise this never runs.
 *
 * Later we may replace this simple loop with Redis. For learning, @Scheduled is enough.
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final Duration staleProcessingAfter;

    public NotificationScheduler(
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            @Value("${notification.processing-stale-after:2m}") Duration staleProcessingAfter) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.staleProcessingAfter = staleProcessingAfter;
    }

    // fixedDelay = 1000 → wait 1 second after the previous run FINISHES, then run again
    @Scheduled(fixedDelay = 1000)
    public void processScheduledNotifications() {
        List<Notification> due = notificationRepository.findByStatusAndScheduledAtLessThanEqual(
                NotificationStatus.PENDING,
                LocalDateTime.now()
        );

        for (Notification notification : due) {
            log.info("Publishing scheduled notification {}", notification.getId());
            // publish() will skip the row if it is no longer PENDING (safe if two runs overlap)
            notificationService.publish(notification);
        }

        recoverStaleProcessing();
    }

    /**
     * PROCESSING rows that never reached SENT/FAILED (worker crash, swallowed error,
     * Kafka message lost) are reset to PENDING so publish() can put them on Kafka again.
     */
    private void recoverStaleProcessing() {
        LocalDateTime staleBefore = LocalDateTime.now().minus(staleProcessingAfter);
        List<Notification> stuck = notificationRepository.findStaleByStatus(
                NotificationStatus.PROCESSING,
                staleBefore
        );

        for (Notification notification : stuck) {
            log.warn("Recovering stuck PROCESSING notification {}", notification.getId());
            notificationService.recoverStuck(notification);
        }
    }
}
