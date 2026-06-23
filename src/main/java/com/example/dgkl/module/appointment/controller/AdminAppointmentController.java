package com.example.dgkl.module.appointment.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    protected QueryWrapper<Appointment> pageQuery(String keyword) {
        QueryWrapper<Appointment> wrapper = new QueryWrapper<Appointment>().orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(item -> item.like("law_firm_name", keyword).or().like("lawyer_name", keyword));
        }
        return wrapper;
    }

    @Override
    @GetMapping
    public Result<PageResult<Appointment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String keyword) {
        appointmentService.completeExpiredAppointments();
        return Result.success(PageResult.of(appointmentService.page(new Page<>(pageNum, pageSize), pageQuery(keyword))));
    }
}
