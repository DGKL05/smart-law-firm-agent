package com.example.dgkl.lawyer.dto;

import lombok.Data;

@Data
public class ScheduleSlotResponse {
    private String date;
    private String startTime;
    private String endTime;
    private Boolean available;
    private String unavailableReason;
}
