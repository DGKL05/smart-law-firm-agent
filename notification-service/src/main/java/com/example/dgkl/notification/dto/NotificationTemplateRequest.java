package com.example.dgkl.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotificationTemplateRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}") String code,
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 500) String content) {
}
