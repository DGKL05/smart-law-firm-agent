package com.example.dgkl.module.legalcase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.legalcase.entity.LegalCase;
import com.example.dgkl.module.legalcase.mapper.LegalCaseMapper;
import com.example.dgkl.module.legalcase.service.LegalCaseService;
import org.springframework.stereotype.Service;

@Service
public class LegalCaseServiceImpl extends ServiceImpl<LegalCaseMapper, LegalCase> implements LegalCaseService {
}
