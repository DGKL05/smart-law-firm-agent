package com.example.dgkl.module.consultation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.consultation.entity.Consultation;

public interface ConsultationService extends IService<Consultation> {
    Consultation reply(Long id, String reply);

    Consultation close(Long id);
}
