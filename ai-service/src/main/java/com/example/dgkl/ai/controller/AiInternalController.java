package com.example.dgkl.ai.controller;

import com.example.dgkl.ai.service.AiChatService;
import com.example.dgkl.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/admin")
@RequiredArgsConstructor
public class AiInternalController {
    private final AiChatService aiChatService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(Map.of("sessionCount", aiChatService.countSessions()));
    }
}
