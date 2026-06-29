package com.example.dgkl.module.ai.controller;

import com.example.dgkl.common.Result;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.module.ai.dto.AiChatRequest;
import com.example.dgkl.module.ai.dto.AiChatResponse;
import com.example.dgkl.module.ai.dto.AiChatMessageResponse;
import com.example.dgkl.module.ai.dto.AiChatSessionDetailResponse;
import com.example.dgkl.module.ai.dto.AiChatSessionSummaryResponse;
import com.example.dgkl.module.ai.service.AiChatService;
import com.example.dgkl.module.ai.service.AiChatHistoryService;
import com.example.dgkl.common.BusinessException;
import com.example.dgkl.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    private final AiChatService aiChatService;
    private final AiChatHistoryService aiChatHistoryService;

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return Result.success(aiChatService.chat(request));
    }

    @GetMapping("/sessions")
    public Result<PageResult<AiChatSessionSummaryResponse>> sessions(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "30") long pageSize) {
        return Result.success(aiChatHistoryService.pageSessions(requireUserId(), pageNum, pageSize));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<AiChatSessionDetailResponse> sessionDetail(@PathVariable Long sessionId) {
        return Result.success(aiChatHistoryService.detail(requireUserId(), sessionId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AiChatMessageResponse>> sessionMessages(@PathVariable Long sessionId) {
        return Result.success(aiChatHistoryService.messages(requireUserId(), sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        aiChatHistoryService.deleteSession(requireUserId(), sessionId);
        return Result.success();
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录后再使用智能法律助理");
        }
        return userId;
    }
}
