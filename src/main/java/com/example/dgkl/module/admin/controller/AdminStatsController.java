package com.example.dgkl.module.admin.controller;

import com.example.dgkl.common.Result;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.consultation.service.ConsultationService;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import com.example.dgkl.module.lawyer.service.LawyerService;
import com.example.dgkl.module.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {
    private final SysUserService userService;
    private final LawFirmService lawFirmService;
    private final LawyerService lawyerService;
    private final ConsultationService consultationService;
    private final AppointmentService appointmentService;

    @GetMapping
    public Result<Map<String, Long>> stats() {
        return Result.success(Map.of(
                "userCount", userService.count(),
                "lawFirmCount", lawFirmService.count(),
                "lawyerCount", lawyerService.count(),
                "consultationCount", consultationService.count(),
                "appointmentCount", appointmentService.count()));
    }
}
