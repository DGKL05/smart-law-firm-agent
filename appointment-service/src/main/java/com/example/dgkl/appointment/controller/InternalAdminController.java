package com.example.dgkl.appointment.controller;

import com.example.dgkl.appointment.service.AppointmentService;
import com.example.dgkl.appointment.service.ConsultationService;
import com.example.dgkl.appointment.service.LegalCaseService;
import com.example.dgkl.common.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InternalAdminController {
    private final AppointmentService appointmentService;
    private final ConsultationService consultationService;
    private final LegalCaseService legalCaseService;

    @GetMapping("/internal/admin/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(Map.of(
                "appointmentCount", appointmentService.count(),
                "consultationCount", consultationService.count(),
                "caseCount", legalCaseService.count()));
    }

    @GetMapping("/internal/appointments/lawyers/{lawyerId}/has-active-work")
    public ApiResponse<Boolean> hasActiveWork(@PathVariable @Positive Long lawyerId) {
        return ApiResponse.success(appointmentService.hasActiveWork(lawyerId));
    }
}
