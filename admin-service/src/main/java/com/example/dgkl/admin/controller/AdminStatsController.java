package com.example.dgkl.admin.controller;

import com.example.dgkl.admin.service.AdminStatsService;
import com.example.dgkl.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {
    private final AdminStatsService adminStatsService;

    @GetMapping
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(adminStatsService.overview());
    }
}
