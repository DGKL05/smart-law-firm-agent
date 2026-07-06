package com.example.dgkl.ai.dto;

import lombok.Data;

@Data
public class AiChatRequest {
    private Long sessionId;
    private String query;
    private String conversationId;
}
