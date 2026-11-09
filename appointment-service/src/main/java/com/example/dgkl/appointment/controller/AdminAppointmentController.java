package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.appointment.dto.AppointmentUpdateRequest;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.appointment.service.AppointmentService;
import com.example.dgkl.appointment.service.AppointmentService.Access;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
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
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping
    public ApiResponse<PageResult<Appointment>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Positive Long lawyerId) {
        return ApiResponse.success(appointmentService.page(Access.ADMIN, userId, lawyerId,
                pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Appointment> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(appointmentService.detail(Access.ADMIN, id));
    }

    @PostMapping
    public ApiResponse<Appointment> book(@Valid @RequestBody AppointmentBookingRequest request) {
        return ApiResponse.success(appointmentService.book(Access.ADMIN, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Appointment> update(@PathVariable @Positive Long id,
                                           @Valid @RequestBody AppointmentUpdateRequest request) {
        return ApiResponse.success(appointmentService.update(Access.ADMIN, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        appointmentService.delete(Access.ADMIN, id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Appointment> cancel(@PathVariable @Positive Long id) {
        return ApiResponse.success(appointmentService.cancel(Access.ADMIN, id));
    }
}
