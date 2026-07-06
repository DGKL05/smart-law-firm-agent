package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.appointment.service.AppointmentService;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/appointments")
@RequiredArgsConstructor
public class UserAppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping
    public ApiResponse<PageResult<Appointment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.success(appointmentService.userPage(pageNum, pageSize));
    }

    @PostMapping("/book")
    public ApiResponse<Appointment> book(@RequestBody AppointmentBookingRequest request) {
        return ApiResponse.success(appointmentService.book(request));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Appointment> cancel(@PathVariable Long id) {
        return ApiResponse.success(appointmentService.cancel(id));
    }
}
