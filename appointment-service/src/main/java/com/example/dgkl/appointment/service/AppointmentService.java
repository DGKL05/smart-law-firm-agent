package com.example.dgkl.appointment.service;

import com.example.dgkl.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.appointment.dto.AppointmentUpdateRequest;
import com.example.dgkl.appointment.dto.ScheduleSlotResponse;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.common.response.PageResult;

import java.util.List;

public interface AppointmentService {
    enum Access {
        USER, LAWYER, ADMIN
    }

    PageResult<Appointment> page(Access access, Long userId, Long lawyerId, long pageNum, long pageSize);

    Appointment detail(Access access, Long id);

    Appointment book(Access access, AppointmentBookingRequest request);

    Appointment update(Access access, Long id, AppointmentUpdateRequest request);

    Appointment cancel(Access access, Long id);

    void delete(Access access, Long id);

    List<ScheduleSlotResponse> schedule(Long lawyerId);

    boolean hasActiveWork(Long lawyerId);

    long count();
}
