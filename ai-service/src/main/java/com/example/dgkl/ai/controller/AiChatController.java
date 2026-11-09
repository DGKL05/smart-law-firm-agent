package com.example.dgkl.ai.controller;

import com.example.dgkl.ai.dto.AiChatRequest;
import com.example.dgkl.ai.dto.AiChatResponse;
import com.example.dgkl.ai.dto.AiChatSessionDetailResponse;
import com.example.dgkl.ai.dto.AiChatSessionRequest;
import com.example.dgkl.ai.dto.AiChatSessionView;
import com.example.dgkl.ai.service.AiChatService;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.success(aiChatService.chat(request));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResult<AiChatSessionView>> sessions(
            @RequestParam(defaultValue = "1") @Min(1) @Max(Integer.MAX_VALUE) long pageNum,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(aiChatService.sessions(pageNum, pageSize));
    }

    @PostMapping("/sessions")
    public ApiResponse<AiChatSessionView> create(@Valid @RequestBody AiChatSessionRequest request) {
        return ApiResponse.success(aiChatService.create(request.title()));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<AiChatSessionDetailResponse> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(aiChatService.detail(id));
    }

    @PutMapping("/sessions/{id}")
    public ApiResponse<AiChatSessionView> rename(@PathVariable @Positive Long id,
                                                @Valid @RequestBody AiChatSessionRequest request) {
        return ApiResponse.success(aiChatService.rename(id, request.title()));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        aiChatService.delete(id);
        return ApiResponse.success();
    }
}
