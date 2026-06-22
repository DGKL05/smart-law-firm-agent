package com.example.dgkl.module.contract.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import com.example.dgkl.module.contract.entity.Contract;
import com.example.dgkl.module.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contracts")
@RequiredArgsConstructor
public class AdminContractController extends AbstractAdminCrudController<Contract> {
    private final ContractService contractService;

    @Override
    protected IService<Contract> service() {
        return contractService;
    }
}
