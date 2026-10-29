package com.notificationsystem.notificationservice.model;

/**
 * Status = WHERE this notification is in its life.
 *
 * Beginner view: Kafka does not remember "did we send it?" for our business.
 * The database row uses these values so we can answer: pending, sending, sent, or failed.
 *
 * Flow today:
 * PENDING → PROCESSING → SENT
 * PROCESSING → FAILED after worker retries are exhausted
 */
public enum NotificationStatus {
    PENDING,     // saved, waiting (maybe scheduled for later)
    PROCESSING,  // already given to Kafka, workers are handling it
    SENT,        // worker finished
    FAILED       // worker tried and it did not work
}
