package com.example.dgkl.module.appointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.mapper.AppointmentMapper;
import com.example.dgkl.module.appointment.service.AppointmentStatusPolicy;
import com.example.dgkl.module.appointment.service.AppointmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {
    @Override
    public void completeExpiredAppointments() {
        completeExpiredAppointments(null);
    }

    @Override
    public void completeExpiredAppointmentsForUser(Long userId) {
        if (userId == null) {
            return;
        }
        completeExpiredAppointments(userId);
    }

    private void completeExpiredAppointments(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<Appointment> wrapper = new QueryWrapper<Appointment>()
                .eq("status", AppointmentStatusPolicy.STATUS_BOOKED)
                .le("appointment_time", now.minusHours(1));
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        List<Appointment> expired = list(wrapper);
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(appointment -> {
            if (AppointmentStatusPolicy.isExpired(appointment.getAppointmentTime(), now)) {
                appointment.setStatus(AppointmentStatusPolicy.STATUS_COMPLETED);
                appointment.setUpdateTime(now);
            }
        });
        updateBatchById(expired);
    }
}
