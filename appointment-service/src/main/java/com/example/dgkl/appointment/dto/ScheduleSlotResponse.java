package com.example.dgkl.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSlotResponse {
    private LocalDate date;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean available;
    private String unavailableReason;
}
