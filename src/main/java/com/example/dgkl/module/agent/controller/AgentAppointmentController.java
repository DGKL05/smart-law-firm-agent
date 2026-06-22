package com.example.dgkl.module.agent.controller;

import com.example.dgkl.common.Result;
import com.example.dgkl.module.appointment.controller.MyAppointmentController;
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
    private final MyAppointmentController myAppointmentController;

    @PostMapping
    public Result<Appointment> create(@RequestBody Appointment appointment) {
        return myAppointmentController.create(appointment);
    }

    @PostMapping("/cancel")
    public Result<Appointment> cancel(@RequestBody Appointment appointment) {
        return myAppointmentController.cancel(appointment.getId());
    }
}
