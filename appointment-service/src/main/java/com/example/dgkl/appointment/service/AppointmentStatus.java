package com.example.dgkl.appointment.service;

import java.util.List;

public final class AppointmentStatus {
    public static final String BOOKING = "预约处理中";
    public static final String BOOKED = "已预约";
    public static final String CANCELLING = "取消处理中";
    public static final String CANCELLED = "已取消";
    public static final String REJECTED = "预约失败";

    public static final List<String> ACTIVE = List.of(BOOKING, BOOKED, CANCELLING);

    private AppointmentStatus() {
    }
}
