package com.example.dgkl.module.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.module.common.BaseEntity;
import com.example.dgkl.module.common.UserOwned;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity implements UserOwned {
    private Long userId;
    private String title;
    private String contractNo;
    private String contractType;
    private String partyA;
    private String partyB;
    private BigDecimal amount;
    private String status;
    private String fileUrl;
}
