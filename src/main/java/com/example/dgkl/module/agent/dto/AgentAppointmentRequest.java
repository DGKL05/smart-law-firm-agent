package com.example.dgkl.module.agent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentAppointmentRequest {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime appointmentTime;
    private String lawFirmName;
    private String lawyerName;
    private String remark;
}
