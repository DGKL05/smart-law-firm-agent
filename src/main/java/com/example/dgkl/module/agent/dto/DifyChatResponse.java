package com.example.dgkl.module.agent.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DifyChatResponse {
    private String event;
    private String taskId;
    private String messageId;
    private String conversationId;
    private String workflowRunId;
    private String answer;
    private Map<String, Object> metadata;
}
