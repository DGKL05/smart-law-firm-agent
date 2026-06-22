package com.example.dgkl.module.appointment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.mapper.AppointmentMapper;
import com.example.dgkl.module.appointment.service.AppointmentService;
import org.springframework.stereotype.Service;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {
}
