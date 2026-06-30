package com.example.dgkl.module.appointment.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.common.AbstractUserCrudController;
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
public class MyAppointmentController extends AbstractUserCrudController<Appointment> {
    private final AppointmentService appointmentService;

    @Override
    protected IService<Appointment> service() {
        return appointmentService;
    }

    @Override
    @GetMapping
    public Result<PageResult<Appointment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String keyword) {
        return Result.success(appointmentService.currentUserPage(pageNum, pageSize, keyword));
    }

    @PostMapping("/book")
    public Result<Appointment> book(@RequestBody AppointmentBookingRequest request) {
        return Result.success(appointmentService.bookForCurrentUser(request));
    }

    @Override
    @PostMapping
    public Result<Appointment> create(@RequestBody Appointment appointment) {
        return Result.success(appointmentService.createForCurrentUser(appointment));
    }

    @PutMapping("/{id}/cancel")
    public Result<Appointment> cancel(@PathVariable Long id) {
        return Result.success(appointmentService.cancelForCurrentUser(id));
    }
}
