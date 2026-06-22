package com.example.dgkl.module.contract.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.contract.entity.Contract;
import com.example.dgkl.module.contract.mapper.ContractMapper;
import com.example.dgkl.module.contract.service.ContractService;
import org.springframework.stereotype.Service;

@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {
}
