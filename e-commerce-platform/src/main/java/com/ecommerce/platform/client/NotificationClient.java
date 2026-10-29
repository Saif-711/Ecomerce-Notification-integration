// package com.ecommerce.platform.client;

// import com.ecommerce.platform.dto.NotificationChannel;
// import com.ecommerce.platform.dto.NotificationRequest;
// import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
// import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
// import jakarta.validation.ConstraintViolation;
// import jakarta.validation.Validator;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.retry.annotation.Backoff;
// import org.springframework.retry.annotation.Recover;
// import org.springframework.retry.annotation.Retryable;
// import org.springframework.stereotype.Component;
// import org.springframework.scheduling.annotation.Async;
// import org.springframework.web.client.RestClientException;
// import org.springframework.web.client.RestTemplate;

// import java.time.LocalDateTime;
// import java.util.Set;
// import java.util.concurrent.CompletableFuture;

// @Component
// public class NotificationClient {

//     private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

//     private final RestTemplate restTemplate;
//     private final String notificationServiceUrl;
//     private final Validator validator;

//     public NotificationClient(RestTemplate restTemplate,
//                               @Value("${notification.service.url}") String notificationServiceUrl,
//                               @Value("${notification.service.endpoint}") String endpoint,
//                               Validator validator) {
//         this.restTemplate = restTemplate;
//         this.notificationServiceUrl = notificationServiceUrl + endpoint;
//         this.validator = validator;
//     }

//     @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationCircuitBreakerFallback")
//     @Retryable(
//             retryFor = RestClientException.class,
//             maxAttempts = 3,
//             backoff = @Backoff(delay = 1000),
//             recover = "sendNotificationRecovery"
//     )
//     public void sendNotification(Long userId, NotificationChannel channel, String message) {
//         sendNotification(userId, channel, message, null);
//     }

//     @Async("notificationExecutor")
//     @CircuitBreaker(name = "notificationService",
//     fallbackMethod = "sendNotificationCircuitBreakerFallbackAsync")
//     public CompletableFuture<Void> sendNotificationAsync(Long userId, NotificationChannel channel, String message) {
//         try {
//             sendNotification(userId, channel, message, null);
//             return CompletableFuture.completedFuture(null);
//         } catch (Exception e) {
//             CompletableFuture<Void> future = new CompletableFuture<>();
//             future.completeExceptionally(e);
//             return future;
//         }
//     }

//     @Async("notificationExecutor")
//     @CircuitBreaker(name = "notificationService",
//     fallbackMethod = "sendNotificationCircuitBreakerFallbackAsync")
//     public CompletableFuture<Void> sendNotificationAsync(Long userId, NotificationChannel channel, String message, String recipientEmail) {
//         try {
//             sendNotification(userId, channel, message, recipientEmail, null);
//             return CompletableFuture.completedFuture(null);
//         } catch (Exception e) {
//             CompletableFuture<Void> future = new CompletableFuture<>();
//             future.completeExceptionally(e);
//             return future;
//         }
//     }

//     @Recover
//     public CompletableFuture<Void> sendNotificationRecoveryAsync(RestClientException e,
//                                                                    Long userId,
//                                                                    NotificationChannel channel,
//                                                                    String message) {
//         log.error("Failed to send notification after 3 retry attempts (async): error={}", e.getMessage());
//         CompletableFuture<Void> future = new CompletableFuture<>();
//         future.completeExceptionally(e);
//         return future;
//     }

//     @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationCircuitBreakerFallback")
//     @Retryable(
//             retryFor = RestClientException.class,
//             maxAttempts = 3,
//             backoff = @Backoff(delay = 1000),
//             recover = "sendNotificationRecovery"
//     )
//     public void sendNotification(Long userId, NotificationChannel channel, String message, LocalDateTime scheduledAt) {
//         sendNotification(userId, channel, message, null, scheduledAt);
//     }

//     @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationCircuitBreakerFallback")
//     @Retryable(
//             retryFor = RestClientException.class,
//             maxAttempts = 3,
//             backoff = @Backoff(delay = 1000),
//             recover = "sendNotificationRecovery"
//     )
//     public void sendNotification(Long userId, NotificationChannel channel, String message, String recipientEmail, LocalDateTime scheduledAt) {
//         NotificationRequest request = new NotificationRequest();
//         request.setUserId(userId);
//         request.setChannel(channel);
//         request.setMessage(message);
//         request.setRecipientEmail(recipientEmail);
//         request.setScheduledAt(scheduledAt);

