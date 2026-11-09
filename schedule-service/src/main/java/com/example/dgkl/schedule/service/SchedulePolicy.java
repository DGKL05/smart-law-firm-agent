package com.example.dgkl.schedule.service;

import com.example.dgkl.schedule.entity.ScheduleRule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

public final class SchedulePolicy {
    private static final List<TimeWindow> WEEKDAY_WINDOWS = List.of(
            new TimeWindow(LocalTime.of(9, 0), LocalTime.of(12, 0)),
            new TimeWindow(LocalTime.of(14, 0), LocalTime.of(17, 0)));

    public record TimeWindow(LocalTime startTime, LocalTime endTime) {
    }

    private SchedulePolicy() {
    }

    public static List<LocalDateTime> starts(LocalDate date, List<ScheduleRule> rules, boolean configured) {
        int weekday = date.getDayOfWeek().getValue();
        if (!configured) {
            return defaultWindows(weekday).stream()
                    .flatMap(window -> hours(date, window.startTime(), window.endTime()))
                    .toList();
        }
        return rules.stream()
                .filter(rule -> rule.getWeekday() == weekday && rule.getEnabled() == 1)
                .flatMap(rule -> hours(date, rule.getStartTime(), rule.getEndTime()))
                .distinct()
                .sorted()
                .toList();
    }

    public static List<TimeWindow> defaultWindows(int weekday) {
        return weekday >= 1 && weekday <= 5 ? WEEKDAY_WINDOWS : List.of();
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
