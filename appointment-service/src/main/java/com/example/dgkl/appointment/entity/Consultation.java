package com.example.dgkl.appointment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("consultation")
public class Consultation extends BaseEntity {
    private Long userId;
    private Long lawFirmId;
    private Long lawyerId;
    private String title;
    private String question;
    private String reply;
    private String status;
}
