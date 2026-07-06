package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.dto.ScheduleSlotResponse;
import com.example.dgkl.appointment.service.AppointmentService;
import com.example.dgkl.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/schedule")
@RequiredArgsConstructor
public class InternalScheduleController {
    private final AppointmentService appointmentService;

    @GetMapping("/lawyers/{lawyerId}")
    public ApiResponse<List<ScheduleSlotResponse>> schedule(@PathVariable Long lawyerId) {
        return ApiResponse.success(appointmentService.schedule(lawyerId));
    }
}
