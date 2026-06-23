package com.example.dgkl.module.appointment.service;

import java.time.Duration;
import java.time.LocalDateTime;

public final class AppointmentStatusPolicy {
    public static final String STATUS_BOOKED = "已预约";
    public static final String STATUS_CANCELLED = "已取消";
    public static final String STATUS_COMPLETED = "已完成";

    private static final Duration CONSULTATION_DURATION = Duration.ofHours(1);

    private AppointmentStatusPolicy() {
    }

    public static boolean isBookableStart(LocalDateTime startTime, LocalDateTime now) {
        return startTime != null && startTime.isAfter(now);
    }

    public static boolean isExpired(LocalDateTime appointmentTime, LocalDateTime now) {
        return appointmentTime != null && !appointmentTime.plus(CONSULTATION_DURATION).isAfter(now);
    }
}
