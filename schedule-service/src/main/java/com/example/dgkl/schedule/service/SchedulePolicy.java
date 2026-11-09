package com.example.dgkl.schedule.service;

import com.example.dgkl.schedule.entity.ScheduleRule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

public final class SchedulePolicy {
    private SchedulePolicy() {
    }

    public static List<LocalDateTime> starts(LocalDate date, List<ScheduleRule> rules, boolean configured) {
        int weekday = date.getDayOfWeek().getValue();
        if (!configured) {
            if (weekday > 5) {
                return List.of();
            }
            return Stream.concat(hours(date, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                    hours(date, LocalTime.of(14, 0), LocalTime.of(17, 0))).toList();
        }
        return rules.stream()
                .filter(rule -> rule.getWeekday() == weekday && rule.getEnabled() == 1)
                .flatMap(rule -> hours(date, rule.getStartTime(), rule.getEndTime()))
                .distinct()
                .sorted()
                .toList();
    }

    public static boolean overlaps(LocalDateTime firstStart, LocalDateTime firstEnd,
                            LocalDateTime secondStart, LocalDateTime secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    private static Stream<LocalDateTime> hours(LocalDate date, LocalTime start, LocalTime end) {
        LocalDateTime endTime = date.atTime(end);
        return Stream.iterate(date.atTime(start), slot -> !slot.plusHours(1).isAfter(endTime),
                slot -> slot.plusHours(1));
    }
}
