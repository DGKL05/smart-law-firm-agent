package com.example.dgkl.appointment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.dgkl.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("legal_case")
public class LegalCase extends BaseEntity {
    private Long userId;
    private Long lawyerId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
