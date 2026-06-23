package com.example.dgkl.module.ai.controller;

import com.example.dgkl.common.Result;
import com.example.dgkl.module.ai.dto.AiChatRequest;
import com.example.dgkl.module.ai.dto.AiChatResponse;
import com.example.dgkl.module.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return Result.success(aiChatService.chat(request));
    }
}
