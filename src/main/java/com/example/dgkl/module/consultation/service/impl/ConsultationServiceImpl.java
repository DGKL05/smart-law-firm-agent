package com.example.dgkl.module.consultation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.consultation.entity.Consultation;
import com.example.dgkl.module.consultation.mapper.ConsultationMapper;
import com.example.dgkl.module.consultation.service.ConsultationService;
import org.springframework.stereotype.Service;

@Service
public class ConsultationServiceImpl extends ServiceImpl<ConsultationMapper, Consultation> implements ConsultationService {
}
