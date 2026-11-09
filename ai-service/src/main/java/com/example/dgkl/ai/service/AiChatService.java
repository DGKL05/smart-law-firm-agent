package com.example.dgkl.ai.service;

import com.example.dgkl.ai.dto.AiChatRequest;
import com.example.dgkl.ai.dto.AiChatResponse;
import com.example.dgkl.ai.dto.AiChatSessionDetailResponse;
import com.example.dgkl.ai.dto.AiChatSessionView;
import com.example.dgkl.common.response.PageResult;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);

    PageResult<AiChatSessionView> sessions(long pageNum, long pageSize);

    AiChatSessionView create(String title);

    AiChatSessionDetailResponse detail(Long id);

    AiChatSessionView rename(Long id, String title);

    void delete(Long id);

    long countSessions();
}
