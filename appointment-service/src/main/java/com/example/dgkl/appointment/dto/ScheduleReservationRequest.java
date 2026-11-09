package com.example.dgkl.appointment.dto;

import java.time.LocalDateTime;

public record ScheduleReservationRequest(Long appointmentId, Long lawyerId, LocalDateTime appointmentTime,
                                         boolean globalTransaction) {
    public ScheduleReservationRequest(Long appointmentId, Long lawyerId, LocalDateTime appointmentTime) {
        this(appointmentId, lawyerId, appointmentTime, false);
    }
}
