package com.example.dgkl.ai.controller;

import com.example.dgkl.ai.dto.AiChatRequest;
import com.example.dgkl.ai.dto.AiChatResponse;
import com.example.dgkl.ai.dto.AiChatSessionDetailResponse;
import com.example.dgkl.ai.entity.AiChatSession;
import com.example.dgkl.ai.service.AiChatService;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return ApiResponse.success(aiChatService.chat(request));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResult<AiChatSession>> sessions(@RequestParam(defaultValue = "1") long pageNum,
                                                           @RequestParam(defaultValue = "30") long pageSize) {
        return ApiResponse.success(aiChatService.sessions(pageNum, pageSize));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<AiChatSessionDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(aiChatService.detail(id));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        aiChatService.delete(id);
        return ApiResponse.success();
    }
}
