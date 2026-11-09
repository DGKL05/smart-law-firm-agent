package com.example.dgkl.ai.dto;

import com.example.dgkl.ai.entity.AiChatSession;

import java.time.LocalDateTime;

public record AiChatSessionView(
        Long id, Long userId, String title, String conversationId, String lastMessage,
        LocalDateTime createTime, LocalDateTime updateTime) {
    public static AiChatSessionView of(AiChatSession session) {
        return new AiChatSessionView(session.getId(), session.getUserId(), session.getTitle(),
                session.getConversationId(), session.getLastMessage(),
                session.getCreateTime(), session.getUpdateTime());
    }
}
