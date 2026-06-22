package com.example.dgkl.module.invoice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.module.common.BaseEntity;
import com.example.dgkl.module.common.UserOwned;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice")
public class Invoice extends BaseEntity implements UserOwned {
    private Long userId;
    private String invoiceNo;
    private String title;
    private String taxNo;
    private BigDecimal amount;
    private String invoiceType;
    private String status;
    private String fileUrl;
}
