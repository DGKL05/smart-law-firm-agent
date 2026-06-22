package com.example.dgkl.module.lawyer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.mapper.LawyerMapper;
import com.example.dgkl.module.lawyer.service.LawyerService;
import org.springframework.stereotype.Service;

@Service
public class LawyerServiceImpl extends ServiceImpl<LawyerMapper, Lawyer> implements LawyerService {
}
