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
    private String caseNo;
    private String title;
    private String caseType;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public LocalDateTime getCaseTime() {
        return startTime;
    }
}
