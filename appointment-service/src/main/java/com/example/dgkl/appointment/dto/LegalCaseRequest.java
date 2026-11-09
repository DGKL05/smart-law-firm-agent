package com.example.dgkl.appointment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record LegalCaseRequest(@NotNull @Positive Long userId,
                               @Positive Long lawyerId,
                               @NotBlank @Size(max = 64) String caseNo,
                               @NotBlank @Size(max = 128) String title,
                               @Size(max = 64) String caseType,
                               @Size(max = 5000) String description,
                               @JsonAlias("caseTime") @NotNull LocalDateTime startTime,
                               LocalDateTime endTime,
                               @Pattern(regexp = "ACTIVE|CLOSED") String status) {
}
