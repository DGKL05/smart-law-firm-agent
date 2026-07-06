package com.example.dgkl.appointment.dto;

import lombok.Data;

@Data
public class LawyerSnapshotResponse {
    private Long id;
    private Long lawFirmId;
    private String name;
    private String lawFirmName;
    private Integer status;
}