//         log.info(">>> Attempting to send notification");

//         // Validate using Bean Validation
//         Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);
//         if (!violations.isEmpty()) {
//             String errorMessage = violations.stream()
//                     .map(ConstraintViolation::getMessage)
//                     .reduce((a, b) -> a + ", " + b)
//                     .orElse("Validation failed");
//             throw new IllegalArgumentException("Notification request validation failed: " + errorMessage);
//         }

//         log.info("Sending notification to Notification System: userId={}, channel={}, message={}",
//                 userId, channel, message);

//         restTemplate.postForObject(notificationServiceUrl, request, Void.class);

//         log.info(">>> Notification sent successfully to Notification System");
//     }

//     @Recover
//     public void sendNotificationRecovery(RestClientException e,
//                                          Long userId,
//                                          NotificationChannel channel,
//                                          String message,
//                                          LocalDateTime scheduledAt) {
//         logRecovery(e);
//         throw e;
//     }

//     @Recover
//     public void sendNotificationRecovery(RestClientException e,
//                                          Long userId,
//                                          NotificationChannel channel,
//                                          String message) {
//         logRecovery(e);
//         throw e;
//     }

//     @Recover
//     public void sendNotificationRecovery(RestClientException e,
//                                          Long userId,
//                                          NotificationChannel channel,
//                                          String message,
//                                          String recipientEmail,
//                                          LocalDateTime scheduledAt) {
//         logRecovery(e);
//         throw e;
//     }

//     public void sendNotificationCircuitBreakerFallback(Long userId,
//                                                        NotificationChannel channel,
//                                                        String message,
//                                                        CallNotPermittedException e) {
//         logCircuitOpen(e);
//     }

//     public void sendNotificationCircuitBreakerFallback(Long userId,
//                                                        NotificationChannel channel,
//                                                        String message,
//                                                        LocalDateTime scheduledAt,
//                                                        CallNotPermittedException e) {
//         logCircuitOpen(e);
//     }

//     public void sendNotificationCircuitBreakerFallback(Long userId,
//                                                        NotificationChannel channel,
//                                                        String message,
//                                                        String recipientEmail,
//                                                        LocalDateTime scheduledAt,
//                                                        CallNotPermittedException e) {
//         logCircuitOpen(e);
//     }

//     public void sendNotificationCircuitBreakerFallback(Long userId,
//                                                        NotificationChannel channel,
//                                                        String message,
//                                                        RestClientException e) {
//         logRecovery(e);
//     }

//     public void sendNotificationCircuitBreakerFallback(Long userId,
//                                                        NotificationChannel channel,
//                                                        String message,
//                                                        LocalDateTime scheduledAt,
//                                                        RestClientException e) {
//         logRecovery(e);
//     }

//     public void sendNotificationCircuitBreakerFallback(Long userId,
//                                                        NotificationChannel channel,
//                                                        String message,
//                                                        String recipientEmail,
//                                                        LocalDateTime scheduledAt,
//                                                        RestClientException e) {
//         logRecovery(e);
//     }

//     private void logRecovery(RestClientException e) {
//         log.error("Failed to send notification after 3 retry attempts: error={}", e.getMessage());
//     }

//     private void logCircuitOpen(CallNotPermittedException e) {
//         log.warn("Notification circuit breaker is open; skipping request: {}", e.getMessage());
//     }

//     // Async circuit breaker fallback
//     public CompletableFuture<Void> sendNotificationCircuitBreakerFallbackAsync(Long userId,
//                                                                                 NotificationChannel channel,
//                                                                                 String message,
//                                                                                 CallNotPermittedException e) {
//         logCircuitOpen(e);
//         CompletableFuture<Void> future = new CompletableFuture<>();
//         future.completeExceptionally(e);
//         return future;
//     }

