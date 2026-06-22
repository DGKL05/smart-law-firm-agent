package com.example.dgkl.module.consultation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.module.common.BaseEntity;
import com.example.dgkl.module.common.UserOwned;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("consultation")
public class Consultation extends BaseEntity implements UserOwned {
    private Long userId;
    private Long lawyerId;
    private Long lawFirmId;
    private String title;
    private String question;
    private String reply;
    private String status;
}
