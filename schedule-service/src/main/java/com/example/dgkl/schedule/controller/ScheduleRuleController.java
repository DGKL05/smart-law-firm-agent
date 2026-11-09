package com.example.dgkl.schedule.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.schedule.dto.ScheduleRuleRequest;
import com.example.dgkl.schedule.entity.ScheduleRule;
import com.example.dgkl.schedule.service.ScheduleLawyers;
import com.example.dgkl.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/{scope:admin|lawyer}/schedules")
@RequiredArgsConstructor
public class ScheduleRuleController {
    private final ScheduleService scheduleService;
    private final ScheduleLawyers lawyers;

    @GetMapping
    public ApiResponse<PageResult<ScheduleRule>> page(@PathVariable String scope,
            @RequestParam(required = false) @Positive Long lawyerId,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(scheduleService.rules(owner(scope), lawyerId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScheduleRule> detail(@PathVariable String scope, @PathVariable @Positive Long id) {
        return ApiResponse.success(scheduleService.rule(id, owner(scope)));
    }

    @PostMapping
    public ApiResponse<ScheduleRule> create(@PathVariable String scope,
                                            @Valid @RequestBody ScheduleRuleRequest request) {
        return ApiResponse.success(scheduleService.createRule(request, owner(scope)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ScheduleRule> update(@PathVariable String scope, @PathVariable @Positive Long id,
            @Valid @RequestBody ScheduleRuleRequest request) {
        return ApiResponse.success(scheduleService.updateRule(id, request, owner(scope)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String scope, @PathVariable @Positive Long id) {
        scheduleService.deleteRule(id, owner(scope));
        return ApiResponse.success();
    }

    private Long owner(String scope) {
        return "lawyer".equals(scope) ? lawyers.currentId() : null;
    }
}
