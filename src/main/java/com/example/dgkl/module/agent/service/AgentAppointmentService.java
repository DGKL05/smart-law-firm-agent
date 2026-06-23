package com.example.dgkl.module.agent.service;

import com.example.dgkl.module.agent.dto.AgentAppointmentRequest;
import com.example.dgkl.module.appointment.entity.Appointment;

public interface AgentAppointmentService {
    Appointment create(AgentAppointmentRequest request);

    Appointment cancel(AgentAppointmentRequest request);
}
