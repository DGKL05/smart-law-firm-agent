package com.example.dgkl.module.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DifyIntentResponse {
    private String intent;
    private String appointmentTime;
    private String lawFirmName;
    private String lawyerName;
    private String remark;
    private List<String> missingFields = new ArrayList<>();
    private String reply;
}
