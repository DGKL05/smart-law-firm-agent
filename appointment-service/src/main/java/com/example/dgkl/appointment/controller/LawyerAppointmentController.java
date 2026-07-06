package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.appointment.service.AppointmentService;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lawyer/appointments")
@RequiredArgsConstructor
public class LawyerAppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping
    public ApiResponse<PageResult<Appointment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(appointmentService.lawyerPage(pageNum, pageSize));
    }
}
