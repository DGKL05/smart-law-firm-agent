package com.example.dgkl.module.ai.dto;

import lombok.Data;

@Data
public class AiChatResponse {
    private String answer;
    private String conversationId;
    private String messageId;
    private String intent;
}
