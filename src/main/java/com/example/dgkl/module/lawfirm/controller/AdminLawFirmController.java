package com.example.dgkl.module.lawfirm.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/law-firms")
@RequiredArgsConstructor
public class AdminLawFirmController extends AbstractAdminCrudController<LawFirm> {
    private final LawFirmService lawFirmService;

    @Override
    protected IService<LawFirm> service() {
        return lawFirmService;
    }

    @Override
    protected List<String> keywordColumns() {
        return List.of("name", "province_name");
    }
}
