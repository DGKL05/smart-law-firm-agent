package com.example.dgkl.module.invoice.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.module.common.AbstractAdminCrudController;
import com.example.dgkl.module.invoice.entity.Invoice;
import com.example.dgkl.module.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/invoices")
@RequiredArgsConstructor
public class AdminInvoiceController extends AbstractAdminCrudController<Invoice> {
    private final InvoiceService invoiceService;

    @Override
    protected IService<Invoice> service() {
        return invoiceService;
    }
}
