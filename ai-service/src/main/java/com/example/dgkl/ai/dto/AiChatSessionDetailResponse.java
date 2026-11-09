package com.example.dgkl.ai.dto;

import com.example.dgkl.ai.entity.AiChatMessage;
import com.example.dgkl.ai.entity.AiChatSession;

import java.time.LocalDateTime;
import java.util.List;

public record AiChatSessionDetailResponse(
        Long id, Long userId, String title, String conversationId, String lastMessage,
        LocalDateTime createTime, LocalDateTime updateTime, List<MessageView> messages) {
    public static AiChatSessionDetailResponse of(AiChatSession session, List<AiChatMessage> messages) {
        return new AiChatSessionDetailResponse(session.getId(), session.getUserId(), session.getTitle(),
                session.getConversationId(), session.getLastMessage(), session.getCreateTime(), session.getUpdateTime(),
                messages.stream().map(MessageView::of).toList());
    }

    public record MessageView(Long id, String role, String content, LocalDateTime createTime) {
        private static MessageView of(AiChatMessage message) {
            return new MessageView(message.getId(), message.getRole(), message.getContent(), message.getCreateTime());
        }
    }
}
