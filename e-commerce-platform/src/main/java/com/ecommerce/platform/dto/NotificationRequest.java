package com.ecommerce.platform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "User ID cannot be null")
    @Positive(message = "User ID must be a positive number")
    private Long userId;

    @NotNull(message = "Channel cannot be null")
    private NotificationChannel channel;

    @NotBlank(message = "Message cannot be null or empty")
    private String message;

    // Optional recipient email for EMAIL channel
    private String recipientEmail;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;
}
