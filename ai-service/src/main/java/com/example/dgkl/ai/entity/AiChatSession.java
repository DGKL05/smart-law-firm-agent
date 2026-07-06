package com.example.dgkl.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_session")
public class AiChatSession extends BaseEntity {
    private Long userId;
    private String title;
    private String conversationId;
    private String lastMessage;
}
