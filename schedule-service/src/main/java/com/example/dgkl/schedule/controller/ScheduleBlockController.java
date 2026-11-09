package com.example.dgkl.schedule.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.schedule.dto.ScheduleBlockRequest;
import com.example.dgkl.schedule.entity.ScheduleBlock;
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
@RequestMapping("/api/{scope:admin|lawyer}/schedule-blocks")
@RequiredArgsConstructor
public class ScheduleBlockController {
    private final ScheduleService scheduleService;
    private final ScheduleLawyers lawyers;

    @GetMapping
    public ApiResponse<PageResult<ScheduleBlock>> page(@PathVariable String scope,
            @RequestParam(required = false) @Positive Long lawyerId,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(scheduleService.blocks(owner(scope), lawyerId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScheduleBlock> detail(@PathVariable String scope, @PathVariable @Positive Long id) {
        return ApiResponse.success(scheduleService.block(id, owner(scope)));
    }

    @PostMapping
    public ApiResponse<ScheduleBlock> create(@PathVariable String scope,
                                             @Valid @RequestBody ScheduleBlockRequest request) {
        return ApiResponse.success(scheduleService.createBlock(request, owner(scope)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ScheduleBlock> update(@PathVariable String scope, @PathVariable @Positive Long id,
            @Valid @RequestBody ScheduleBlockRequest request) {
        return ApiResponse.success(scheduleService.updateBlock(id, request, owner(scope)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String scope, @PathVariable @Positive Long id) {
        scheduleService.deleteBlock(id, owner(scope));
        return ApiResponse.success();
    }

    private Long owner(String scope) {
        return "lawyer".equals(scope) ? lawyers.currentId() : null;
    }
}
