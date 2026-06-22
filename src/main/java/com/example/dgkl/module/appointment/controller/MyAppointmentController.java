package com.example.dgkl.module.appointment.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.common.AbstractUserCrudController;
import com.example.dgkl.module.common.EntityLifecycle;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    protected QueryWrapper<Appointment> userQuery(Long userId, String keyword) {
        QueryWrapper<Appointment> wrapper = new QueryWrapper<Appointment>().eq("user_id", userId).orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("law_firm_name", keyword).or().like("lawyer_name", keyword);
        }
        return wrapper;
    }

    @PutMapping("/{id}/cancel")
    public Result<Appointment> cancel(@PathVariable Long id) {
        Appointment appointment = requireOwnRecord(id);
        appointment.setStatus("已取消");
        EntityLifecycle.forUpdate(appointment);
        appointmentService.updateById(appointment);
        return Result.success(appointmentService.getById(id));
    }
}
