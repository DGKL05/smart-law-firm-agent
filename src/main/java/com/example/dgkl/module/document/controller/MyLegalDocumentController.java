package com.example.dgkl.module.document.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractUserCrudController;
import com.example.dgkl.module.document.entity.LegalDocument;
import com.example.dgkl.module.document.service.LegalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/documents")
@RequiredArgsConstructor
public class MyLegalDocumentController extends AbstractUserCrudController<LegalDocument> {
    private final LegalDocumentService legalDocumentService;

    @Override
    protected IService<LegalDocument> service() {
        return legalDocumentService;
    }
}
