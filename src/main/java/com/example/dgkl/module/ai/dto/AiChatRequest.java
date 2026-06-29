package com.example.dgkl.module.ai.dto;

import lombok.Data;

@Data
public class AiChatRequest {
    private Long sessionId;
    private String query;
    private String conversationId;
}
