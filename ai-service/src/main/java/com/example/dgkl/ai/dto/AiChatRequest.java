package com.example.dgkl.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @Positive Long sessionId,
        @NotBlank @Size(max = 8000) String query,
        @Size(max = 128) String conversationId) {
}
