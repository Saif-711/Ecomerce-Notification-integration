package com.notificationsystem.notificationservice.repository;

import com.notificationsystem.notificationservice.domain.Notification;
import com.notificationsystem.notificationservice.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationRepository = how Java talks to the "notifications" table.
 *
 * Beginner view: you do not write SQL for simple save/find.
 * Spring Data JPA reads the METHOD NAME and builds the query.
 *
 * JpaRepository already gives you: save, findById, findAll, delete, ...
 */
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /**
     * Used by the scheduler:
     * "Give me rows that are still PENDING and whose time has already arrived."
     *
     * LessThanEqual = scheduledAt <= now
     */
    List<Notification> findByStatusAndScheduledAtLessThanEqual(
            NotificationStatus status,
            LocalDateTime scheduledAt
    );

    /**
     * Stuck jobs: still PROCESSING and not touched since staleBefore
     * (or never given updatedAt — old rows created before that column existed).
     */
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND (n.updatedAt IS NULL OR n.updatedAt <= :staleBefore)")
    List<Notification> findStaleByStatus(
            @Param("status") NotificationStatus status,
            @Param("staleBefore") LocalDateTime staleBefore
    );

    /**
     * IMPORTANT: this is not a normal update.
     *
     * We change PENDING → PROCESSING only if the row is STILL PENDING.
     * If two threads (API + scheduler, or two app instances) try at the same time,
     * only one of them gets claimed == 1. The other gets 0 and must stop.
     *
     * That prevents sending the same notification to Kafka twice.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.status = :next, n.updatedAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.status = :current")
    int updateStatusIfCurrent(
            @Param("id") String id,
            @Param("current") NotificationStatus current,
            @Param("next") NotificationStatus next
    );
}
