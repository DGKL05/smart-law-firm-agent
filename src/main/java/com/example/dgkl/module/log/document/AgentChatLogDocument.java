package com.example.dgkl.module.log.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "agent_chat_log")
public class AgentChatLogDocument {
    @Id
    private String id;
    private Long userId;
    private String sessionId;
    private String message;
    private String response;
    private LocalDateTime createTime;
}
