package com.example.dgkl.module.appointment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.module.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.module.appointment.entity.Appointment;

public interface AppointmentService extends IService<Appointment> {
    PageResult<Appointment> adminPage(long pageNum, long pageSize, String keyword);

    PageResult<Appointment> currentUserPage(long pageNum, long pageSize, String keyword);

    Appointment bookForCurrentUser(AppointmentBookingRequest request);

    Appointment createForCurrentUser(Appointment appointment);

    Appointment cancelForCurrentUser(Long id);

    void completeExpiredAppointments();

    void completeExpiredAppointmentsForUser(Long userId);
}
