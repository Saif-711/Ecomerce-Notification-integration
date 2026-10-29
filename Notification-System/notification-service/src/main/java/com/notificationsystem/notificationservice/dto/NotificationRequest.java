package com.notificationsystem.notificationservice.dto;

import com.notificationsystem.notificationservice.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * NotificationRequest = the JSON body the CLIENT sends to POST /notifications.
 *
 * Beginner view: this is NOT saved in Kafka and NOT the database table.
 * It is only the input of the HTTP API.
 *
 * Example:
 * {
 *   "userId": 50,
 *   "channel": "EMAIL",
 *   "message": "Your exam is tomorrow at 10 AM",
 *   "scheduledAt": "2026-09-10T10:00:00"   // optional
 * }
 */
public class NotificationRequest {

    @NotNull  // Spring Validation: if missing, API returns 400 (see ApiExceptionHandler)
    private Long userId;

    @NotNull
    private Channel channel;

    @NotBlank // not null and not empty text
    private String message;

    // Optional recipient email. If provided, used for EMAIL channel. Otherwise, userId is used to lookup email.
    private String recipientEmail;

    // Optional. If this is in the future, we wait. If missing, we send as soon as possible.
    private LocalDateTime scheduledAt;

    public NotificationRequest() {
        // Jackson (JSON library) needs an empty constructor to build this object from JSON
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

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
}