//     @Recover
//     public CompletableFuture<Void> sendNotificationRecoveryAsync(RestClientException e,
//                                                                    Long userId,
//                                                                    NotificationChannel channel,
//                                                                    String message,
//                                                                    String recipientEmail,
//                                                                    LocalDateTime scheduledAt) {
//         log.error("Failed to send notification after 3 retry attempts (async): error={}", e.getMessage());
//         CompletableFuture<Void> future = new CompletableFuture<>();
//         future.completeExceptionally(e);
//         return future;
//     }

//     // Async circuit breaker fallback with recipientEmail
//     public CompletableFuture<Void> sendNotificationCircuitBreakerFallbackAsync(Long userId,
//                                                                                 NotificationChannel channel,
//                                                                                 String message,
//                                                                                 String recipientEmail,
//                                                                                 CallNotPermittedException e) {
//         logCircuitOpen(e);
//         CompletableFuture<Void> future = new CompletableFuture<>();
//         future.completeExceptionally(e);
//         return future;
//     }

//     // Async circuit breaker fallback with recipientEmail (RestClientException)
//     public CompletableFuture<Void> sendNotificationCircuitBreakerFallbackAsync(Long userId,
//                                                                                 NotificationChannel channel,
//                                                                                 String message,
//                                                                                 String recipientEmail,
//                                                                                 RestClientException e) {
//         logRecovery(e);
//         CompletableFuture<Void> future = new CompletableFuture<>();
//         future.completeExceptionally(e);
//         return future;
//     }
// }











package com.ecommerce.platform.client;

