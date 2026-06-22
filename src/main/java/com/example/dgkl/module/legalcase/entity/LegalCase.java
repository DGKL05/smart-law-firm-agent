package com.example.dgkl.module.legalcase.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.module.common.BaseEntity;
import com.example.dgkl.module.common.UserOwned;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("legal_case")
public class LegalCase extends BaseEntity implements UserOwned {
    private Long userId;
    private Long lawyerId;
    private Long lawFirmId;
    private String caseNo;
    private String title;
    private String caseType;
    private String description;
    private LocalDateTime caseTime;
    private String status;
}
