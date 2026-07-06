package com.example.dgkl.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_message")
public class AiChatMessage {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String role;
    private String content;
    private LocalDateTime createTime;
    private Integer deleted;
}
