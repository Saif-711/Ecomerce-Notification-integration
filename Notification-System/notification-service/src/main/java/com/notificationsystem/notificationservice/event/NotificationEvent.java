package com.notificationsystem.notificationservice.event;

import com.notificationsystem.notificationservice.model.Channel;

/**
 * NotificationEvent = the MESSAGE we put on Kafka.
 *
 * Beginner view: the HTTP request stays in the API.
 * This object is what travels: API → Kafka topic "notifications" → Processor → email/sms/webhook topics → Worker.
 *
 * Why a separate class from NotificationRequest?
 * - Request is what the client sends (may include scheduledAt)
 * - Event is what workers need (id + user + channel + message)
 * Kafka does not need scheduledAt; scheduling already happened before we publish.
 *
 * Empty constructor + getters/setters are required so Spring Kafka can turn JSON into this class.
 */
public class NotificationEvent {

    private String id;       // same id as the database row, so the worker can mark that row SENT
    private Long userId;
    private Channel channel;
    private String message;
    private String recipientEmail; // Optional recipient email for EMAIL channel

    public NotificationEvent() {
    }

    public NotificationEvent(String id, Long userId, Channel channel, String message) {
        this.id = id;
        this.userId = userId;
        this.channel = channel;
        this.message = message;
    }

    public NotificationEvent(String id, Long userId, Channel channel, String message, String recipientEmail) {
        this.id = id;
        this.userId = userId;
        this.channel = channel;
        this.message = message;
        this.recipientEmail = recipientEmail;
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

    @Override
    public String toString() {
        return "NotificationEvent{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", channel=" + channel +
                ", message='" + message + '\'' +
                '}';
    }
}
