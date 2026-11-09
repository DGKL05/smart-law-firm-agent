package com.example.dgkl.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ScheduleReservationRequest(@NotNull @Positive Long appointmentId,
                                         @NotNull @Positive Long lawyerId,
                                         @NotNull LocalDateTime appointmentTime,
                                         boolean globalTransaction) {
    public ScheduleReservationRequest(Long appointmentId, Long lawyerId, LocalDateTime appointmentTime) {
        this(appointmentId, lawyerId, appointmentTime, false);
    }
}
