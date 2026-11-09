package com.example.dgkl.schedule.dto;

import java.util.List;

public record WeeklyScheduleResponse(String revision, boolean configured, List<WeeklyScheduleRule> rules) {
}
