package com.example.dgkl.module.consultation.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import com.example.dgkl.module.common.EntityLifecycle;
import com.example.dgkl.module.consultation.entity.Consultation;
import com.example.dgkl.module.consultation.service.ConsultationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/consultations")
@RequiredArgsConstructor
public class AdminConsultationController extends AbstractAdminCrudController<Consultation> {
    private final ConsultationService consultationService;

    @Override
    protected IService<Consultation> service() {
        return consultationService;
    }

    @PutMapping("/{id}/reply")
    public Result<Consultation> reply(@PathVariable Long id, @RequestBody ReplyRequest request) {
        Consultation consultation = consultationService.getById(id);
        consultation.setReply(request.getReply());
        consultation.setStatus("已回复");
        EntityLifecycle.forUpdate(consultation);
        consultationService.updateById(consultation);
        return Result.success(consultationService.getById(id));
    }

    @PutMapping("/{id}/close")
    public Result<Consultation> close(@PathVariable Long id) {
        Consultation consultation = consultationService.getById(id);
        consultation.setStatus("已关闭");
        EntityLifecycle.forUpdate(consultation);
        consultationService.updateById(consultation);
        return Result.success(consultationService.getById(id));
    }

    @Data
    public static class ReplyRequest {
        private String reply;
    }
}
