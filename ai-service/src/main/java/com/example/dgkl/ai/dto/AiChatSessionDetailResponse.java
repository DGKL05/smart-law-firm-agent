package com.example.dgkl.ai.dto;

import com.example.dgkl.ai.entity.AiChatMessage;
import com.example.dgkl.ai.entity.AiChatSession;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
public class AiChatSessionDetailResponse extends AiChatSession {
    private List<AiChatMessage> messages;

    public static AiChatSessionDetailResponse of(AiChatSession session, List<AiChatMessage> messages) {
        AiChatSessionDetailResponse response = new AiChatSessionDetailResponse();
        BeanUtils.copyProperties(session, response);
        response.setMessages(messages);
        return response;
    }
}
