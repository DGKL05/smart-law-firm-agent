package com.example.dgkl.module.invoice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.module.invoice.entity.Invoice;
import com.example.dgkl.module.invoice.mapper.InvoiceMapper;
import com.example.dgkl.module.invoice.service.InvoiceService;
import org.springframework.stereotype.Service;

@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements InvoiceService {
}
