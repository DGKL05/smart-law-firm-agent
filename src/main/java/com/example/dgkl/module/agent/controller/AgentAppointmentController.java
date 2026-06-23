package com.example.dgkl.module.agent.controller;

import com.example.dgkl.common.Result;
import com.example.dgkl.module.agent.dto.AgentAppointmentRequest;
import com.example.dgkl.module.agent.service.AgentAppointmentService;
import com.example.dgkl.module.appointment.entity.Appointment;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/appointments")
@RequiredArgsConstructor
public class AgentAppointmentController {
    private final AgentAppointmentService agentAppointmentService;

    @PostMapping
    public Result<Appointment> create(@RequestBody AgentAppointmentRequest request) {
        return Result.success(agentAppointmentService.create(request));
    }

    @PostMapping("/cancel")
    public Result<Appointment> cancel(@RequestBody AgentAppointmentRequest request) {
        return Result.success(agentAppointmentService.cancel(request));
    }
}
