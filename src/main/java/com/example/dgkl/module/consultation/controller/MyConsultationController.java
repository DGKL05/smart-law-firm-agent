package com.example.dgkl.module.consultation.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractUserCrudController;
import com.example.dgkl.module.consultation.entity.Consultation;
import com.example.dgkl.module.consultation.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/consultations")
@RequiredArgsConstructor
public class MyConsultationController extends AbstractUserCrudController<Consultation> {
    private final ConsultationService consultationService;

    @Override
    protected IService<Consultation> service() {
        return consultationService;
    }
}
