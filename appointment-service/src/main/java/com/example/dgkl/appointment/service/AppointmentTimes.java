package com.example.dgkl.appointment.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class AppointmentTimes {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    public static final int SLOT_DURATION_MINUTES = 60;

    private AppointmentTimes() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    public static LocalDateTime slotEnd(LocalDateTime start) {
        return start.plusMinutes(SLOT_DURATION_MINUTES);
    }
}
