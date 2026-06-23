package com.example.dgkl.module.agent.controller;

import com.example.dgkl.common.Result;
import com.example.dgkl.module.agent.dto.DifyChatRequest;
import com.example.dgkl.module.agent.dto.DifyChatResponse;
import com.example.dgkl.module.agent.service.DifyChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/agent")
@RequiredArgsConstructor
public class DifyChatController {
    private final DifyChatService difyChatService;

    @PostMapping("/chat")
    public Result<DifyChatResponse> chat(@RequestBody DifyChatRequest request) {
        return Result.success(difyChatService.chat(request));
    }
}
