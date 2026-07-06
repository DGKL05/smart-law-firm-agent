package com.example.dgkl.lawyer.dto;

import com.example.dgkl.lawyer.entity.Lawyer;
import lombok.Data;

@Data
public class LawyerSnapshotResponse {
    private Long id;
    private Long lawFirmId;
    private String name;
    private String lawFirmName;
    private Integer status;

    public static LawyerSnapshotResponse of(Lawyer lawyer, String lawFirmName) {
        LawyerSnapshotResponse response = new LawyerSnapshotResponse();
        response.setId(lawyer.getId());
        response.setLawFirmId(lawyer.getLawFirmId());
        response.setName(lawyer.getName());
        response.setLawFirmName(lawFirmName);
        response.setStatus(lawyer.getStatus());
        return response;
    }
}
