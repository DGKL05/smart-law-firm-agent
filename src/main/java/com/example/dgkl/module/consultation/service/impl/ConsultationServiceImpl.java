package com.example.dgkl.module.consultation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.common.BusinessException;
import com.example.dgkl.module.common.EntityLifecycle;
import com.example.dgkl.module.consultation.entity.Consultation;
import com.example.dgkl.module.consultation.mapper.ConsultationMapper;
import com.example.dgkl.module.consultation.service.ConsultationService;
import org.springframework.stereotype.Service;

@Service
public class ConsultationServiceImpl extends ServiceImpl<ConsultationMapper, Consultation> implements ConsultationService {
    @Override
    public Consultation reply(Long id, String reply) {
        Consultation consultation = requireConsultation(id);
        consultation.setReply(reply);
        consultation.setStatus("已回复");
        EntityLifecycle.forUpdate(consultation);
        updateById(consultation);
        return getById(id);
    }

    @Override
    public Consultation close(Long id) {
        Consultation consultation = requireConsultation(id);
        consultation.setStatus("已关闭");
        EntityLifecycle.forUpdate(consultation);
        updateById(consultation);
        return getById(id);
    }

    private Consultation requireConsultation(Long id) {
        Consultation consultation = getById(id);
        if (consultation == null) {
            throw new BusinessException("咨询记录不存在");
        }
        return consultation;
    }
}
