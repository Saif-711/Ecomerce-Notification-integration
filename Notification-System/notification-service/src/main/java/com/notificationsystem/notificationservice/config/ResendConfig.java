package com.notificationsystem.notificationservice.config;

import com.resend.Resend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class ResendConfig {

    private static final Logger log = LoggerFactory.getLogger(ResendConfig.class);

    @Value("${notification.resend.api-key:}")
    private String apiKey;

    @Value("${notification.resend.from-email:}")
    private String fromEmail;

    @PostConstruct
    public void init() {
        log.info("=== Resend Configuration ===");
        log.info("API Key loaded: {}", hasApiKey() ? "yes" : "no");
        log.info("From Email loaded: {}", fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "null/empty");
        log.info("===========================");
    }

    @Bean
    @ConditionalOnProperty(name = "notification.resend.api-key")
    public Resend resendClient() {
        String trimmed = apiKey == null ? "" : apiKey.trim();
        log.info("Creating Resend client (API key present: {})", !trimmed.isEmpty());
        return new Resend(trimmed);
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
