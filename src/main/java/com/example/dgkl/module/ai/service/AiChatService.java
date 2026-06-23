package com.example.dgkl.module.ai.service;

import com.example.dgkl.module.ai.dto.AiChatRequest;
import com.example.dgkl.module.ai.dto.AiChatResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
}
