package com.example.dgkl.lawyer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LawyerAdminRequest extends LawyerProfileRequest {
    @NotNull
    @Positive
    private Long lawFirmId;
    @Positive
    private Long userId;
    @Min(0)
    @Max(1)
    private Integer status;
}
