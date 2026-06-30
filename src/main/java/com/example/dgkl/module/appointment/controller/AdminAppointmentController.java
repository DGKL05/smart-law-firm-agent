package com.example.dgkl.module.appointment.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController extends AbstractAdminCrudController<Appointment> {
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
        return Result.success(appointmentService.adminPage(pageNum, pageSize, keyword));
    }
}
