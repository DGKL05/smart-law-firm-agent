package com.example.dgkl.module.appointment.service;

import com.example.dgkl.module.appointment.dto.AppointmentScheduleSlot;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentScheduleService {
    List<AppointmentScheduleSlot> upcomingSlots(Long lawyerId);

    boolean isSlotAvailable(Long lawyerId, LocalDateTime appointmentTime);
}
