package com.example.dgkl.module.publicapi.dto;

import com.example.dgkl.module.appointment.dto.AppointmentScheduleSlot;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PublicLawyerResponse {
    private Long id;
    private Long lawFirmId;
    private String lawFirmName;
    private String name;
    private String gender;
    private String avatarUrl;
    private String phone;
    private String email;
    private String category;
    private String title;
    private Integer experienceYears;
    private String description;
    private String goodAt;
    private String availableTimeSlots;
    private Integer status;
    private List<AppointmentScheduleSlot> scheduleSlots = new ArrayList<>();

    public static PublicLawyerResponse of(Lawyer lawyer, String lawFirmName) {
        PublicLawyerResponse response = new PublicLawyerResponse();
        response.setId(lawyer.getId());
        response.setLawFirmId(lawyer.getLawFirmId());
        response.setLawFirmName(lawFirmName);
        response.setName(lawyer.getName());
        response.setGender(lawyer.getGender());
        response.setAvatarUrl(lawyer.getAvatarUrl());
        response.setPhone(lawyer.getPhone());
        response.setEmail(lawyer.getEmail());
        response.setCategory(lawyer.getCategory());
        response.setTitle(lawyer.getTitle());
        response.setExperienceYears(lawyer.getExperienceYears());
        response.setDescription(lawyer.getDescription());
        response.setGoodAt(lawyer.getGoodAt());
        response.setAvailableTimeSlots(lawyer.getAvailableTimeSlots());
        response.setStatus(lawyer.getStatus());
        return response;
    }
}
