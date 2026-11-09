package com.example.dgkl.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationReplyRequest(@NotBlank @Size(max = 5000) String reply) {
}
