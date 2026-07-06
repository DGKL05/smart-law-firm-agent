package com.example.dgkl.lawyer.dto;

import com.example.dgkl.lawyer.entity.Lawyer;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
public class LawyerResponse extends Lawyer {
    private String lawFirmName;
    private List<ScheduleSlotResponse> scheduleSlots;

    public static LawyerResponse of(Lawyer lawyer, String lawFirmName, List<ScheduleSlotResponse> slots) {
        LawyerResponse response = new LawyerResponse();
        BeanUtils.copyProperties(lawyer, response);
        response.setLawFirmName(lawFirmName);
        response.setScheduleSlots(slots);
        return response;
    }
}
