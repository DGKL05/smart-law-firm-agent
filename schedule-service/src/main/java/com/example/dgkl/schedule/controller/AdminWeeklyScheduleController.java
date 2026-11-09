package com.example.dgkl.schedule.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.schedule.dto.WeeklyScheduleRequest;
import com.example.dgkl.schedule.dto.WeeklyScheduleResponse;
import com.example.dgkl.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/schedules/weekly")
@RequiredArgsConstructor
public class AdminWeeklyScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping
    public ApiResponse<WeeklyScheduleResponse> weekly(@RequestParam @Positive Long lawyerId) {
        return ApiResponse.success(scheduleService.weeklyRules(lawyerId));
    }

    @PutMapping
    public ApiResponse<WeeklyScheduleResponse> replace(@RequestParam @Positive Long lawyerId,
            @Valid @RequestBody WeeklyScheduleRequest request) {
        return ApiResponse.success(scheduleService.replaceWeeklyRules(lawyerId, request));
    }
}
