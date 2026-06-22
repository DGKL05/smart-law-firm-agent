package com.example.dgkl.module.legalcase.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractUserCrudController;
import com.example.dgkl.module.legalcase.entity.LegalCase;
import com.example.dgkl.module.legalcase.service.LegalCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/cases")
@RequiredArgsConstructor
public class MyLegalCaseController extends AbstractUserCrudController<LegalCase> {
    private final LegalCaseService legalCaseService;

    @Override
    protected IService<LegalCase> service() {
        return legalCaseService;
    }
}
