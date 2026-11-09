package com.example.dgkl.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatSessionRequest(@NotBlank @Size(max = 128) String title) {
}
