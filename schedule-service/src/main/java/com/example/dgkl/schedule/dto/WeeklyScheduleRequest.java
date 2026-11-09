package com.example.dgkl.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WeeklyScheduleRequest(@NotNull @Pattern(regexp = "[0-9a-f]{64}") String revision,
                                    @NotNull @Size(max = 168) List<@NotNull @Valid WeeklyScheduleRule> rules) {
}
