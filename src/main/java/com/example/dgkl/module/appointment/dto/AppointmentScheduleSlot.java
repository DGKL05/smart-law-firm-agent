package com.example.dgkl.module.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentScheduleSlot {
    private LocalDate date;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean available;
    private String unavailableReason;
}
