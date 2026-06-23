package com.example.dgkl.module.publicapi.dto;

import com.example.dgkl.module.appointment.dto.AppointmentScheduleSlot;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawyer.entity.Lawyer;

import java.util.List;

public class PublicLawyerDetailResponse extends PublicLawyerResponse {
    public static PublicLawyerDetailResponse of(Lawyer lawyer, LawFirm lawFirm, List<AppointmentScheduleSlot> slots) {
        PublicLawyerDetailResponse response = new PublicLawyerDetailResponse();
        response.setId(lawyer.getId());
        response.setLawFirmId(lawyer.getLawFirmId());
        response.setLawFirmName(lawFirm == null ? null : lawFirm.getName());
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
        response.setScheduleSlots(slots);
        return response;
    }
}
