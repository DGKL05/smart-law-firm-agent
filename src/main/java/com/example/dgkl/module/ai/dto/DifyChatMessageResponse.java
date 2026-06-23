package com.example.dgkl.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DifyChatMessageResponse {
    private String event;

    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("workflow_run_id")
    private String workflowRunId;

    private String answer;
}
