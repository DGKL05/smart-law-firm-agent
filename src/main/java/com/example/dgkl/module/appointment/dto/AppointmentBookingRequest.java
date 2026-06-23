package com.example.dgkl.module.appointment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentBookingRequest {
    private Long lawyerId;
    private LocalDateTime appointmentTime;
    private String remark;
}
