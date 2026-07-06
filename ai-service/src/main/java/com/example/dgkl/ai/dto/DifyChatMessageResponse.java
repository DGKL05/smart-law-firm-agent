package com.example.dgkl.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DifyChatMessageResponse {
    @JsonProperty("message_id")
    private String messageId;
    @JsonProperty("conversation_id")
    private String conversationId;
    private String answer;
}
