package com.example.dgkl.module.appointment.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
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
            wrapper.like("law_firm_name", keyword).or().like("lawyer_name", keyword);
        }
        return wrapper;
    }
}
