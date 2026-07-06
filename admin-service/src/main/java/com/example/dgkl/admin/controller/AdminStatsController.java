package com.example.dgkl.admin.controller;

import com.example.dgkl.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {
    @GetMapping
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(Map.of("service", "admin-service", "status", "ready"));
    }
}
