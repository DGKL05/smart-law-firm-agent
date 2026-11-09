package com.example.dgkl.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ScheduleBlockRequest(@Positive Long lawyerId,
                                   @NotNull LocalDateTime startTime,
                                   @NotNull LocalDateTime endTime,
                                   @Size(max = 256) String reason) {
}
