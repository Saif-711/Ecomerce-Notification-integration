package com.notificationsystem.notificationservice.domain;

import com.notificationsystem.notificationservice.model.Channel;
import com.notificationsystem.notificationservice.model.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Notification = one ROW in the MySQL table "notifications".
 *
 * Beginner view: this is our memory of the job.
 * Kafka is only a mailbox. If we restart Kafka, we still need to know:
 * who should get it, when, and did we already send it?
 *
 * @Entity tells Spring Data JPA: turn this class into a database table.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id // primary key
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Store "EMAIL" as text, not a number, so the table is easy to read
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // Optional recipient email for EMAIL channel. If null, userId is used to lookup email.
    @Column(name = "recipient_email")
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    // When this notification should go out. The scheduler looks at this column.
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Null until a worker successfully handles it
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // Bumped on every status change so we can detect stuck PROCESSING rows
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Notification() {
        // JPA needs an empty constructor
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
