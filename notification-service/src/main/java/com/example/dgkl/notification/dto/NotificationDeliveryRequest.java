package com.example.dgkl.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NotificationDeliveryRequest(
        @NotBlank @Size(max = 128) String eventKey,
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 500) String content) {
}
