package com.notificationsystem.notificationservice.config;

import com.notificationsystem.notificationservice.event.NotificationEvent;
import com.notificationsystem.notificationservice.service.NotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * After a worker throws (for example Resend fails), Kafka retries a few times.
 * If it still fails, mark the notifications row FAILED instead of leaving PROCESSING forever.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public CommonErrorHandler kafkaErrorHandler(NotificationDeliveryService deliveryService) {
        DefaultErrorHandler handler = new DefaultErrorHandler((record, exception) -> {
            try {
                String id = notificationId(record.value());
                log.error("Giving up on Kafka record after retries", exception);

                if (id != null) {
                    // Check if already SENT before marking as FAILED
                    if (!deliveryService.isAlreadySent(id)) {
                        log.error("Marking notification {} as FAILED after Kafka retries", id);
                        deliveryService.markFailed(id);
                    } else {
                        log.warn("Notification {} is already SENT, not marking as FAILED despite Kafka error", id);
                    }
                }
            } catch (Exception recoverError) {
                log.error("Failed to mark notification as FAILED after Kafka retries", recoverError);
            }
        }, new FixedBackOff(2000L, 3));
        handler.setAckAfterHandle(true);
        return handler;
    }

    private static String notificationId(Object value) {
        if (value instanceof NotificationEvent event) {
            return event.getId();
        }
        if (value instanceof Map<?, ?> map && map.get("id") instanceof String id) {
            return id;
        }
        return null;
    }
}
