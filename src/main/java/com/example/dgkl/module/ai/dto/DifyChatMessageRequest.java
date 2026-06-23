package com.example.dgkl.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class DifyChatMessageRequest {
    private Map<String, Object> inputs;
    private String query;

    @JsonProperty("response_mode")
    private String responseMode;

    @JsonProperty("conversation_id")
    private String conversationId;

    private String user;
}