import com.ecommerce.platform.dto.NotificationChannel;
import com.ecommerce.platform.dto.NotificationRequest;
import com.ecommerce.platform.domain.User;
import com.ecommerce.platform.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;
    private final Validator validator;
    private final UserRepository userRepository;

    public NotificationClient(RestTemplate restTemplate,
                              @Value("${notification.service.url}") String notificationServiceUrl,
                              @Value("${notification.service.endpoint}") String endpoint,
                              Validator validator,
                              UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl + endpoint;
        this.validator = validator;
        this.userRepository = userRepository;
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationCircuitBreakerFallback")
    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000),
            recover = "sendNotificationRecovery"
    )
    public void sendNotification(Long userId, NotificationChannel channel, String message) {
        sendNotification(userId, channel, message, null);
    }

    @Async("notificationExecutor")
    @CircuitBreaker(name = "notificationService",
    fallbackMethod = "sendNotificationCircuitBreakerFallbackAsync")
    public CompletableFuture<Void> sendNotificationAsync(Long userId, NotificationChannel channel, String message) {
        try {
            sendNotification(userId, channel, message, null);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Async("notificationExecutor")
    @CircuitBreaker(name = "notificationService",
    fallbackMethod = "sendNotificationCircuitBreakerFallbackAsync")
    public CompletableFuture<Void> sendNotificationAsync(Long userId, NotificationChannel channel, String message, String recipientEmail) {
        try {
            sendNotification(userId, channel, message, recipientEmail, null);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Recover
    public CompletableFuture<Void> sendNotificationRecoveryAsync(RestClientException e,
                                                                   Long userId,
                                                                   NotificationChannel channel,
                                                                   String message) {
        log.error("Failed to send notification after 3 retry attempts (async): error={}", e.getMessage());
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationCircuitBreakerFallback")
    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000),
            recover = "sendNotificationRecovery"
    )
    public void sendNotification(Long userId, NotificationChannel channel, String message, LocalDateTime scheduledAt) {
        sendNotification(userId, channel, message, null, scheduledAt);
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendNotificationCircuitBreakerFallback")
    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000),
            recover = "sendNotificationRecovery"
    )
    public void sendNotification(Long userId, NotificationChannel channel, String message, String recipientEmail, LocalDateTime scheduledAt) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setChannel(channel);
        request.setMessage(message);
        request.setRecipientEmail(resolveRecipientEmail(userId, channel, recipientEmail));
        request.setScheduledAt(scheduledAt);

        log.info(">>> Attempting to send notification");

        // Validate using Bean Validation
        Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Validation failed");
            throw new IllegalArgumentException("Notification request validation failed: " + errorMessage);
        }

        log.info("Sending notification to Notification System: userId={}, channel={}, message={}",
                userId, channel, message);

        restTemplate.postForObject(notificationServiceUrl, request, Void.class);

        log.info(">>> Notification sent successfully to Notification System");
    }

    @Recover
    public void sendNotificationRecovery(RestClientException e,
                                         Long userId,
                                         NotificationChannel channel,
                                         String message,
                                         LocalDateTime scheduledAt) {
        logRecovery(e);
        throw e;
    }

    @Recover
    public void sendNotificationRecovery(RestClientException e,
                                         Long userId,
                                         NotificationChannel channel,
                                         String message) {
        logRecovery(e);
        throw e;
    }

    @Recover
    public void sendNotificationRecovery(RestClientException e,
                                         Long userId,
                                         NotificationChannel channel,
                                         String message,
                                         String recipientEmail,
                                         LocalDateTime scheduledAt) {
        logRecovery(e);
        throw e;
    }

    public void sendNotificationCircuitBreakerFallback(Long userId,
                                                       NotificationChannel channel,
                                                       String message,
                                                       CallNotPermittedException e) {
        logCircuitOpen(e);
    }

    public void sendNotificationCircuitBreakerFallback(Long userId,
                                                       NotificationChannel channel,
                                                       String message,
                                                       LocalDateTime scheduledAt,
                                                       CallNotPermittedException e) {
        logCircuitOpen(e);
    }

    public void sendNotificationCircuitBreakerFallback(Long userId,
                                                       NotificationChannel channel,
                                                       String message,
                                                       String recipientEmail,
                                                       LocalDateTime scheduledAt,
                                                       CallNotPermittedException e) {
        logCircuitOpen(e);
    }

    public void sendNotificationCircuitBreakerFallback(Long userId,
                                                       NotificationChannel channel,
                                                       String message,
                                                       RestClientException e) {
        logRecovery(e);
    }

    public void sendNotificationCircuitBreakerFallback(Long userId,
                                                       NotificationChannel channel,
                                                       String message,
                                                       LocalDateTime scheduledAt,
                                                       RestClientException e) {
        logRecovery(e);
    }

    public void sendNotificationCircuitBreakerFallback(Long userId,
                                                       NotificationChannel channel,
                                                       String message,
                                                       String recipientEmail,
                                                       LocalDateTime scheduledAt,
                                                       RestClientException e) {
        logRecovery(e);
    }

    private String resolveRecipientEmail(Long userId, NotificationChannel channel, String recipientEmail) {
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            return recipientEmail.trim();
        }
        if (channel != NotificationChannel.EMAIL || userId == null) {
            return null;
        }
        String email = userRepository.findById(userId).map(User::getEmail).orElse(null);
        if (email == null) {
            log.warn("No email found for userId={}; notification-service will use its fallback address", userId);
        }
        return email;
    }

    private void logRecovery(RestClientException e) {
        log.error("Failed to send notification after 3 retry attempts: error={}", e.getMessage());
    }

    private void logCircuitOpen(CallNotPermittedException e) {
        log.warn("Notification circuit breaker is open; skipping request: {}", e.getMessage());
    }

    // Async circuit breaker fallback
    public CompletableFuture<Void> sendNotificationCircuitBreakerFallbackAsync(Long userId,
                                                                                NotificationChannel channel,
                                                                                String message,
                                                                                CallNotPermittedException e) {
        logCircuitOpen(e);
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }

    @Recover
    public CompletableFuture<Void> sendNotificationRecoveryAsync(RestClientException e,
                                                                   Long userId,
                                                                   NotificationChannel channel,
                                                                   String message,
                                                                   String recipientEmail,
                                                                   LocalDateTime scheduledAt) {
        log.error("Failed to send notification after 3 retry attempts (async): error={}", e.getMessage());
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }

    // Async circuit breaker fallback with recipientEmail
    public CompletableFuture<Void> sendNotificationCircuitBreakerFallbackAsync(Long userId,
                                                                                NotificationChannel channel,
                                                                                String message,
                                                                                String recipientEmail,
                                                                                CallNotPermittedException e) {
        logCircuitOpen(e);
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }

    // Async circuit breaker fallback with recipientEmail (RestClientException)
    public CompletableFuture<Void> sendNotificationCircuitBreakerFallbackAsync(Long userId,
                                                                                NotificationChannel channel,
                                                                                String message,
                                                                                String recipientEmail,
                                                                                RestClientException e) {
        logRecovery(e);
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }
}

