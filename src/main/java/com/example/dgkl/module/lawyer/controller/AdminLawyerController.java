package com.example.dgkl.module.lawyer.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.service.LawyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/lawyers")
@RequiredArgsConstructor
public class AdminLawyerController extends AbstractAdminCrudController<Lawyer> {
    private final LawyerService lawyerService;

    @Override
    protected IService<Lawyer> service() {
        return lawyerService;
    }

    @Override
    protected List<String> keywordColumns() {
        return List.of("name", "category");
    }
}
