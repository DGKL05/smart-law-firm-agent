package com.example.dgkl.lawyer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LawyerProfileRequest {
    @NotBlank
    @Size(max = 64)
    private String name;
    @Size(max = 16)
    private String gender;
    @Size(max = 32)
    private String phone;
    @Email
    @Size(max = 128)
    private String email;
    @Size(max = 64)
    private String category;
    @Size(max = 64)
    private String title;
    @Min(0)
    @Max(80)
    private Integer experienceYears;
    @Size(max = 4000)
    private String description;
    @Size(max = 255)
    private String goodAt;
}
