package com.example.dgkl.module.agent.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DifyChatRequest {
    private String query;
    private String conversationId;
    private Map<String, Object> inputs = new HashMap<>();
}
