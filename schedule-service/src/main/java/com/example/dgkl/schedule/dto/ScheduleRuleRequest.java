package com.example.dgkl.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

public record ScheduleRuleRequest(@Positive Long lawyerId,
                                  @NotNull @Min(1) @Max(7) Integer weekday,
                                  @NotNull LocalTime startTime,
                                  @NotNull LocalTime endTime,
                                  @NotNull @Min(0) @Max(1) Integer enabled) {
}
