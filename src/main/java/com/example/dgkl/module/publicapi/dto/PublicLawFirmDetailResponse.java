package com.example.dgkl.module.publicapi.dto;

import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import lombok.Data;

import java.util.List;

@Data
public class PublicLawFirmDetailResponse {
    private Long id;
    private String name;
    private String provinceCode;
    private String provinceName;
    private String city;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private String description;
    private String specialties;
    private String licenseNo;
    private Integer status;
    private List<PublicLawyerResponse> lawyers;

    public static PublicLawFirmDetailResponse of(LawFirm lawFirm, List<Lawyer> lawyers) {
        PublicLawFirmDetailResponse response = new PublicLawFirmDetailResponse();
        response.setId(lawFirm.getId());
        response.setName(lawFirm.getName());
        response.setProvinceCode(lawFirm.getProvinceCode());
        response.setProvinceName(lawFirm.getProvinceName());
        response.setCity(lawFirm.getCity());
        response.setAddress(lawFirm.getAddress());
        response.setPhone(lawFirm.getPhone());
        response.setEmail(lawFirm.getEmail());
        response.setLogoUrl(lawFirm.getLogoUrl());
        response.setDescription(lawFirm.getDescription());
        response.setSpecialties(lawFirm.getSpecialties());
        response.setLicenseNo(lawFirm.getLicenseNo());
        response.setStatus(lawFirm.getStatus());
        response.setLawyers(lawyers.stream()
                .map(lawyer -> PublicLawyerResponse.of(lawyer, lawFirm.getName()))
                .toList());
        return response;
    }
}
