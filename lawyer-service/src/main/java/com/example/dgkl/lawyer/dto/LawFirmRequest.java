package com.example.dgkl.lawyer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LawFirmRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 16) String provinceCode,
        @Size(max = 32) String provinceName,
        @Size(max = 64) String city,
        @Size(max = 255) String address,
        @Size(max = 32) String phone,
        @Email @Size(max = 128) String email,
        @Size(max = 255) String specialties,
        @Size(max = 4000) String description,
        @Size(max = 128) String licenseNo,
        @Min(0) @Max(1) Integer status) {
}
