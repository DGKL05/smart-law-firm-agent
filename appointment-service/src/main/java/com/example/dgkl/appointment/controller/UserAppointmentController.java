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
@RequestMapping("/api/me/appointments")
@RequiredArgsConstructor
public class UserAppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping
    public ApiResponse<PageResult<Appointment>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(appointmentService.page(Access.USER, null, null,
                pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Appointment> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(appointmentService.detail(Access.USER, id));
    }

    @PostMapping({"", "/book"})
    public ApiResponse<Appointment> book(@Valid @RequestBody AppointmentBookingRequest request) {
        return ApiResponse.success(appointmentService.book(Access.USER, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Appointment> update(@PathVariable @Positive Long id,
                                           @Valid @RequestBody AppointmentUpdateRequest request) {
        return ApiResponse.success(appointmentService.update(Access.USER, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        appointmentService.delete(Access.USER, id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Appointment> cancel(@PathVariable @Positive Long id) {
        return ApiResponse.success(appointmentService.cancel(Access.USER, id));
    }
}
