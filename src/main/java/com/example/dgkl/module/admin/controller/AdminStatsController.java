package com.example.dgkl.module.admin.controller;

import com.example.dgkl.common.Result;
import com.example.dgkl.module.admin.service.AdminStatsService;
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
    public Result<Map<String, Long>> stats() {
        return Result.success(adminStatsService.stats());
    }
}
