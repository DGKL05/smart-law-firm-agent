package com.example.dgkl.module.appointment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.appointment.entity.Appointment;

public interface AppointmentService extends IService<Appointment> {
    void completeExpiredAppointments();

    void completeExpiredAppointmentsForUser(Long userId);
}
