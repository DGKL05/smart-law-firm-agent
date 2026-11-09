package com.example.dgkl.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConsultationRequest(@Positive Long userId, @Positive Long lawyerId, @Positive Long lawFirmId,
                                  @NotBlank @Size(max = 128) String title,
                                  @NotBlank @Size(max = 5000) String question) {
}
