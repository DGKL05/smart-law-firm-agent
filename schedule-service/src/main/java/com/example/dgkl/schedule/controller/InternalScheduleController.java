package com.example.dgkl.schedule.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.schedule.dto.ScheduleReservationRequest;
import com.example.dgkl.schedule.dto.ScheduleSlotResponse;
import com.example.dgkl.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
public class InternalScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping("/internal/schedule/lawyers/{lawyerId}")
    public ApiResponse<List<ScheduleSlotResponse>> slots(@PathVariable @Positive Long lawyerId) {
        return ApiResponse.success(scheduleService.slots(lawyerId));
    }

    @PostMapping("/internal/schedule/reservations")
    public ApiResponse<Boolean> reserve(@Valid @RequestBody ScheduleReservationRequest request) {
        return ApiResponse.success(scheduleService.reserve(request));
    }

    @PostMapping("/internal/schedule/reservations/release")
    public ApiResponse<Void> release(@Valid @RequestBody ScheduleReservationRequest request) {
        scheduleService.release(request);
        return ApiResponse.success();
    }

    @GetMapping("/internal/admin/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(Map.of("scheduleCount", scheduleService.ruleCount()));
    }
}
