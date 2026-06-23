package com.example.dgkl.module.agent.service;

import com.example.dgkl.module.agent.dto.DifyChatRequest;
import com.example.dgkl.module.agent.dto.DifyChatResponse;

public interface DifyChatService {
    DifyChatResponse chat(DifyChatRequest request);
}
