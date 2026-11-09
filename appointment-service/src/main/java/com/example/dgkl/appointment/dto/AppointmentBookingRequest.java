package com.example.dgkl.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentBookingRequest {
    @Positive
    private Long userId;
    @NotNull
    @Positive
    private Long lawyerId;
    @NotNull
    private LocalDateTime appointmentTime;
    @Size(max = 500)
    private String remark;
}
