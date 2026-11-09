package com.example.dgkl.schedule.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ScheduleSlotResponse(LocalDate date, LocalDateTime startTime, LocalDateTime endTime,
                                   boolean available, String unavailableReason) {
}
